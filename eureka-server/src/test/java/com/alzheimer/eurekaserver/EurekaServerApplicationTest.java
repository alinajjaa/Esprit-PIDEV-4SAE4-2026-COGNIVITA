package com.alzheimer.eurekaserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EurekaServerApplication Unit Tests")
class EurekaServerApplicationTest {

    @Test
    @DisplayName("main class can be instantiated")
    void application_canBeInstantiated() {
        EurekaServerApplication app = new EurekaServerApplication();

        assertThat(app).isNotNull();
    }

    @Test
    @DisplayName("application class has @EnableEurekaServer annotation")
    void application_hasEnableEurekaServerAnnotation() {
        boolean hasAnnotation = EurekaServerApplication.class
                .isAnnotationPresent(
                        org.springframework.cloud.netflix.eureka.server.EnableEurekaServer.class);

        assertThat(hasAnnotation).isTrue();
    }

    @Test
    @DisplayName("application class has @SpringBootApplication annotation")
    void application_hasSpringBootApplicationAnnotation() {
        boolean hasAnnotation = EurekaServerApplication.class
                .isAnnotationPresent(
                        org.springframework.boot.autoconfigure.SpringBootApplication.class);

        assertThat(hasAnnotation).isTrue();
    }
}
