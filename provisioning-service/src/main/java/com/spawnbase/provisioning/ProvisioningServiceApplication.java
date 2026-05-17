package com.spawnbase.provisioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class ProvisioningServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProvisioningServiceApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins(
								"http://localhost:3000",  // React UI
								"http://localhost:8080"   // API Gateway
						)
						.allowedMethods(
								"GET","POST","PATCH",
								"PUT","DELETE","OPTIONS");
			}
		};
	}
}


