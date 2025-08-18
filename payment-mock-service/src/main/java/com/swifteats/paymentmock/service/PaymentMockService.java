package com.swifteats.paymentmock.service;

import com.swifteats.paymentmock.dto.PaymentRequest;
import com.swifteats.paymentmock.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentMockService {

    private final double failureRate;
    private final long latencyMs;

    public PaymentMockService(@Value("${payment.failure-rate:0.0}") double failureRate,
                              @Value("${payment.latency-ms:100}") long latencyMs) {
        this.failureRate = failureRate;
        this.latencyMs = latencyMs;
    }

    public PaymentResponse process(PaymentRequest req) {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        PaymentResponse resp = new PaymentResponse();
        resp.setOrderId(req.getOrderId());
        resp.setTransactionId(UUID.randomUUID().toString());

        double r = Math.random();
        if (r < failureRate) {
            resp.setStatus("FAILED");
        } else {
            resp.setStatus("SUCCESS");
        }
        return resp;
    }
}
