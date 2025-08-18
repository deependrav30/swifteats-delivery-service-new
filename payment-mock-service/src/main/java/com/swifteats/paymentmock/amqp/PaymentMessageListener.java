package com.swifteats.paymentmock.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.paymentmock.dto.PaymentRequest;
import com.swifteats.paymentmock.dto.PaymentResponse;
import com.swifteats.paymentmock.service.PaymentMockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentMessageListener.class);

    private final PaymentMockService paymentService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PaymentMessageListener(PaymentMockService paymentService, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payments.incoming")
    public void onMessage(String body) {
        try {
            PaymentRequest req = objectMapper.readValue(body, PaymentRequest.class);
            PaymentResponse resp = paymentService.process(req);
            String routingKey = resp.getStatus().equals("SUCCESS") ? "payment.success" : "payment.failed";
            String payload = objectMapper.writeValueAsString(resp);

            // publish to topic exchange for wider consumers
            rabbitTemplate.convertAndSend("swifteats.payment.events", routingKey, payload);
            // also send to payments.results queue for order-service to consume directly
            rabbitTemplate.convertAndSend("", "payments.results", payload);

            log.info("Processed payment for orderId={} status={}", resp.getOrderId(), resp.getStatus());
        } catch (Exception e) {
            log.error("Failed to process payment message", e);
        }
    }
}
