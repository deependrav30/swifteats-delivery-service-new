package com.swifteats.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class IntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        r.add("spring.datasource.username", () -> postgres.getUsername());
        r.add("spring.datasource.password", () -> postgres.getPassword());
        // Disable Flyway for tests to avoid unsupported DB detection in the current flyway-core
        r.add("spring.flyway.enabled", () -> "false");
    }

    @Test
    public void contextLoads() {
        // basic context load using Testcontainers Postgres
    }
}
