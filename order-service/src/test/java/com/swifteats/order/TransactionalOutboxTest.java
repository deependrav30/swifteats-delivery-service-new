package com.swifteats.order;

import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.repo.OrderRepository;
import com.swifteats.order.repo.OutboxRepository;
import com.swifteats.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false"})
@Testcontainers
public class TransactionalOutboxTest extends TestContainersBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private OutboxRepository outboxRepository;

    @BeforeEach
    public void initDb() throws Exception {
        // ensure schema & tables exist before each test
        TestContainersBase.runSql("db/migration/V1__init.sql");
        // clear any pre-existing orders
        orderRepository.deleteAll();
    }

    @Test
    public void whenOutboxSaveFails_thenOrderNotPersisted() throws Exception {
        OrderEntity o = new OrderEntity();
        o.setClientOrderId(UUID.randomUUID().toString());
        o.setRestaurantId(1L);
        o.setTotalCents(1000L);

        // make outbox save throw to simulate failure during outbox persistence
        Mockito.doThrow(new RuntimeException("simulated outbox failure")).when(outboxRepository).save(Mockito.any());

        // createOrder should propagate the exception
        assertThrows(RuntimeException.class, () -> orderService.createOrder(o));

        // because the service method is transactional, the order insert must be rolled back
        assertThat(orderRepository.findByClientOrderId(o.getClientOrderId())).isEmpty();
    }

}
