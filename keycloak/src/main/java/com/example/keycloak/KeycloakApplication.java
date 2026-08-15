package com.example.keycloak;

import org.springframework.boot.SpringApplication;
import com.example.keycloak.config.KeycloakProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakApplication {

	public static void main(String[] args) {
		SpringApplication.run(KeycloakApplication.class, args);
	}

}
