package com.swifteats.tracking.ingest.controller;

import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);
    private final AmqpTemplate amqpTemplate;

    public TrackingController(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @PostMapping("/location")
    public ResponseEntity<Void> pushLocation(@RequestBody DriverLocationDto dto) {
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(Instant.now());
        }
        // publish to driver.location.v1 exchange with routing key driver.<driverId>
        String routingKey = "driver." + dto.getDriverId();
        try {
            amqpTemplate.convertAndSend("driver.location.v1", routingKey, dto);
            log.info("Published driver location for {} to exchange driver.location.v1", dto.getDriverId());
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            log.error("Failed to publish driver location", ex);
            return ResponseEntity.status(502).build();
        }
    }
}
