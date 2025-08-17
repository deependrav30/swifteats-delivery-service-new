package com.swifteats.catalog;

import com.swifteats.catalog.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheConfigTest {

    @Test
    public void testRedisConnectionFactory() {
        CacheConfig cfg = new CacheConfig();
        RedisConnectionFactory f = cfg.redisConnectionFactory();
        assertThat(f).isInstanceOf(LettuceConnectionFactory.class);
    }
}
