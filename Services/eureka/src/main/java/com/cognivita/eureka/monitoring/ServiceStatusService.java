package com.cognivita.eureka.monitoring;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class ServiceStatusService {

    private final List<String> expectedServices;

    public ServiceStatusService(
            @Value("${monitoring.expected-services:GATEWAY-SERVICE,CONTENT-SERVICE,COMMUNITY-SERVICE,TRACKING-SERVICE}") String expectedServices) {
        this.expectedServices = Arrays.stream(expectedServices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<ServiceStatusDto> getServiceStatuses() {
        List<ServiceStatusDto> statuses = new ArrayList<>();
        PeerAwareInstanceRegistry registry = getRegistry();

        for (String serviceName : expectedServices) {
            ServiceStatusDto status = buildStatus(serviceName.toUpperCase(Locale.ROOT), registry);
            statuses.add(status);
        }
        return statuses;
    }

    private ServiceStatusDto buildStatus(String serviceName, PeerAwareInstanceRegistry registry) {
        if (registry == null) {
            return new ServiceStatusDto(serviceName, "DOWN", 0, 0);
        }

        Application application = registry.getApplication(serviceName);
        if (application == null) {
            return new ServiceStatusDto(serviceName, "DOWN", 0, 0);
        }

        int total = application.getInstances().size();
        int up = (int) application.getInstances()
                .stream()
                .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                .count();

        String status = up > 0 ? "UP" : "DOWN";
        return new ServiceStatusDto(serviceName, status, up, total);
    }

    private PeerAwareInstanceRegistry getRegistry() {
        if (EurekaServerContextHolder.getInstance() == null || EurekaServerContextHolder.getInstance().getServerContext() == null) {
            return null;
        }
        return EurekaServerContextHolder.getInstance().getServerContext().getRegistry();
    }
}
