package com.alzheimer.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);

        System.out.println("\n╔═══════════���═══════��════════════════════════════════════╗");
        System.out.println("║                                                        ║");
        System.out.println("║        🚀 API GATEWAY STARTED SUCCESSFULLY 🚀          ║");
        System.out.println("║                                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println("📌 Gateway URL: http://localhost:9090");
        System.out.println("🔍 Eureka Discovery: http://localhost:8761");
        System.out.println("📊 Actuator: http://localhost:9090/actuator");
        System.out.println("⚙️  Load Balancer: Enabled with Eureka Service Discovery");
        System.out.println("🔐 Security: CSRF disabled for reactive gateway\n");
    }
}
