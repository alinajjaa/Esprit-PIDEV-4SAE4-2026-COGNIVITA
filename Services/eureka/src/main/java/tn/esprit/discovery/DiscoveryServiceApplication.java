// src/main/java/tn/esprit/discovery/DiscoveryServiceApplication.java
package tn.esprit.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║     ✅ EUREKA SERVER STARTED          ║");
        System.out.println("║     📍 Port: 8761                     ║");
        System.out.println("║     📊 Dashboard: http://localhost:8761 ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }
}