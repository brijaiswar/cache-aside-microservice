package com.example.cacheaside.service;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.entity.Product;
import com.example.cacheaside.exception.ProductNotFoundException;
import com.example.cacheaside.mapper.ProductMapper;
import com.example.cacheaside.repository.ProductRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer implementing cache-aside pattern for Product operations.
 * All cache operations are logged and timed for monitoring.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Cache-aside read: check Redis first; on miss, load from PostgreSQL and populate Redis.
     * Metrics: Tracks cache hit/miss ratio and query duration.
     */
    @Cacheable(cacheNames = "products", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    @Timed(value = "product.service.getById", description = "Time taken to get product by ID")
    public ProductResponse getById(Long id) {
        log.debug("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product not found with id: {}", id);
                return new ProductNotFoundException(id);
            });
        log.debug("Product found: {}", product);
        return ProductMapper.toResponse(product);
    }

    /**
     * List all products â€” always reads from DB, no individual entry caching.
     * Consider adding pagination for large datasets.
     */
    @Transactional(readOnly = true)
    @Timed(value = "product.service.getAll", description = "Time taken to get all products")
    public List<ProductResponse> getAll() {
        log.debug("Fetching all products");
        List<Product> products = productRepository.findAll();
        log.debug("Found {} products", products.size());
        return products.stream()
            .map(ProductMapper::toResponse)
            .toList();
    }

    /**
     * Create product and populate Redis cache with the new entry.
     */
    @CachePut(cacheNames = "products", key = "#result.id()", unless = "#result == null")
    @Timed(value = "product.service.create", description = "Time taken to create product")
    public ProductResponse create(ProductRequest request) {
        log.info("Creating new product: {}", request.name());
        Product product = ProductMapper.toEntity(request);
        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return ProductMapper.toResponse(saved);
    }

    /**
     * Update product and replace Redis cache entry with updated value.
     * Uses optimistic locking via @Version for concurrent update safety.
     */
    @CachePut(cacheNames = "products", key = "#id", unless = "#result == null")
    @Timed(value = "product.service.update", description = "Time taken to update product")
    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Cannot update - Product not found with id: {}", id);
                return new ProductNotFoundException(id);
            });

        updateProductFields(product, request);
        
        Product updated = productRepository.save(product);
        log.info("Product updated successfully: {}", id);
        return ProductMapper.toResponse(updated);
    }

    /**
     * Delete product and evict from Redis cache.
     */
    @CacheEvict(cacheNames = "products", key = "#id")
    @Timed(value = "product.service.delete", description = "Time taken to delete product")
    public void delete(Long id) {
        log.info("Deleting product with id: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Cannot delete - Product not found with id: {}", id);
                return new ProductNotFoundException(id);
            });
        productRepository.delete(product);
        log.info("Product deleted successfully: {}", id);
    }

    /**
     * Evict all cache entries. Useful for admin operations.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    @Timed(value = "product.service.evictAllCache", description = "Time taken to evict all cache")
    public void evictAllCache() {
        log.info("Evicting all product cache entries");
    }

    /**
     * Helper method to update product fields from request.
     */
    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category());
    }
}