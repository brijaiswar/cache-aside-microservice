package com.example.cacheaside;

import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.entity.Product;
import com.example.cacheaside.repository.ProductRepository;
import com.example.cacheaside.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property 5: Cache-Aside Read â€” Miss Populates Cache
 * Validates: Requirement 3.2
 *
 * Tests that on a cache miss, repository is called exactly once, result is cached,
 * and subsequent calls within TTL are served from cache.
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheMissPropertyTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("products").clear();

        sampleProduct = new Product();
        sampleProduct.setId(2L);
        sampleProduct.setName("Miss Test Product");
        sampleProduct.setDescription("Tests cache miss");
        sampleProduct.setPrice(new BigDecimal("49.99"));
        sampleProduct.setStockQuantity(50);
        sampleProduct.setCategory("Books");
    }

    @Test
    @DisplayName("Property 5: Cache miss loads from DB, caches result, second call skips DB")
    void cacheMissPopulatesCache() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(sampleProduct));

        // First call â€” cache miss, loads from repository
        ProductResponse first = productService.getById(2L);
        assertThat(first.id()).isEqualTo(2L);

        // Verify repository was called once
        verify(productRepository, times(1)).findById(2L);

        // Second call â€” should now be served from cache
        ProductResponse second = productService.getById(2L);
        assertThat(second.id()).isEqualTo(2L);

        // Repository still called only once (second call used cache)
        verify(productRepository, times(1)).findById(2L);

        // Verify cache actually contains the entry
        assertThat(cacheManager.getCache("products").get(2L)).isNotNull();
    }
}
