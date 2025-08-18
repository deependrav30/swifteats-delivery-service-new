package com.swifteats.common;

import java.util.Map;

/**
 * Utility Ping provider — not a Spring bean. Individual modules provide their
 * own REST endpoint in com.swifteats.<module>.controller.PingController.
 */
public class PingController {

    public static Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
