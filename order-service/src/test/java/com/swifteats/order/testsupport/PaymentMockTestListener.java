package com.swifteats.order.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentMockTestListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentMockTestListener.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentMockTestListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "payments.incoming")
    public void onPaymentIncoming(String payload) {
        try {
            log.info("PaymentMockTestListener received payload: {}", payload);
            String orderId = UUID.randomUUID().toString();
            try {
                JsonNode n = objectMapper.readTree(payload);
                if (n.has("orderId")) {
                    orderId = n.get("orderId").asText();
                } else if (n.has("clientOrderId")) {
                    orderId = n.get("clientOrderId").asText();
                }
            } catch (Exception ignored) {
            }

            // build a simple result payload that contains the key "orderId" (test asserts this)
            String result = objectMapper.createObjectNode()
                    .put("orderId", orderId)
                    .put("status", "PAID")
                    .toString();

            // small artificial delay to mimic payment processing
            Thread.sleep(200);

            // publish to results queue (direct queue)
            rabbitTemplate.convertAndSend("", "payments.results", result);
            log.info("PaymentMockTestListener published result for orderId={}", orderId);
        } catch (Exception e) {
            // swallow errors in test component
            log.error("PaymentMockTestListener error", e);
        }
    }
}
