package com.example.keycloak;

import com.example.common.security.ResourceServerSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * The resource server setup lives in the common-security jar, outside this application's
 * component-scan root, so it is pulled in explicitly here. {@code keycloak.public-paths} in
 * application.yml decides which endpoints stay open.
 */
@SpringBootApplication
@Import(ResourceServerSecurityConfig.class)
public class KeycloakApplication {

	public static void main(String[] args) {
		SpringApplication.run(KeycloakApplication.class, args);
	}

}
