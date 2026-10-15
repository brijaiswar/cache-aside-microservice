package com.example.cacheaside;

import com.example.cacheaside.dto.ProductRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CachePutTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("products").clear();
    }

    @Test
    @DisplayName("7.2a: After create, getById served from cache without DB call")
    void createPopulatesCache() {
        Product saved = new Product();
        saved.setId(200L);
        saved.setName("New Product");
        saved.setPrice(new BigDecimal("14.99"));
        saved.setStockQuantity(5);

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productService.create(new ProductRequest("New Product", null, new BigDecimal("14.99"), 5, null));

        // getById should be served from cache
        when(productRepository.findById(200L)).thenReturn(Optional.of(saved));
        ProductResponse response = productService.getById(200L);

        assertThat(response.id()).isEqualTo(200L);
        verify(productRepository, never()).findById(200L);
    }

    @Test
    @DisplayName("7.2b: After update, cache entry reflects updated fields")
    void updateRefreshesCache() {
        Product existing = new Product();
        existing.setId(201L);
        existing.setName("Old Name");
        existing.setPrice(new BigDecimal("10.00"));
        existing.setStockQuantity(3);

        Product updatedEntity = new Product();
        updatedEntity.setId(201L);
        updatedEntity.setName("Updated Name");
        updatedEntity.setPrice(new BigDecimal("20.00"));
        updatedEntity.setStockQuantity(7);

        when(productRepository.findById(201L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(updatedEntity);

        ProductResponse result = productService.update(201L,
            new ProductRequest("Updated Name", null, new BigDecimal("20.00"), 7, null));

        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(cacheManager.getCache("products").get(201L)).isNotNull();
    }
}