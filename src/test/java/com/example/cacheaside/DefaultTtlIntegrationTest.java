package com.example.cacheaside;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 7: Default TTL Applied When Configuration Is Absent
 * Validates: Requirement 5.2
 *
 * Verifies that after a cache entry is created, its Redis TTL is <= 10 minutes and > 0.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DefaultTtlIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("cacheaside_ttl_test")
        .withUsername("cacheaside")
        .withPassword("cacheaside");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Property 7: Freshly cached product entry has TTL <= 10 minutes and > 0")
    void freshCacheEntryHasTtlWithinBounds() {
        // Create a product (triggers @CachePut)
        ProductRequest request = new ProductRequest(
            "TTL Test Product", null, new BigDecimal("12.99"), 8, "TTL");

        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
            "/api/v1/products", request, ProductResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = response.getBody().id();

        // Trigger a GET to ensure the entry is definitely in the cache
        restTemplate.getForEntity("/api/v1/products/" + id, ProductResponse.class);

        // Find the Redis key for this entry
        Set<String> keys = stringRedisTemplate.keys("products*");
        assertThat(keys).isNotEmpty();

        // Check TTL of the first matching key
        String cacheKey = keys.stream()
            .filter(k -> k.contains(String.valueOf(id)))
            .findFirst()
            .orElse(keys.iterator().next());

        Long ttlSeconds = stringRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0);
        assertThat(ttlSeconds).isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());
    }
}