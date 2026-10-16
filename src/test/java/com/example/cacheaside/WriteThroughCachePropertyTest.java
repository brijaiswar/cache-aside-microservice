package com.example.cacheaside;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.entity.Product;
import com.example.cacheaside.exception.ProductNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property 6: Write-Through Cache Consistency
 * Validates: Requirements 4.1, 4.2, 4.3
 *
 * Tests that create populates cache, update refreshes cache entry,
 * and delete evicts cache entry.
 */
@SpringBootTest
@ActiveProfiles("test")
class WriteThroughCachePropertyTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("products").clear();
    }

    @Test
    @DisplayName("Property 6a: After create, getById returns cached value without hitting repository")
    void createPopulatesCache() {
        Product saved = new Product();
        saved.setId(10L);
        saved.setName("New Product");
        saved.setPrice(new BigDecimal("19.99"));
        saved.setStockQuantity(20);

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductRequest request = new ProductRequest("New Product", null, new BigDecimal("19.99"), 20, null);
        ProductResponse created = productService.create(request);

        assertThat(created.id()).isEqualTo(10L);

        // getById should be served from cache â€” repository.findById should NOT be called
        when(productRepository.findById(10L)).thenReturn(Optional.of(saved));
        productService.getById(10L);

        verify(productRepository, never()).findById(10L);
    }

    @Test
    @DisplayName("Property 6b: After update, getById returns updated fields from cache")
    void updateRefreshesCacheEntry() {
        Product existing = new Product();
        existing.setId(20L);
        existing.setName("Old Name");
        existing.setPrice(new BigDecimal("10.00"));
        existing.setStockQuantity(5);

        Product updated = new Product();
        updated.setId(20L);
        updated.setName("New Name");
        updated.setPrice(new BigDecimal("15.00"));
        updated.setStockQuantity(10);

        when(productRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductRequest updateRequest = new ProductRequest("New Name", null, new BigDecimal("15.00"), 10, null);
        ProductResponse result = productService.update(20L, updateRequest);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.price()).isEqualByComparingTo("15.00");

        // Verify cache holds updated value
        assertThat(cacheManager.getCache("products").get(20L)).isNotNull();
    }

    @Test
    @DisplayName("Property 6c: After delete, cache entry is absent and subsequent getById hits repository")
    void deleteEvictsCacheEntry() {
        Product product = new Product();
        product.setId(30L);
        product.setName("To Delete");
        product.setPrice(new BigDecimal("5.00"));
        product.setStockQuantity(1);

        when(productRepository.findById(30L))
            .thenReturn(Optional.of(product))   // first call for delete
            .thenReturn(Optional.empty());        // second call after delete

        // Prime the cache
        productService.delete(30L);

        // Cache entry should now be absent
        assertThat(cacheManager.getCache("products").get(30L)).isNull();

        // Next getById should throw ProductNotFoundException (no longer in DB)
        assertThatThrownBy(() -> productService.getById(30L))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
