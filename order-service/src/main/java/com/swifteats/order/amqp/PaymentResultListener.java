package com.swifteats.order.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);
    private final ObjectMapper objectMapper;

    // test helper: store processed raw messages
    public static final ConcurrentLinkedQueue<String> processedMessages = new ConcurrentLinkedQueue<>();

    public PaymentResultListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payments.results")
    public void onPaymentResult(String body) {
        try {
            Map<String, Object> m = objectMapper.readValue(body, Map.class);
            log.info("Received payment result: {}", m);
            // store raw body for integration tests to assert consumption
            processedMessages.add(body);
            // In real flow, update order status, publish events, etc. For now just log.
        } catch (Exception e) {
            log.error("Failed to process payment result", e);
        }
    }
}
