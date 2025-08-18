package com.swifteats.tracking.ingest;

import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

// Exclude Rabbit auto-configuration so the test context doesn't require a live RabbitTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
public class TrackingIngestIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AmqpTemplate amqpTemplate;

    @Test
    public void postLocationPublishesToExchange() throws Exception {
        DriverLocationDto dto = new DriverLocationDto("driver-1", 18.5204, 73.8567, 10.0, Instant.now());
        restTemplate.postForEntity("http://localhost:" + port + "/tracking/location", dto, Void.class);

        // verify that the controller attempted to publish to the expected exchange
        verify(amqpTemplate, timeout(1000)).convertAndSend(eq("driver.location.v1"), any(String.class), any(Object.class));
    }
}
