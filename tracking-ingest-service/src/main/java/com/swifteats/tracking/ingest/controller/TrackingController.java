package com.swifteats.tracking.ingest.controller;

import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TrackingController(AmqpTemplate amqpTemplate, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.amqpTemplate = amqpTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/location")
    public ResponseEntity<Void> pushLocation(@RequestBody DriverLocationDto dto) {
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(Instant.now());
        }

        String key = "driver.location." + dto.getDriverId();
        try {
            // persist latest location to Redis so tracking-query can read it even if Rabbit is unavailable
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json);
        } catch (Exception ex) {
            log.error("Failed to write driver location to Redis", ex);
            // continue to attempt Rabbit publish
        }

        // publish to driver.location.v1 exchange with routing key driver.<driverId>
        String routingKey = "driver." + dto.getDriverId();
        try {
            amqpTemplate.convertAndSend("driver.location.v1", routingKey, dto);
            log.info("Published driver location for {} to exchange driver.location.v1", dto.getDriverId());
        } catch (Exception ex) {
            // do not fail the HTTP request if Rabbit is down; Redis write is primary for query path
            log.warn("Failed to publish driver location to RabbitMQ, continuing — data written to Redis", ex);
        }

        return ResponseEntity.accepted().build();
    }
}
