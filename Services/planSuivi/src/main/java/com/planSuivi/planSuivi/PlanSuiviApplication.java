package com.planSuivi.planSuivi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PlanSuiviApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanSuiviApplication.class, args);
	}

}
