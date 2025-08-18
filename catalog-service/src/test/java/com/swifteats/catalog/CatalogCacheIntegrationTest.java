package com.swifteats.catalog;

import com.swifteats.catalog.dto.MenuItemDto;
import com.swifteats.catalog.repo.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.flyway.enabled=false"})
@Testcontainers
public class CatalogCacheIntegrationTest extends TestContainersBase {

    @Container
    public static GenericContainer<?> redis = new GenericContainer("redis:7").withExposedPorts(6379).waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.redis.host", () -> redis.getHost());
        r.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MenuItemRepository repo;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long restaurantId;

    @BeforeEach
    public void initDb() throws Exception {
        TestContainersBase.runSql("db/migration/V1__init.sql");
        repo.deleteAll();

        // ensure a restaurant exists and capture its id
        Long id = null;
        try {
            id = jdbcTemplate.queryForObject("SELECT id FROM restaurants WHERE name = 'Demo Diner' LIMIT 1", Long.class);
        } catch (Exception ignored) {
        }
        if (id == null) {
            jdbcTemplate.update("INSERT INTO restaurants(name,address,is_open) VALUES ('Test R','addr',true)");
            id = jdbcTemplate.queryForObject("SELECT id FROM restaurants WHERE name = 'Test R' LIMIT 1", Long.class);
        }
        this.restaurantId = id;
    }

    @Test
    public void testCacheSetAndEvict() throws Exception {
        // insert item via HTTP POST to ensure eviction annotation is on controller
        MenuItemDto m = new MenuItemDto(null, "Burger", 599);
        ResponseEntity<MenuItemDto> postResp = restTemplate.postForEntity("/restaurants/{id}/menu", m, MenuItemDto.class, restaurantId);
        assertThat(postResp.getStatusCode().is2xxSuccessful()).isTrue();

        // first read via HTTP GET should populate cache (uses @Cacheable)
        ResponseEntity<MenuItemDto[]> getResp = restTemplate.getForEntity("/restaurants/{id}/menu", MenuItemDto[].class, restaurantId);
        MenuItemDto[] first = getResp.getBody();
        assertThat(first).isNotNull();
        assertThat(first.length).isGreaterThan(0);

        // delete DB rows for restaurant to ensure subsequent GET would return empty if cache is not used
        jdbcTemplate.update("DELETE FROM menu_items WHERE restaurant_id = ?", restaurantId);

        // second GET should still return cached result (cache hit) even though DB is empty
        ResponseEntity<MenuItemDto[]> cachedResp = restTemplate.getForEntity("/restaurants/{id}/menu", MenuItemDto[].class, restaurantId);
        MenuItemDto[] cached = cachedResp.getBody();
        assertThat(cached).isNotNull();
        assertThat(cached.length).isGreaterThan(0);

        // now add another item via HTTP POST which should evict cache and persist the new item
        MenuItemDto m2 = new MenuItemDto(null, "Fries", 199);
        ResponseEntity<MenuItemDto> postResp2 = restTemplate.postForEntity("/restaurants/{id}/menu", m2, MenuItemDto.class, restaurantId);
        assertThat(postResp2.getStatusCode().is2xxSuccessful()).isTrue();

        // After eviction, GET should reflect DB (only the newly added item)
        ResponseEntity<MenuItemDto[]> afterEvictResp = restTemplate.getForEntity("/restaurants/{id}/menu", MenuItemDto[].class, restaurantId);
        MenuItemDto[] after = afterEvictResp.getBody();
        assertThat(after).isNotNull();
        assertThat(after.length).isEqualTo(1);
        assertThat(after[0].getName()).isEqualTo("Fries");
    }
}
