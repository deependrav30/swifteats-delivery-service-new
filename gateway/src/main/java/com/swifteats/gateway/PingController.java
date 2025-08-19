package com.swifteats.gateway;

import java.util.Map;

/**
 * Helper (non-bean) Ping provider. The real REST endpoint lives in
 * {@code com.swifteats.gateway.controller.PingController} so this class must not
 * be a Spring bean to avoid duplicate bean name 'pingController'.
 */
public final class PingController {

    private PingController() {}

    public static Map<String, String> pingMap() {
        return Map.of("status", "pong", "service", "gateway");
    }
}
