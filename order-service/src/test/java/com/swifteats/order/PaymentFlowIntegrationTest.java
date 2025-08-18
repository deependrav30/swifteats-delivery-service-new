package com.swifteats.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.order.amqp.PaymentResultListener;
import com.swifteats.order.model.OrderEntity;
import com.swifteats.order.poller.OutboxPoller;
import com.swifteats.order.repo.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false"})
@Testcontainers
@ComponentScan(basePackages = {"com.swifteats.order.testsupport", "com.swifteats.order"})
public class PaymentFlowIntegrationTest extends TestContainersBase {

    @Container
    // ensure we use RabbitMQContainer with the updated image
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.11-management");

    @DynamicPropertySource
    static void props2(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.host", () -> rabbit.getHost());
        r.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort());
        r.add("spring.rabbitmq.username", () -> rabbit.getAdminUsername());
        r.add("spring.rabbitmq.password", () -> rabbit.getAdminPassword());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxPoller outboxPoller;

    @BeforeEach
    public void init() throws Exception {
        // seed schema
        TestContainersBase.runSql("db/migration/V1__init.sql");
        outboxRepository.deleteAll();
        PaymentResultListener.processedMessages.clear();
    }

    @Test
    public void testOrderCreateTriggersPaymentAndOrderConsumesResult() throws Exception {
        OrderEntity o = new OrderEntity();
        o.setClientOrderId(UUID.randomUUID().toString());
        o.setRestaurantId(1L);
        o.setTotalCents(1500L);

        ResponseEntity<OrderEntity> resp = restTemplate.postForEntity("/orders", o, OrderEntity.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // explicitly trigger the outbox poller since scheduling is disabled in tests
        outboxPoller.pollAndPublish();

        // wait up to 10 seconds for the async flow to complete
        long start = System.nanoTime();
        long timeoutNs = Duration.ofSeconds(10).toNanos();
        while (PaymentResultListener.processedMessages.isEmpty() && (System.nanoTime() - start) < timeoutNs) {
            Thread.sleep(200);
        }

        // assert that order-service received a payment result message
        assertThat(PaymentResultListener.processedMessages).isNotEmpty();
        String msg = PaymentResultListener.processedMessages.peek();
        assertThat(msg).isNotNull();
        // basic sanity check: message contains orderId
        assertThat(msg).contains("orderId");
    }
}
