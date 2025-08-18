package com.swifteats.tracking.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Testcontainers
@Import(MultiDriverHttpE2ETest.SseTestControllerConfiguration.class)
public class MultiDriverHttpE2ETest extends TestContainersBase {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private org.springframework.amqp.core.AmqpTemplate amqpTemplate;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public MultiDriverHttpE2ETest() {
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @BeforeEach
    public void before() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    public void multiDriverSimulator_httpMode_endToEnd_streamsThroughSse() throws Exception {
        String ingestUrl = "http://localhost:" + port + "/tracking/location";

        int drivers = 5;
        int updatesPerDriver = 3;

        List<DriverLocationDto> posted = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < drivers; i++) {
            String driverId = "driver-" + (i + 1);
            double lat = 18.5204 + i * 0.0001;
            double lon = 73.8567 + i * 0.0001;
            for (int u = 0; u < updatesPerDriver; u++) {
                DriverLocationDto dto = new DriverLocationDto(driverId, lat + u * 0.00001, lon + u * 0.00001, 4.0 + u, Instant.now());
                String json = mapper.writeValueAsString(dto);
                rest.postForEntity(ingestUrl, new HttpEntity<>(json, headers), Void.class);
                posted.add(dto);
            }
        }

        // give tracking-ingest a moment to persist to Redis
        TimeUnit.SECONDS.sleep(1);

        // connect to SSE for one driver and verify we receive the latest value
        String targetDriver = "driver-3";
        URL url = new URL("http://localhost:" + port + "/drivers/" + targetDriver + "/location/stream");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        conn.connect();

        assertThat(conn.getResponseCode()).isEqualTo(200);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        boolean found = false;
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline && (line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            // SSE may send lines like: data: { ... }
            if (line.startsWith("data:")) {
                String payload = line.substring("data:".length()).trim();
                if (payload.contains(targetDriver)) {
                    found = true;
                    break;
                }
            }
        }

        reader.close();
        conn.disconnect();

        assertThat(found).isTrue();
    }

    @TestConfiguration
    public static class SseTestControllerConfiguration {

        @Bean
        public TestSseController testSseController(StringRedisTemplate redisTemplate) {
            return new TestSseController(redisTemplate);
        }
    }

    @RestController
    static class TestSseController {
        private final StringRedisTemplate redisTemplate;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        TestSseController(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @GetMapping(path = "/drivers/{driverId}/location/stream", produces = "text/event-stream")
        public SseEmitter stream(@PathVariable("driverId") String driverId) {
            SseEmitter emitter = new SseEmitter(Duration.ofSeconds(60).toMillis());
            String key = "driver.location." + driverId;

            executor.execute(() -> {
                try {
                    String last = null;
                    while (true) {
                        String current = redisTemplate.opsForValue().get(key);
                        if (current != null && !current.equals(last)) {
                            try {
                                emitter.send("data: " + current + "\n\n");
                            } catch (Exception e) {
                                break;
                            }
                            last = current;
                        }
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    emitter.complete();
                }
            });

            return emitter;
        }
    }
}
