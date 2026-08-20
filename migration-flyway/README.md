# migration-flyway

Database migrations for the demo schema, driven by Flyway. Not a server: it starts, brings the
database up to the latest version, prints what it did and exits.

## Run it

```bash
./mvnw spring-boot:run                                          # migrate, then print the history
./mvnw spring-boot:run -Dspring-boot.run.arguments=validate      # history still matches the files?
./mvnw spring-boot:run -Dspring-boot.run.arguments=repair        # clean up failed/changed history rows
./mvnw test                                                      # clean + migrate on flyway_demo_test
```

Target database comes from `spring.datasource.url` in `src/main/resources/application.yml`
(`flyway_demo`, created on first connect). Point it elsewhere without editing the file:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://host:3306/appdb"
```

## What is where

| File | Kind | Runs |
| --- | --- | --- |
| `db/migration/V1__create_user_table.sql` | versioned | once, in version order |
| `db/migration/V2__create_department_tables.sql` | versioned | once |
| `db/migration/V3__index_user_lookups.sql` | versioned | once |
| `db/migration/V4__seed_reference_data.sql` | versioned | once, idempotent SQL |
| `migration/V5__Assign_default_department.java` | versioned, Java | once |
| `db/migration/R__user_directory_view.sql` | repeatable | after the versioned ones, again whenever its checksum changes |

Flyway records every applied migration in `flyway_schema_history` with a checksum.

## Rules that keep this working

- **Never edit an applied migration.** The checksum stops matching and `validate` fails for
  everyone. Fix forward with a new version instead.
- **One version number per change, never reused.** Two developers picking `V6` is a merge
  conflict on purpose - `out-of-order: false` makes it fail loudly rather than interleave.
- **Versioned for schema changes, repeatable for objects you redefine** (views, procedures).
- **`clean` is disabled** (`clean-disabled: true`); it drops every object in the schema. Only the
  test profile turns it on, against `flyway_demo_test`.
- **Placeholder names are snake_case.** `spring.flyway.placeholders` keys go through relaxed
  configuration binding, which lowercases camelCase, and the SQL would then not resolve.

## Taking over a database that already has tables

`baseline-on-migrate: true` handles this: Flyway writes a baseline row at
`baseline-version: 0` and applies everything above it, instead of failing on a non-empty schema.
So for `appdb`, which the `orm-jpa` module currently builds with `spring.sql.init` and
`schema.sql`, the switch is:

1. Point this module's datasource at `appdb` and run it.
2. Set `spring.sql.init.mode: never` in `orm-jpa`, and delete its `schema.sql` / `data.sql`.

From then on the schema has one owner and one history, instead of being recreated on every boot.
