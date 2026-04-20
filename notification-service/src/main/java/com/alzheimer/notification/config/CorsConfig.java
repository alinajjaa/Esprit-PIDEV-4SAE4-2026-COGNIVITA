package com.alzheimer.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration.
 *
 * FIX: the original code used @CrossOrigin(origins = "*") on the controller,
 * which is dangerous in production — it allows any domain to call the API.
 * This config drives allowed origins from application.yml so that dev can
 * stay permissive while production is locked down via env var CORS_ALLOWED_ORIGINS.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${notification.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
