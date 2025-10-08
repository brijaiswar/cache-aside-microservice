package com.wisepay.cache.microservices.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // 1. Configure default caching behavior for Redis
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Set default TTL for all cache entries to 10 minutes
                .entryTtl(Duration.ofMinutes(10))

                // Disable caching of null values to prevent caching 'not found' results
                .disableCachingNullValues()

                // Configure Key serialization (Keys are simple Strings)
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))

                // Configure Value serialization (Values are Java objects, converted to JSON)
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
