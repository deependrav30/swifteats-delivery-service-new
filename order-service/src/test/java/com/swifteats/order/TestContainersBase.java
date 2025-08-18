package com.swifteats.order;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;

@Testcontainers
public abstract class TestContainersBase {

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
        // Disable Flyway auto-config for tests (SQL seeding is used)
        r.add("spring.flyway.enabled", () -> "false");
        // Disable scheduled tasks during context startup to avoid pollers hitting DB before migrations are applied
        r.add("spring.scheduling.enabled", () -> "false");
    }

    public static void runSql(String classpathResource) throws Exception {
        String jdbc = postgres.getJdbcUrl();
        String cleanedJdbc = jdbc.contains("?") ? jdbc.substring(0, jdbc.indexOf('?')) : jdbc;
        try (Connection conn = DriverManager.getConnection(cleanedJdbc, postgres.getUsername(), postgres.getPassword())) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(classpathResource));
        }
    }
}
