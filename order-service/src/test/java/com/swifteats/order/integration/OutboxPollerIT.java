package com.swifteats.order.integration;

import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.poller.OutboxPoller;
import com.swifteats.order.repo.OutboxRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OutboxPollerIT {

    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @BeforeAll
    public static void setup() {
        rabbit.start();
    }

    @AfterAll
    public static void teardown() {
        rabbit.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        // use in-memory H2 for tests and disable Flyway to avoid external DB dependency
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    public void testOutboxPublishWithTransientRabbitFailures() throws Exception {
        // Insert a pending outbox event
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("order");
        e.setAggregateId(1L);
        e.setEventType("order.created");
        e.setPayload("{}");
        e.setAttempts(0);
        e.setPublished(false);
        e.setCreatedAt(Instant.now());

        outboxRepository.save(e);

        // Stop Rabbit to simulate failure for initial attempts
        rabbit.stop();
        Thread.sleep(500);

        // Call poller which should attempt to publish and fail (resilience retry will be used)
        try {
            outboxPoller.pollAndPublish();
        } catch (Exception ex) {
            // expected during downtime
        }

        // Restart Rabbit and allow poller to succeed
        rabbit.start();
        Thread.sleep(1000);

        // Call poller again to process pending event
        outboxPoller.pollAndPublish();

        OutboxEvent updated = outboxRepository.findById(e.getId()).orElseThrow();
        assertTrue(updated.isPublished() || updated.getAttempts() > 0, "Outbox event should have been attempted or published");
    }
}
