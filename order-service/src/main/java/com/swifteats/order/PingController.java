package com.swifteats.order;

import java.util.Map;

/** Non-bean ping provider to avoid duplicate bean registration. */
public class PingController {

    public static Map<String, String> ping() {
        return Map.of("status", "pong", "service", "order");
    }
}
