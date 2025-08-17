package com.swifteats.order.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderEntity order) throws JsonProcessingException {
        OrderEntity saved = service.createOrder(order);
        return ResponseEntity.ok(saved);
    }
}
