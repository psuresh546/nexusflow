package com.spawnbase.credential;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class CredentialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CredentialServiceApplication.class, args);
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


