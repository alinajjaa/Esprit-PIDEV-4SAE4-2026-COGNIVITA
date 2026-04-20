package com.alzheimer.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CorsConfig Unit Tests")
class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    @DisplayName("corsWebFilter: bean is created successfully")
    void corsWebFilter_beanCreated() {
        CorsWebFilter filter = corsConfig.corsWebFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("corsWebFilter: creates a non-null filter instance")
    void corsWebFilter_isNonNull() {
        CorsWebFilter filter1 = corsConfig.corsWebFilter();
        CorsWebFilter filter2 = corsConfig.corsWebFilter();

        assertThat(filter1).isNotNull();
        assertThat(filter2).isNotNull();
    }
}
