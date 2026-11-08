package com.example.gacha;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * The single entry point of the Gacha monolith.
 * <p>
 * Every feature lives in its own package under {@code com.example.gacha} - banner, and
 * whatever comes next - so this one component scan picks up their controllers, services and
 * repositories without any registration step. Adding a feature means adding a package, nothing
 * here changes.
 */
@SpringBootApplication
// Feature entities are found by the scan below com.example.gacha; the second package holds the
// @MappedSuperclass shared through the common-jpa jar, which sits outside that root.
@EntityScan({ "com.example.gacha", "com.example.common.jpa.entity" })
public class GachaApplication {

	public static void main(String[] args) {
		SpringApplication.run(GachaApplication.class, args);
	}

}
