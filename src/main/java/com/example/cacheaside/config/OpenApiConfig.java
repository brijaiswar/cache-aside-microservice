package com.example.cacheaside.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cacheAsideOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Cache-Aside Microservice API")
                .description("RESTful API for Product management with Redis cache-aside pattern and PostgreSQL persistence")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Cache-Aside Microservice")
                    .email("support@example.com")));
    }
}
