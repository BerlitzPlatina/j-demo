package com.example.migration.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the migrations against a throwaway database and checks the result.
 * <p>
 * The point of these tests is that a broken migration fails here, in CI, instead of halfway
 * through a deploy.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CleanMigrationStrategy.class)
class MigrationFlywayApplicationTests {

    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyMigrationApplied() {
        // Boot already ran migrate() on startup.
        assertThat(flyway.info().pending()).isEmpty();

        List<MigrationInfo> applied = List.of(flyway.info().applied());
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(migration ->
                assertThat(migration.getState().isFailed()).isFalse());
        assertThat(applied).extracting(MigrationInfo::getState)
                .allMatch(state -> state == MigrationState.SUCCESS || state == MigrationState.BASELINE);
    }

    @Test
    void historyMatchesTheMigrationsOnDisk() {
        // Fails if an applied migration file was edited after the fact.
        flyway.validate();
    }

    @Test
    void schemaHasTheExpectedShape() {
        assertThat(tableExists("orm_user")).isTrue();
        assertThat(tableExists("orm_department")).isTrue();
        assertThat(tableExists("orm_user_dept")).isTrue();

        // V3 added these.
        assertThat(indexExists("orm_user", "idx_user_name")).isTrue();
        assertThat(indexExists("orm_user", "idx_user_create_time")).isTrue();
    }

    @Test
    void seedDataAndPlaceholderResolved() {
        // V4 seeded reference data, with ${defaultDepartment} resolved from the config.
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from orm_department where levels = 0 and name = 'Head Office'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from orm_user", Integer.class))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void javaMigrationBackfilledDepartments() {
        // V5 assigned every user without a department to the default one, so none is left over.
        Integer usersWithoutDepartment = jdbcTemplate.queryForObject("""
                select count(*) from orm_user u
                where not exists (select 1 from orm_user_dept ud where ud.user_id = u.id)
                """, Integer.class);
        assertThat(usersWithoutDepartment).isZero();
    }

    @Test
    void repeatableMigrationCreatedTheView() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from user_directory", Integer.class)).isPositive();
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database() and table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database() and table_name = ? and index_name = ?
                """, Integer.class, table, index);
        return count != null && count > 0;
    }
}
