package com.example.cacheaside;

import com.example.cacheaside.dto.ErrorResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("cacheaside_test")
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

    private ProductRequest sampleRequest() {
        return new ProductRequest(
            "Integration Product",
            "A product for integration testing",
            new BigDecimal("49.99"),
            25,
            "Testing"
        );
    }

    @Test
    @DisplayName("8.1a: Full CRUD round-trip — POST, GET, PUT, DELETE")
    void fullCrudRoundTrip() {
        // CREATE
        ResponseEntity<ProductResponse> createResp = restTemplate.postForEntity(
            "/api/v1/products", sampleRequest(), ProductResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResp.getBody().id();
        assertThat(id).isNotNull();

        // GET by ID
        ResponseEntity<ProductResponse> getResp = restTemplate.getForEntity(
            "/api/v1/products/" + id, ProductResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().name()).isEqualTo("Integration Product");

        // UPDATE
        ProductRequest updateReq = new ProductRequest(
            "Updated Product", "Updated description", new BigDecimal("59.99"), 30, "Electronics");
        ResponseEntity<ProductResponse> updateResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.PUT,
            new HttpEntity<>(updateReq), ProductResponse.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResp.getBody().name()).isEqualTo("Updated Product");

        // DELETE
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
            "/api/v1/products/" + id, HttpMethod.DELETE,
            null, Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify 404 after delete
        ResponseEntity<ErrorResponse> notFound = restTemplate.getForEntity(
            "/api/v1/products/" + id, ErrorResponse.class);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("8.1b: 404 ErrorResponse structure for unknown ID")
    void notFoundReturnsStructuredError() {
        ResponseEntity<ErrorResponse> resp = restTemplate.getForEntity(
            "/api/v1/products/99999", ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isNotBlank();
    }

    @Test
    @DisplayName("8.1c: List completeness — GET /api/v1/products returns all inserted products")
    void listCompletenessAfterInserts() {
        int initialCount = restTemplate.getForObject("/api/v1/products", List.class).size();

        int N = 3;
        for (int i = 0; i < N; i++) {
            restTemplate.postForEntity("/api/v1/products",
                new ProductRequest("Product " + i, null, new BigDecimal("1.00"), 1, null),
                ProductResponse.class);
        }

        List<?> all = restTemplate.getForObject("/api/v1/products", List.class);
        assertThat(all.size()).isEqualTo(initialCount + N);
    }
}