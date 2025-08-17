package com.swifteats.catalog;

import com.swifteats.catalog.repo.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CatalogIntegrationTest {

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
        // Disable Boot's Flyway autoconfig for tests
        r.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Test
    public void testSeedDataPresent() throws Exception {
        // Execute the migration SQL directly against the Testcontainers Postgres so the DB is seeded for the test
        String jdbc = postgres.getJdbcUrl();
        String cleanedJdbc = jdbc.contains("?") ? jdbc.substring(0, jdbc.indexOf('?')) : jdbc;

        try (Connection conn = DriverManager.getConnection(cleanedJdbc, postgres.getUsername(), postgres.getPassword())) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V1__init.sql"));
        }

        // Ensure repository can read seeded data
        assertThat(menuItemRepository.findAll()).isNotEmpty();
    }
}
