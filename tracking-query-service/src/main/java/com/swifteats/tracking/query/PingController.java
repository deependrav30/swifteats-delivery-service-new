package com.swifteats.tracking.query;

/**
 * Non-bean helper kept to avoid breaking existing references. This file is intentionally
 * NOT annotated with @RestController so Spring won't register a second bean named
 * 'pingController' (the real controller lives in com.swifteats.tracking.query.controller).
 */
public final class PingController {
    private PingController() {}

    public static java.util.Map<String, String> info() {
        return java.util.Map.of("service", "tracking-query", "status", "ok");
    }
}
