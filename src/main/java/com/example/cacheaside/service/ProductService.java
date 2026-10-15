package com.example.cacheaside.service;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.entity.Product;
import com.example.cacheaside.exception.ProductNotFoundException;
import com.example.cacheaside.mapper.ProductMapper;
import com.example.cacheaside.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Cache-aside read: check Redis first; on miss, load from PostgreSQL and populate Redis.
     */
    @Cacheable(cacheNames = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductMapper.toResponse(product);
    }

    /**
     * List all products — always reads from DB, no individual entry caching.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Create product and populate Redis cache with the new entry.
     */
    @CachePut(cacheNames = "products", key = "#result.id()")
    public ProductResponse create(ProductRequest request) {
        Product product = ProductMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    /**
     * Update product and replace Redis cache entry with updated value.
     */
    @CachePut(cacheNames = "products", key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category());

        Product updated = productRepository.save(product);
        return ProductMapper.toResponse(updated);
    }

    /**
     * Delete product and evict from Redis cache.
     */
    @CacheEvict(cacheNames = "products", key = "#id")
    public void delete(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
    }
}
