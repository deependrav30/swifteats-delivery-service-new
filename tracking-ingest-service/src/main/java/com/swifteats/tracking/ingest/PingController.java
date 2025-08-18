package com.swifteats.tracking.ingest;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

// package-private helper to avoid creating a second Spring bean named 'pingController'
class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "pong", "service", "tracking-ingest");
    }
}
