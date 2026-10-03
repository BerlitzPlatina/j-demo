---
name: db-seed
description: Write a guarded Liquibase dev seed file for an existing erp table - context dev, NOT EXISTS guard on the business key, foreign keys resolved by joining on the parent's business key, and a rollback. Use when asked to seed data, add fixtures, or fill a table that already has a schema changelog.
---

# Seed data for an existing erp table

Produces `erp/src/main/resources/db/changelog/<n>_seed_<feature>.yml` and registers it in
`changelog-master.yaml`.

Use this when the table already exists. If the table still has to be created, use the
`erp-migration` skill - it writes schema and seed together.

## Before writing anything

Read the table's own changelog first (`<n>_<feature>.yml`) and confirm from it:

1. **Column names and nullability** - never guess. Audit columns differ by table age:
   older tables use `create_time` / `last_update_time`, newer ones `created_at` / `updated_at`
   plus `created_by` / `updated_by` (see `19_rename_audit_columns.yml`).
2. **The business key** - the NOT NULL unique column, or the composite unique key. This is what
   the guard and the rollback key on. If the table has no unique key at all, ask the user which
   column identifies a row before writing.
3. **Every NOT NULL column without a default** - each one must appear in the INSERT.
4. **Foreign keys** - the parent table and its business key, so the seed can join instead of
   hardcoding an id.

Check the master changelog for the next free file number, and place the include **after** the
tables the seed fills.

## The file shape

```yaml
databaseChangeLog:
  # One short paragraph: what these rows are, which business key the file owns, and any
  # non-obvious choice (why a column is NULL, why a flag is set on exactly one row).

  - changeSet:
      id: seed-<plural feature>
      author: nam
      context: dev
      changes:
        - sql:
            sql: |-
              INSERT INTO <table> (<columns>)
              SELECT <columns>
              FROM ( SELECT ... UNION ALL SELECT ... ) c
              WHERE NOT EXISTS (SELECT 1 FROM <table> x WHERE x.<key> = c.<key>)
      rollback:
        - delete:
            tableName: <table>
            where: >
              <key> IN (...)
```

## Rules

- **`context: dev` on every changeSet.** These are fixtures, not production data.
- **Guard on the rows this file owns, never on the table being empty.** A changeSet whose
  precondition fails is recorded as MARK_RAN and never reconsidered, so "the table was empty at
  the time" loses the rows forever - the mistake `6_seed_organization_more.yml` had to repair.
  The `WHERE NOT EXISTS` on the business key is the guard; re-running is then a no-op.
  A `preConditions` block is only for a dependency the insert cannot express, e.g. "the parent
  table has at least one row":

  ```yaml
  preConditions:
    - onFail: MARK_RAN
    - sqlCheck:
        expectedResult: 1
        sql: SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM legal_entities
  ```

- **Composite business key** - repeat every column of the unique key in the NOT EXISTS, and in
  the rollback use an OR-ed pair list or a tuple `IN`, not just the code column.
- **Never hardcode a foreign key id.** Resolve it by joining the parent on its business key:

  ```sql
  JOIN legal_entities le ON le.entity_code = c.entity_code
  ```

  - same catalog for every parent: `CROSS JOIN <parent>`
  - rows differ per parent: `JOIN ... ON` the parent's business key
  - spread rows over an unknown number of parents:
    `((ROW_NUMBER() OVER (ORDER BY id) - 1) % <n>) + 1`, as in `5_seed_address.yml`
  - nullable FK: use a `LEFT JOIN` so a row with no parent still inserts, and give at least one
    row a NULL parent so the optional relation is exercised

- **Shape the catalog as `SELECT ... UNION ALL SELECT`** with columns aligned in a grid, so each
  line reads as a record. Name the columns with `AS` on the first line only. Put genuinely
  constant columns (`source`, `created_by`) in the outer `SELECT`, not on every line.
- **Always write a `rollback`** - a `delete` on the same business key.
- **Respect business invariants the service enforces** (the "exactly one default" style flag):
  set it on at most one row, and never flip it on rows this file does not own.
- **Make the data exercise the API**: varied timestamps so a sort has something to order, one
  inactive/archived row, one row with children and one without, `NULL` in the optional columns,
  and enough rows to page (10+ when the endpoint is a list).
- **English only** in comments and in the data, matching the rest of the changelogs.

## Worked example - seeding `positions` (two FKs, one nullable, composite key)

`positions` is keyed on `(legal_entity_id, position_code)` and on `legacy_position_id`, and has
a nullable `job_id`. So: join `legal_entities` on `entity_code`, LEFT JOIN `jobs` on
`(legal_entity_id, job_code)`, and guard on both unique keys.

```yaml
databaseChangeLog:
  # Development fixtures for positions.
  #
  # Rows are owned by (entity_code, position_code) and by legacy_position_id, so both unique
  # keys are guarded. job_code is NULL on the unassigned positions, which keeps the optional
  # job relation exercised. The table carries no created_by/updated_by, so neither is inserted.

  - changeSet:
      id: seed-positions
      author: nam
      context: dev
      preConditions:
        - onFail: MARK_RAN
        - sqlCheck:
            expectedResult: 1
            sql: SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM legal_entities
      changes:
        - sql:
            sql: |-
              INSERT INTO positions (
                  legal_entity_id, job_id, legacy_position_id, position_code,
                  legacy_position_code, position_name, position_status,
                  reference_quality, source, created_at, updated_at
              )
              SELECT
                  le.id, j.id, c.legacy_position_id, c.position_code,
                  c.legacy_position_code, c.position_name, c.position_status,
                  'legacy_position_identity_only', 'manual-seed',
                  c.created_at, c.updated_at
              FROM (
                            SELECT 'LE-VNM-001' AS entity_code, 'JOB-TEACHER'  AS job_code, 9001 AS legacy_position_id, 'POS-VN-0001' AS position_code, 'LP-0001' AS legacy_position_code, 'English Teacher - District 1'  AS position_name, 'active'   AS position_status, '2024-03-04 08:15:00' AS created_at, '2024-03-04 08:15:00' AS updated_at
                  UNION ALL SELECT 'LE-VNM-001',                'JOB-TEACHER',            9002,                        'POS-VN-0002',                 'LP-0002',                       'English Teacher - District 7',                    'active',                     '2024-03-04 08:15:00',               '2024-09-11 10:40:00'
                  UNION ALL SELECT 'LE-VNM-001',                NULL,                     9003,                        'POS-VN-0003',                 NULL,                            'Centre Receptionist',                            'inactive',                   '2024-05-20 13:00:00',               '2024-05-20 13:00:00'
                  UNION ALL SELECT 'LE-SGP-002',                'JOB-ADMIN',              9101,                        'POS-SG-0001',                 'LP-1001',                       'Operations Administrator',                       'active',                     '2024-06-02 09:30:00',               '2024-06-02 09:30:00'
              ) c
              JOIN legal_entities le
                  ON le.entity_code = c.entity_code
              LEFT JOIN jobs j
                  ON j.legal_entity_id = le.id
                 AND j.job_code = c.job_code
              WHERE NOT EXISTS (
                  SELECT 1 FROM positions x
                  WHERE x.legal_entity_id = le.id
                    AND x.position_code = c.position_code
              )
              AND NOT EXISTS (
                  SELECT 1 FROM positions y
                  WHERE y.legacy_position_id = c.legacy_position_id
              )
      rollback:
        - delete:
            tableName: positions
            where: >
              legacy_position_id IN (9001, 9002, 9003, 9101)
```

Then append to `changelog-master.yaml`:

```yaml
  - include:
      file: db/changelog/<n>_seed_position.yml
```

## Checklist before handing the file over

- [ ] `context: dev` present
- [ ] every NOT NULL column without a default is in the INSERT
- [ ] guard covers **every** unique key, not just the first
- [ ] no literal foreign key id anywhere
- [ ] rollback keys on the same business key as the guard
- [ ] include appended to `changelog-master.yaml`, after the tables it fills
- [ ] audit column names taken from this table's own changelog
