package com.swifteats.tracking.amqp;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConnectionFactoryConfig {

    @Bean
    public ConnectionFactory connectionFactory(
            @Value("${spring.rabbitmq.host:rabbitmq}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        String sanitizedHost = host == null ? "rabbitmq" : host.replaceAll("^-+", "");
        CachingConnectionFactory cf = new CachingConnectionFactory(sanitizedHost, port);
        cf.setUsername(username);
        cf.setPassword(password);
        return cf;
    }
}
