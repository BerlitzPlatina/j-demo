package com.example.orm.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
// The second package holds the @MappedSuperclass shared through the common-jpa jar.
@EntityScan({ "com.example.orm.jpa.entity", "com.example.common.jpa.entity" })
public class OrmJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrmJpaApplication.class, args);
	}

}
