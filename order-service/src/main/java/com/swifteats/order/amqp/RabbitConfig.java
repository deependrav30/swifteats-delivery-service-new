package com.swifteats.order.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue paymentsResultsQueue() {
        return new Queue("payments.results", true);
    }

    @Bean
    public Queue paymentsIncomingQueue() {
        return new Queue("payments.incoming", true);
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange("swifteats.payment.events");
    }

    // Declare the order events exchange used by the OutboxPoller
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange("swifteats.order.events");
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        // avoid failing startup if broker isn't fully ready during tests; retries will redeclare when connection recovers
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }
}
