package com.swifteats.order;

import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import com.swifteats.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false"})
@Testcontainers
public class SuccessfulOutboxTest extends TestContainersBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    public void initDb() throws Exception {
        // ensure schema & tables exist before each test
        TestContainersBase.runSql("db/migration/V1__init.sql");
        // clear any pre-existing outbox rows
        outboxRepository.deleteAll();
    }

    @Test
    public void whenCreateOrder_thenOutboxEventPersisted() throws Exception {
        OrderEntity o = new OrderEntity();
        o.setClientOrderId(UUID.randomUUID().toString());
        o.setRestaurantId(1L);
        o.setTotalCents(1500L);

        // should not throw
        orderService.createOrder(o);

        // outbox row should be present and pending
        assertThat(outboxRepository.findAll()).isNotEmpty();
        OutboxEvent e = outboxRepository.findAll().get(0);
        assertThat(e.getStatus()).isEqualTo("PENDING");
        assertThat(e.isPublished()).isFalse();
        assertThat(e.getAggregateType()).isEqualTo("order");
    }
}
