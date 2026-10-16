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

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceCacheTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product buildProduct(Long id, String name) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal("9.99"));
        p.setStockQuantity(10);
        return p;
    }

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("products").clear();
    }

    @Test
    @DisplayName("7.1a: Cache hit â€” second getById call does not invoke repository")
    void cacheHitDoesNotHitRepository() {
        Product p = buildProduct(100L, "Cache Hit Product");
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));

        productService.getById(100L); // miss -> loads from DB
        productService.getById(100L); // hit  -> served from cache

        verify(productRepository, times(1)).findById(100L);
    }

    @Test
    @DisplayName("7.1b: Cache miss â€” repository called, result stored, second call skips DB")
    void cacheMissLoadsAndPopulatesCache() {
        Product p = buildProduct(101L, "Cache Miss Product");
        when(productRepository.findById(101L)).thenReturn(Optional.of(p));

        ProductResponse first = productService.getById(101L);
        assertThat(first.id()).isEqualTo(101L);
        verify(productRepository, times(1)).findById(101L);

        ProductResponse second = productService.getById(101L);
        assertThat(second.id()).isEqualTo(101L);
        // Still only one DB call
        verify(productRepository, times(1)).findById(101L);
    }
}