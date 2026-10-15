package com.example.cacheaside.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Name is required") String name,
    String description,
    @NotNull(message = "Price is required") @DecimalMin(value = "0.0", message = "Price must be non-negative") BigDecimal price,
    @NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock quantity must be non-negative") Integer stockQuantity,
    String category
) {}
