package com.swifteats.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.simulator.dto.DriverLocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class SimulatorService {
    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);
    private final RestTemplate rest = new RestTemplate();
    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Future<?>> tasks = new ArrayList<>();

    public SimulatorService(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void start(SimConfig config) {
        stop();
        log.info("Starting simulator: drivers={}, ups={}, mode={}", config.getDrivers(), config.getUpdatesPerSecond(), config.getMode());
        Random rnd = new Random();

        for (int i = 0; i < config.getDrivers(); i++) {
            final String driverId = "driver-" + (i + 1);
            Future<?> f = executor.submit(() -> {
                try {
                    double lat = 18.5204 + rnd.nextDouble() * 0.01;
                    double lon = 73.8567 + rnd.nextDouble() * 0.01;

                    // If rabbit mode and custom host/port provided, create a local RabbitTemplate to avoid modifying global AmqpTemplate
                    RabbitTemplate localRabbit = null;
                    if ("rabbit".equalsIgnoreCase(config.getMode())) {
                        try {
                            CachingConnectionFactory ccf = new CachingConnectionFactory(config.getRabbitHost(), config.getRabbitPort());
                            localRabbit = new RabbitTemplate(ccf);
                        } catch (Exception ex) {
                            log.error("Failed to create local rabbit template", ex);
                        }
                    }

                    while (!Thread.currentThread().isInterrupted()) {
                        DriverLocationDto dto = new DriverLocationDto(driverId, lat, lon, 5.0 + rnd.nextDouble() * 10.0, Instant.now());
                        if ("rabbit".equalsIgnoreCase(config.getMode())) {
                            try {
                                if (localRabbit != null) {
                                    localRabbit.convertAndSend(config.getRabbitExchange(), "driver." + driverId, dto);
                                } else {
                                    amqpTemplate.convertAndSend(config.getRabbitExchange(), "driver." + driverId, dto);
                                }
                            } catch (Exception ex) {
                                log.error("Failed to publish to rabbit", ex);
                            }
                        } else {
                            try {
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                String json = mapper.writeValueAsString(dto);
                                rest.postForEntity(config.getIngestUrl(), new HttpEntity<>(json, headers), Void.class);
                            } catch (Exception ex) {
                                log.error("Failed to POST to ingest", ex);
                            }
                        }

                        // move small amount
                        lat += (rnd.nextDouble() - 0.5) * 0.0005;
                        lon += (rnd.nextDouble() - 0.5) * 0.0005;
                        try {
                            Thread.sleep(1000 / Math.max(1, config.getUpdatesPerSecond()));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (Throwable t) {
                    log.error("Simulator thread error", t);
                }
            });
            tasks.add(f);
        }
    }

    public void stop() {
        for (Future<?> f : tasks) {
            f.cancel(true);
        }
        tasks.clear();
    }
}
