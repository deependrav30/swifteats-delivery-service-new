package com.swifteats.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OrderRepository;
import com.swifteats.order.repo.OutboxRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Timed("order.create.duration")
    @Retry(name = "orderCreateRetry")
    @CircuitBreaker(name = "orderCreateCircuit")
    public OrderEntity createOrder(OrderEntity order) throws JsonProcessingException {
        // idempotency by clientOrderId
        orderRepository.findByClientOrderId(order.getClientOrderId()).ifPresent(existing -> {
            throw new IllegalStateException("order.already.exists");
        });

        OrderEntity saved = orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("order");
        event.setAggregateId(saved.getId());
        event.setEventType("order.created");
        try {
            event.setPayload(objectMapper.writeValueAsString(saved));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        event.setAttempts(0);
        event.setStatus("PENDING");
        outboxRepository.save(event);

        return saved;
    }
}
