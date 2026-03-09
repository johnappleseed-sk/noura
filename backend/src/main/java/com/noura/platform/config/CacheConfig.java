package com.noura.platform.config;

import org.springframework.cache.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    /**
     * Executes cache manager.
     *
     * @param redisConnectionFactory The redis connection factory value.
     * @return The result of cache manager.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(15));

        Map<String, RedisCacheConfiguration> perCacheTtl = Map.of(
                "products", defaults.entryTtl(Duration.ofMinutes(10)),
                "stores", defaults.entryTtl(Duration.ofMinutes(5)),
                "recommendations", defaults.entryTtl(Duration.ofMinutes(3))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCacheTtl)
                .build();
    }
}
