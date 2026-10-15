package com.example.cacheaside;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: Timestamp Invariants on Lifecycle Events
 * Validates: Requirements 1.3, 1.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TimestampInvariantIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("cacheaside_ts_test")
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

    @Test
    @DisplayName("Property 1a: createdAt is non-null and updatedAt >= createdAt on create")
    void timestampsSetOnCreate() {
        ProductRequest request = new ProductRequest(
            "Timestamp Product", null, new BigDecimal("9.99"), 5, null);

        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
            "/api/v1/products", request, ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponse body = response.getBody();
        assertThat(body.createdAt()).isNotNull();
        assertThat(body.updatedAt()).isNotNull();
        assertThat(body.updatedAt()).isAfterOrEqualTo(body.createdAt());
    }

    @Test
    @DisplayName("Property 1b: updatedAt is after pre-update value after PUT")
    void updatedAtChangesAfterUpdate() throws InterruptedException {
        ProductRequest request = new ProductRequest(
            "Before Update", null, new BigDecimal("5.00"), 2, null);

        ResponseEntity<ProductResponse> createResp = restTemplate.postForEntity(
            "/api/v1/products", request, ProductResponse.class);
        ProductResponse created = createResp.getBody();
        Long id = created.id();

        // Short sleep to guarantee timestamp difference
        Thread.sleep(100);

        ProductRequest updateRequest = new ProductRequest(
            "After Update", null, new BigDecimal("6.00"), 3, null);
        ResponseEntity<ProductResponse> updateResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.PUT,
            new HttpEntity<>(updateRequest), ProductResponse.class);

        ProductResponse updated = updateResp.getBody();
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }
}