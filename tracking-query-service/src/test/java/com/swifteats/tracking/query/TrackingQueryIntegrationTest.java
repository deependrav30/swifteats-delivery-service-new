package com.swifteats.tracking.query;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TrackingQueryIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void beforeAll() {
        redis.start();
        System.setProperty("spring.redis.host", redis.getHost());
        System.setProperty("spring.redis.port", String.valueOf(redis.getMappedPort(6379)));
    }

    @AfterAll
    static void afterAll() {
        redis.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void pingAvailable() {
        ResponseEntity<String> resp = restTemplate.getForEntity("http://localhost:" + port + "/ping", String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void sseStreamReceivesValueFromRedis() throws Exception {
        String key = "driver.location.driver-42";
        String value = "{\"lat\":12.34,\"lon\":56.78}";
        redisTemplate.opsForValue().set(key, value);

        ResponseEntity<String> resp = restTemplate.getForEntity("http://localhost:" + port + "/drivers/driver-42/location/stream", String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).contains("12.34");
    }
}
