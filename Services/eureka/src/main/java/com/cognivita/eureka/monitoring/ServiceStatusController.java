package com.cognivita.eureka.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/monitoring")
public class ServiceStatusController {

    private final ServiceStatusService serviceStatusService;

    public ServiceStatusController(ServiceStatusService serviceStatusService) {
        this.serviceStatusService = serviceStatusService;
    }

    @GetMapping("/services")
    @ResponseBody
    public ResponseEntity<List<ServiceStatusDto>> getStatuses() {
        return ResponseEntity.ok(serviceStatusService.getServiceStatuses());
    }

    @GetMapping
    public String monitoringPage() {
        return "redirect:/monitoring.html";
    }
}
