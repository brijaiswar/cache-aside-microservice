package com.example.cacheaside;

import com.example.cacheaside.dto.ProductRequest;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ProductNotFoundExceptionTest {

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
    @DisplayName("7.4a: getById throws ProductNotFoundException for non-existent ID")
    void getByIdThrowsWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(999L))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    @DisplayName("7.4b: update throws ProductNotFoundException for non-existent ID")
    void updateThrowsWhenNotFound() {
        when(productRepository.findById(998L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(998L,
            new ProductRequest("X", null, new BigDecimal("1.00"), 1, null)))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("998");
    }

    @Test
    @DisplayName("7.4c: delete throws ProductNotFoundException for non-existent ID")
    void deleteThrowsWhenNotFound() {
        when(productRepository.findById(997L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(997L))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("997");
    }
}