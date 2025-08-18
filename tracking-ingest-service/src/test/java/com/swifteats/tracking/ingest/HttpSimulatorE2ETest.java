package com.swifteats.tracking.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false", "spring.scheduling.enabled=false", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@Testcontainers
public class HttpSimulatorE2ETest extends TestContainersBase {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private AmqpTemplate amqpTemplate;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpSimulatorE2ETest() {
        // support java.time.Instant serialization
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @BeforeEach
    public void initDb() throws Exception {
        // ensure redis is reachable via TestContainersBase setup
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    public void simulatorHttpMode_writesToRedis_and_queryReads() throws Exception {
        String ingestUrl = "http://localhost:" + port + "/tracking/location";

        DriverLocationDto dto = new DriverLocationDto("driver-1", 18.52, 73.85, 3.5, Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = mapper.writeValueAsString(dto);

        // post to ingest
        rest.postForEntity(ingestUrl, new HttpEntity<>(json, headers), Void.class);

        // give tracking-ingest a short moment to write to Redis when it's implemented to do so
        TimeUnit.SECONDS.sleep(1);

        String key = "driver.location.driver-1";
        String val = redisTemplate.opsForValue().get(key);
        assertThat(val).isNotNull();
        assertThat(val).contains("driver-1");
    }
}
