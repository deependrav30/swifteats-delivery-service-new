package com.swifteats.paymentmock.controller;

import com.swifteats.paymentmock.dto.PaymentRequest;
import com.swifteats.paymentmock.dto.PaymentResponse;
import com.swifteats.paymentmock.service.PaymentMockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentMockService service;

    public PaymentController(PaymentMockService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> charge(@RequestBody PaymentRequest req) {
        PaymentResponse resp = service.process(req);
        if ("SUCCESS".equals(resp.getStatus())) {
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.status(502).body(resp);
        }
    }
}
