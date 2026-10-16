package com.example.cacheaside.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration for automatic auditing of entity timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}