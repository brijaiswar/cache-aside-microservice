package com.example.cacheaside.dto;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message
) {}
