package com.swifteats.order;

import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.repo.OutboxRepository;
import com.swifteats.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OutboxIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    public static GenericContainer<?> rabbit = new GenericContainer<>("rabbitmq:3.13-management")
            .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        r.add("spring.datasource.username", () -> postgres.getUsername());
        r.add("spring.datasource.password", () -> postgres.getPassword());
        r.add("spring.rabbitmq.host", () -> rabbit.getHost());
        r.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    public void testOutboxCreatedOnOrder() throws Exception {
        OrderEntity o = new OrderEntity();
        o.setClientOrderId(UUID.randomUUID().toString());
        o.setRestaurantId(1L);
        o.setTotalCents(1000L);

        // use service to ensure transactional outbox write
        orderService.createOrder(o);

        // outbox row should be present
        assertThat(outboxRepository.findAll()).isNotEmpty();
    }

}
