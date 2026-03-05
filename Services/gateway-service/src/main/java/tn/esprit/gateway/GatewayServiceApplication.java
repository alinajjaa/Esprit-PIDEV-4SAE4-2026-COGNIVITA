package tn.esprit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // ← AJOUTÉ pour l'enregistrement dans Eureka
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║     ✅ GATEWAY SERVICE STARTED       ║");
        System.out.println("║     📍 Port: 8080                     ║");
        System.out.println("║     📊 Eureka: http://localhost:8761  ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }
}