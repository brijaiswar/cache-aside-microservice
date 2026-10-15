package com.example.cacheaside;

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
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CacheEvictTest {

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
    @DisplayName("7.3: After delete, cache entry absent and subsequent getById throws exception")
    void deleteEvictsCacheEntry() {
        Product product = new Product();
        product.setId(300L);
        product.setName("To Delete");
        product.setPrice(new BigDecimal("1.00"));
        product.setStockQuantity(1);

        // First findById call during delete succeeds, second after eviction fails
        when(productRepository.findById(300L))
            .thenReturn(Optional.of(product))
            .thenReturn(Optional.empty());

        // Perform delete
        productService.delete(300L);

        // Cache entry should be gone
        assertThat(cacheManager.getCache("products").get(300L)).isNull();

        // Next getById must hit repository again and throw
        assertThatThrownBy(() -> productService.getById(300L))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("300");

        // Repository should have been invoked again (once for delete, once for getById)
        verify(productRepository, times(2)).findById(300L);
    }
}