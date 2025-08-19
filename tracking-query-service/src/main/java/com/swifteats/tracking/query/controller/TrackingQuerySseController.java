package com.swifteats.tracking.query.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class TrackingQuerySseController {

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TrackingQuerySseController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * SSE endpoint that streams the latest driver location stored in Redis under key driver.location.{driverId}.
     * It polls Redis every second and pushes updates if changed. Connection timeout is 60s by default.
     */
    @GetMapping(path = "/drivers/{driverId}/location/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDriverLocation(@PathVariable("driverId") String driverId) {
        SseEmitter emitter = new SseEmitter(0L);

        Runnable task = () -> {
            try {
                String key = "driver.location." + driverId;
                String payload = redisTemplate.opsForValue().get(key);
                if (payload != null) {
                    emitter.send(SseEmitter.event().data(payload));
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);

        emitter.onCompletion(() -> scheduler.shutdown());
        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }
}
