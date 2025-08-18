package com.swifteats.tracking.query.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class TrackingQuerySseController {

    private final StringRedisTemplate redisTemplate;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TrackingQuerySseController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * SSE endpoint that streams the latest driver location stored in Redis under key driver.location.{driverId}.
     * It polls Redis every second and pushes updates if changed. Connection timeout is 60s by default.
     */
    @GetMapping(path = "/drivers/{driverId}/location/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDriverLocation(@PathVariable("driverId") String driverId) {
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(60).toMillis());
        String key = "driver.location." + driverId;

        executor.execute(() -> {
            try {
                String last = null;
                while (true) {
                    String current = redisTemplate.opsForValue().get(key);
                    if (current != null && !current.equals(last)) {
                        emitter.send(current, MediaType.APPLICATION_JSON);
                        last = current;
                    }
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException ex) {
                // client disconnected or interrupted
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }
}
