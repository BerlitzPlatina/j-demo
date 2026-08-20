package com.example.migration.flyway.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Java migration: give every user without a department the default one.
 * <p>
 * Reach for a Java migration when the change needs logic a single SQL statement cannot express -
 * a conditional backfill, a computed value, chunked updates over a large table. Everything else
 * belongs in a {@code .sql} file, which is easier to read and to review.
 * <p>
 * The class name carries the version: {@code V5} plus a double underscore plus the description.
 * Flyway records and checksums it exactly like a SQL migration, so this file must not change
 * once it has run.
 */
public class V5__Assign_default_department extends BaseJavaMigration {

    private static final String FIND_DEFAULT_DEPARTMENT =
            "select id from orm_department where levels = 0 order by id limit 1";

    private static final String ASSIGN_USERS_WITHOUT_DEPARTMENT = """
            insert into orm_user_dept (user_id, dept_id)
            select u.id, ?
            from orm_user u
            where not exists (select 1 from orm_user_dept ud where ud.user_id = u.id)
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Integer defaultDepartmentId = findDefaultDepartmentId(context);
        if (defaultDepartmentId == null) {
            // Nothing to assign to; leaving the data untouched is the correct no-op.
            return;
        }
        try (PreparedStatement statement =
                     context.getConnection().prepareStatement(ASSIGN_USERS_WITHOUT_DEPARTMENT)) {
            statement.setInt(1, defaultDepartmentId);
            statement.executeUpdate();
        }
    }

    private Integer findDefaultDepartmentId(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(FIND_DEFAULT_DEPARTMENT)) {
            return resultSet.next() ? resultSet.getInt(1) : null;
        }
    }
}
