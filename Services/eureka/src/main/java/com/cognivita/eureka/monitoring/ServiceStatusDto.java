package com.cognivita.eureka.monitoring;

public record ServiceStatusDto(
        String serviceName,
        String status,
        int upInstances,
        int totalInstances
) {
}
