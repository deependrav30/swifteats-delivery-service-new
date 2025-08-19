package com.swifteats.tracking.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.micrometer.core.annotation.Timed;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

// This class was previously annotated as a RestController, which caused a duplicate bean
// definition at runtime because another controller with the same simple name exists
// under the `controller` package. Remove the controller annotation so the implementation
// in `com.swifteats.tracking.query.controller.TrackingQuerySseController` is the active bean.

public class TrackingQuerySseController {

    private static final Logger log = LoggerFactory.getLogger(TrackingQuerySseController.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TrackingQuerySseController(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @GetMapping(path = "/drivers/{driverId}/location/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Timed("tracking.stream.latency")
    public SseEmitter streamDriverLocation(@PathVariable String driverId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());

        // Poll Redis for latest location every second and emit when changed
        new Thread(() -> {
            try {
                String last = null;
                while (true) {
                    String key = "driver.location." + driverId;
                    String payload = redis.opsForValue().get(key);
                    if (payload != null && !payload.equals(last)) {
                        emitter.send(payload);
                        last = payload;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.info("SSE stream closed for driver {}: {}", driverId, e.getMessage());
                emitter.complete();
            }
        }).start();

        return emitter;
    }
}
