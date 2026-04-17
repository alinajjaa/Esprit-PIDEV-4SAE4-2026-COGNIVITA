package com.alzheimer.medicalrecords.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS + RestTemplate configuration.
 *
 * RestTemplate is still needed for:
 *   - PdfReportService: on-demand read of family-tree data when generating a PDF (acceptable blocking call)
 *   - UserService: fallback user sync from user-service (read-only, cached after first hit)
 *
 * Both uses have a 5-second connect + 10-second read timeout so a slow downstream
 * service never hangs a thread indefinitely.
 */
@Configuration
public class CorsConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);   // 5s connect timeout
        factory.setReadTimeout(10_000);      // 10s read timeout
        return new RestTemplate(factory);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
