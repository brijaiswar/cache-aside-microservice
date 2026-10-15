package com.example.cacheaside;

import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.entity.Product;
import com.example.cacheaside.exception.ProductNotFoundException;
import com.example.cacheaside.mapper.ProductMapper;
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
import static org.mockito.Mockito.*;

/**
 * Property 4: Cache-Aside Read — Hit Avoids Database
 * Validates: Requirement 3.1
 *
 * Tests that once a product is cached, subsequent reads do not invoke the repository.
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheHitPropertyTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheManager.getCache("products").clear();

        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Test Product");
        sampleProduct.setDescription("A test product");
        sampleProduct.setPrice(new BigDecimal("29.99"));
        sampleProduct.setStockQuantity(100);
        sampleProduct.setCategory("Electronics");
    }

    @Test
    @DisplayName("Property 4: Second getById call hits cache and does not invoke repository")
    void cacheHitAvoidsDatabase() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        // First call — should hit the repository (cache miss)
        ProductResponse first = productService.getById(1L);
        assertThat(first.id()).isEqualTo(1L);

        // Second call — should hit cache, NOT the repository
        ProductResponse second = productService.getById(1L);
        assertThat(second.id()).isEqualTo(1L);

        // Repository should have been called exactly once despite two service calls
        verify(productRepository, times(1)).findById(1L);
    }
}
