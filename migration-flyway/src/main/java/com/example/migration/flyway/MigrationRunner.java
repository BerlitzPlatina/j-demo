package com.example.migration.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.RepairResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns the module into a command line migration tool.
 * <p>
 * Spring Boot has already run {@code migrate} by the time this runs - that is what the Flyway
 * auto-configuration does on startup - so this reports the result and offers the two commands
 * that are useful afterwards:
 * <pre>
 *   mvn spring-boot:run                                  # migrate, then print the history
 *   mvn spring-boot:run -Dspring-boot.run.arguments=validate
 *   mvn spring-boot:run -Dspring-boot.run.arguments=repair
 * </pre>
 * {@code clean} is deliberately not offered: it drops every object in the schema, and
 * {@code spring.flyway.clean-disabled} keeps it off in configuration as well.
 */
@Component
public class MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final Flyway flyway;

    public MigrationRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(ApplicationArguments args) {
        String command = args.getNonOptionArgs().isEmpty() ? "info" : args.getNonOptionArgs().get(0);

        switch (command) {
            case "info" -> printInfo();
            case "validate" -> validate();
            case "repair" -> repair();
            default -> log.error("Unknown command: {}. Use one of: info, validate, repair", command);
        }
    }

    /** What has been applied, what is still pending, and in which order. */
    private void printInfo() {
        MigrationInfoService info = flyway.info();
        log.info("Schema version: {}", info.current() == null ? "<empty schema>" : info.current().getVersion());

        for (MigrationInfo migration : info.all()) {
            log.info("  {} | {} | {} | {}",
                    migration.getVersion() == null ? "repeatable" : migration.getVersion(),
                    migration.getState(),
                    migration.getInstalledOn() == null ? "-" : migration.getInstalledOn(),
                    migration.getDescription());
        }

        List<MigrationInfo> pending = List.of(info.pending());
        log.info("Applied: {}, pending: {}", info.applied().length, pending.size());
    }

    /**
     * Fails when an applied migration no longer matches the file on disk, or when a migration is
     * missing. Worth running in CI against a copy of production.
     */
    private void validate() {
        flyway.validate();
        log.info("Validation passed: history matches the migrations on the classpath");
    }

    /**
     * Cleans up the history table itself: removes failed entries and realigns checksums after a
     * migration file was legitimately corrected. It does not touch the data.
     */
    private void repair() {
        RepairResult result = flyway.repair();
        log.info("Repair finished. Removed: {}, aligned: {}",
                result.migrationsRemoved.size(), result.migrationsAligned.size());
    }
}
