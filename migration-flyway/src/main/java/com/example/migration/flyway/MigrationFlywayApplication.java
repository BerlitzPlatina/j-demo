package com.example.migration.flyway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Database migration tool for the demo schema.
 * <p>
 * Not a server: it starts, lets the Flyway auto-configuration bring the database up to the
 * latest version, reports what it did through {@link MigrationRunner} and exits. Run it before
 * the application modules, or from CI as a deploy step.
 */
@SpringBootApplication
public class MigrationFlywayApplication {

	public static void main(String[] args) {
		SpringApplication.run(MigrationFlywayApplication.class, args);
	}

}
