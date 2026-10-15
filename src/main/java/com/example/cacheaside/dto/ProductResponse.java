package com.example.cacheaside.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    String category,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {}
