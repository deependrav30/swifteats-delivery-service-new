package com.swifteats.order;

import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import com.swifteats.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false"})
@Testcontainers
public class OutboxIntegrationTest extends TestContainersBase {

    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.11-management");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // postgres properties come from TestContainersBase
        r.add("spring.rabbitmq.host", () -> rabbit.getHost());
        r.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort());
        r.add("spring.rabbitmq.username", () -> rabbit.getAdminUsername());
        r.add("spring.rabbitmq.password", () -> rabbit.getAdminPassword());
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    public void initDb() throws Exception {
        // ensure schema & tables exist before each test
        TestContainersBase.runSql("db/migration/V1__init.sql");
    }

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

        OutboxEvent e = outboxRepository.findAll().get(0);
        assertThat(e.getStatus()).isEqualTo("PENDING");
        assertThat(e.isPublished()).isFalse();
    }

}
