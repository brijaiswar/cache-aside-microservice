package com.example.cacheaside.controller;

import com.example.cacheaside.dto.ProductRequest;
import com.example.cacheaside.dto.ProductResponse;
import com.example.cacheaside.service.ProductService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Product CRUD operations with cache-aside pattern.
 * All endpoints are monitored via Micrometer metrics.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management APIs with Redis caching")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new product", description = "Creates a new product and caches it in Redis")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Timed(value = "product.controller.create", description = "Time to handle create request")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody @Parameter(description = "Product details") ProductRequest request) {
        log.info("REST request to create Product: {}", request.name());
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID", description = "Fetches product from cache if available, otherwise from database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Timed(value = "product.controller.getById", description = "Time to handle get by ID request")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable @Parameter(description = "Product ID") Long id) {
        log.debug("REST request to get Product: {}", id);
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Returns a list of all products")
    @ApiResponse(responseCode = "200", description = "List of all products")
    @Timed(value = "product.controller.getAll", description = "Time to handle get all request")
    public ResponseEntity<List<ProductResponse>> getAll() {
        log.debug("REST request to get all Products");
        List<ProductResponse> responses = productService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product", description = "Updates product and refreshes cache")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Timed(value = "product.controller.update", description = "Time to handle update request")
    public ResponseEntity<ProductResponse> update(
            @PathVariable @Parameter(description = "Product ID") Long id,
            @Valid @RequestBody @Parameter(description = "Updated product details") ProductRequest request) {
        log.info("REST request to update Product: {}", id);
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a product", description = "Deletes product and evicts from cache")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Timed(value = "product.controller.delete", description = "Time to handle delete request")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "Product ID") Long id) {
        log.info("REST request to delete Product: {}", id);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Evict all cache", description = "Admin endpoint to clear all product cache")
    @ApiResponse(responseCode = "204", description = "Cache evicted successfully")
    public ResponseEntity<Void> evictAllCache() {
        log.warn("REST request to evict all product cache");
        productService.evictAllCache();
        return ResponseEntity.noContent().build();
    }
}
