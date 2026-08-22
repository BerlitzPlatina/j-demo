package com.example.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * The single entry point of the ERP monolith.
 * <p>
 * Every feature lives in its own package under {@code com.example.erp} - organization, and
 * whatever comes next - so this one component scan picks up their controllers, services and
 * repositories without any registration step. Adding a feature means adding a package, nothing
 * here changes.
 */
@SpringBootApplication
// Feature entities are found by the scan below com.example.erp; the second package holds the
// @MappedSuperclass shared through the common-jpa jar, which sits outside that root.
@EntityScan({ "com.example.erp", "com.example.common.jpa.entity" })
public class ErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpApplication.class, args);
	}

}
