package com.example.migration.flyway;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only strategy: wipe the schema, then migrate from version zero.
 * <p>
 * This is what makes the assertions deterministic - the tests exercise the whole migration
 * chain on an empty database, the same path a fresh environment takes, instead of whatever
 * state a previous run left behind.
 */
@TestConfiguration
public class CleanMigrationStrategy {

    @Bean
    FlywayMigrationStrategy cleanBeforeMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
