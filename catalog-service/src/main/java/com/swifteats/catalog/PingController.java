package com.swifteats.catalog;

import java.util.Map;

/**
 * Utility Ping provider — not a Spring bean. The actual REST endpoint lives in
 * com.swifteats.catalog.controller.PingController to avoid duplicate bean names.
 */
public class PingController {

    public static Map<String, String> ping() {
        return Map.of("status", "pong", "service", "catalog");
    }
}
