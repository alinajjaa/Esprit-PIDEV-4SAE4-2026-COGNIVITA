package com.alzheimer.mmse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MMSEApplication {
    public static void main(String[] args) {
        SpringApplication.run(MMSEApplication.class, args);
    }
}
