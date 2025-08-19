package com.swifteats.order.integration;

import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OutboxMetricsIT {

    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    public void prometheusEndpointExposesResilienceMetrics() throws Exception {
        // create a dummy outbox event so poller exists but we mainly assert metrics endpoint
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("order");
        e.setAggregateId(1L);
        e.setEventType("order.created");
        e.setPayload("{}");
        e.setAttempts(0);
        e.setPublished(false);
        e.setCreatedAt(Instant.now());
        outboxRepository.save(e);

        // hit /actuator/prometheus and assert resilience metrics present
        String body = restTemplate.getForObject("/actuator/prometheus", String.class);
        assertThat(body).isNotNull();
        // ensure at least one resilience metric is present
        assertThat(body != null && (body.contains("resilience_circuitbreaker_state") || body.contains("resilience_retry_count"))).isTrue();
    }
}
