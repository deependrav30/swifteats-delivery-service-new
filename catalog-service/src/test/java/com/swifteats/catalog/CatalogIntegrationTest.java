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
public class CatalogIntegrationTest extends TestContainersBase {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Test
    public void testSeedDataPresent() throws Exception {
        // Use shared helper to run migration SQL against the Testcontainers Postgres
        TestContainersBase.runSql("db/migration/V1__init.sql");

        // Ensure repository can read seeded data
        assertThat(menuItemRepository.findAll()).isNotEmpty();
    }
}
