---
name: erp-migration
description: Turn a JSON API payload (or a described entity) into a Liquibase changelog and dev seed data for the erp module - schema changeSets, foreign keys, unique constraints, indexes, and a guarded seed file registered in changelog-master.yaml. Use when asked to write a migration, add a table/column, or seed data for erp.
---

# ERP migration and seed data

Liquibase owns the erp schema. `spring.jpa.hibernate.ddl-auto=validate`, so a mapping that a
changelog does not back fails the application at startup, not at request time. Changelogs live in
[erp/src/main/resources/db/changelog/](erp/src/main/resources/db/changelog/) and are registered in
[changelog-master.yaml](erp/src/main/resources/db/changelog/changelog-master.yaml).

Reference files to copy the style from: [4_contact.yml](erp/src/main/resources/db/changelog/4_contact.yml)
(schema) and [7_seed_contact.yml](erp/src/main/resources/db/changelog/7_seed_contact.yml) (seed).

## Hard rules

- **Never edit or renumber an existing changeSet.** They have run on real databases; Liquibase
  compares checksums and a changed one aborts startup. Corrections go in a new numbered file -
  that is exactly why `6_seed_organization_more.yml` exists.
- One file per feature, prefixed with the next free number: `<n>_<feature>.yml` for schema,
  `<n>_seed_<feature>.yml` for data. Append an `include` to `changelog-master.yaml` in the same
  order - master is read top to bottom, so a seed file must come after the tables it fills.
- Target database is MySQL. `BOOLEAN` becomes `TINYINT(1)`; that is already handled by
  `preferred_boolean_jdbc_type: TINYINT` in application.yml, so keep declaring `BOOLEAN`.
- Comments in English, and they explain *why* a column or guard exists, not what the YAML says.
- Author is the repo's existing author (`nam` today - match the neighbouring changeSets).
- Compile and, when a database is reachable, start the app once so `validate` proves the schema
  matches the entities: `./mvnw -q -pl erp -am compile`.

## Reading a JSON payload into a schema

Work from the payload the user gives. Decide per field:

| JSON | Column |
| --- | --- |
| external id string pointing at **another** entity (`location_id`, `currency_id`) | `VARCHAR(50)` - it is a foreign system's id, not our `BIGINT` PK |
| external id string for **this row itself** (`opening_balance_id`, `account_split_id`) | no column - see the judgement call below |
| our own row id | `BIGINT` autoIncrement primary key, always named `id` |
| money (`amount`, `bcy_amount`, `total`, balances) | `DECIMAL(19,4)` |
| rate (`exchange_rate`) | `DECIMAL(19,6)` |
| percentage (`tax_percentage`) | `DECIMAL(7,4)` |
| count / precision / term in days | `INT` |
| `"2013-10-01"` | `DATE`; a timestamp is `DATETIME` |
| short code (`currency_code`, `language_code`) | `VARCHAR(3)`/`VARCHAR(10)` |
| enum-ish word (`debit_or_credit`, `status`, `contact_type`) | `VARCHAR(20)`, documented in a comment |
| name / label | `VARCHAR(100)` or `VARCHAR(255)`; free text is `TEXT` |
| nested object | columns inlined on the parent when it is 1:1 and small, else its own table |
| array | a relation - **stop and ask** whether it is one-to-many (child table with a `<parent>_id` FK) or many-to-many (its own entity table plus a join table) |

Five judgement calls to make explicitly and mention in the report:

- **Request-time switches are not columns.** Fields that tell the server how to handle *this*
  call (`ignore_auto_number_generation`) or how to *render* a number (`price_precision`) carry no
  state worth persisting. Leave them out and say so in a comment, the way `contact_number` does.
- **Derived totals are not columns** unless the source of truth can drift. `"total": 10000` next
  to lines that sum to 2000 is the payload disagreeing with itself - ask, or store it and comment
  that it is the value the upstream system sent rather than a computed sum.
- **Only the relations the payload actually states.** A foreign key needs a field in the JSON to
  come from: a nested object or array is a relation, because the nesting says so, and an id
  field naming another entity is a relation once that entity has a table. Anything else is not.
  A payload that never mentions a contact does not become a child of `contacts` because it
  happens to sit near one in an API, and a payload with no `organization_id` does not get a
  tenancy column invented for it - that is guessing at a model, and a wrong guess is far more
  expensive to undo than a missing column is to add later. Where the payload is a fragment and
  the owning entity is genuinely unclear, ask before choosing a parent; do not default to the
  nearest table that already exists. Map the payload as a root table and say in the report that
  it stands alone because nothing in the JSON points anywhere.
- **An array is a relation to confirm, not to assume.** An array almost always means one-to-many
  or many-to-many, and the JSON alone cannot tell you which: the same nested list looks identical
  whether its elements belong to this parent alone or are shared entities the parent merely
  refers to. Ask the user which one it is before writing the tables - do not read the nesting as
  proof of ownership. The tell is whether an element has an identity of its own that outlives the
  parent: `accounts` carry an `account_id` from a chart of accounts every balance draws on, so
  they are shared and the relation is many-to-many - an `accounts` table plus an
  `opening_balance_accounts` join table holding the per-link attributes (`account_split_id`,
  `amount`, `exchange_rate`). A line that exists only inside its parent is one-to-many. Getting
  this wrong buries a shared entity inside a child table, and unpicking it later means moving
  data, not just adding a column.
- **An upstream id for the row itself is not a column.** Every table already has `id`; storing
  the source system's id for the same row is a second name for it, one more value that can go
  stale or disagree. Drop `opening_balance_id` from `opening_balances` and `account_split_id`
  from the link the way you drop `price_precision`. The distinction is which row the id names:
  `location_id` and `currency_id` stay, because they point at a *different* entity that has no
  table here - they are references outward, not duplicates of this row's key.

  Dropping them takes the table's business key with it, so replace it in the same breath or the
  table becomes unmatchable on re-import: `accounts` keys on `account_name` (made NOT NULL and
  unique), `opening_balances` on `(location_id, balance_date)`, a join table on its pair of
  foreign keys. Seed guards and rollbacks must name those same business columns - key them on an
  upstream id and they break silently the day that column is removed.

Naming: tables are plural snake_case, child tables are `<parent-singular>_<child-plural>`
(`opening_balances`, `opening_balance_accounts`) - the prefix names the table's real parent, so
a root table is not prefixed with an entity it has no column pointing at. Constraint names are
`fk_<table-singular>_<target>`, `uk_<table-singular>_<columns>`, `idx_<table-singular>_<columns>`.

## Schema file layout

Split the file into changeSets by kind, in this order, so a failure is easy to locate and roll
back individually:

1. `create-<x>` - one changeSet per table, parents first.
2. `add-<feature>-foreign-keys` - all `addForeignKeyConstraint` together. `onDelete: CASCADE` for
   a child that cannot outlive its parent; `SET NULL` for a pointer back into a child (see
   `fk_contact_primary_contact_person`).
3. `add-<feature>-unique-constraints` - business keys. Remember tenancy: a contact number is
   unique *per organization*, not globally.
4. `add-<feature>-indexes` - lead with the column every query filters on (`organization_id`).

Every table carries `id BIGINT autoIncrement`. Root entities also carry `create_time` and
`last_update_time` `DATETIME` for `AbstractAuditModel`; child rows written only through their
parent do not need them.

## Seed file rules

Seeds are development fixtures: `context: dev` on every changeSet.

- Guard on **the rows this file owns**, never on the table being empty. A changeSet whose
  precondition fails is recorded as MARK_RAN and never reconsidered, so "the table was empty at
  the time" silently loses the data forever - the mistake `6_seed_organization_more.yml` had to
  repair.

  ```yaml
  preConditions:
    - onFail: MARK_RAN
    - sqlCheck:
        expectedResult: 1
        sql: SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM organizations
  ```

- Insert with `WHERE NOT EXISTS` on that same business key, so re-running is a no-op.
- Do not hardcode parent ids. Join to the parent - `CROSS JOIN organizations` when every tenant
  should get the same catalog, or `JOIN ... ON` a business key when rows differ per parent. Where
  rows must be spread over an unknown number of parents, map them with
  `((ROW_NUMBER() OVER (ORDER BY id) - 1) % <n>) + 1` as in `5_seed_address.yml`.
- Shape the catalog as a `SELECT ... UNION ALL SELECT` block with the columns aligned, so a row
  reads as a record. Constant columns go in the outer `SELECT`, not repeated in every line.
- Give every changeSet a `rollback` - a `delete` on the same business key.
- Seed data must respect business invariants the service enforces (organization's "exactly one
  default org"): a seed never flips such a flag on rows it does not own.
- Values should be plausible and varied enough to exercise the API: mixed currencies, one
  inactive row, one row with children and one without, `NULL` where the column is optional.

## Worked example - the `opening_balance` payload

```json
{ "opening_balance_id": "460000000050041", "date": "2013-10-01", "location_id": "460000000038080",
  "price_precision": 2, "total": 10000,
  "accounts": [ { "account_split_id": "460000000050045", "account_id": "460000000000358",
                  "account_name": "Undeposited Funds", "debit_or_credit": "debit",
                  "exchange_rate": 1, "currency_id": "460000000000097", "currency_code": "USD",
                  "bcy_amount": 2000, "amount": 2000 } ] }
```

`accounts` is an array, so the relation was **confirmed with the user before writing anything**:
it is many-to-many, which makes this three tables, not two.

- `opening_balances` - the header: `date` DATE (as `balance_date`),
  `location_id` VARCHAR(50), `total` DECIMAL(19,4), keyed on `(location_id, balance_date)`. `price_precision` is
  dropped as display-only.
- `accounts` - the shared entity, because an account belongs to a chart of accounts that many
  balances draw on rather than to one balance: `account_name` VARCHAR(255), NOT NULL and unique.
- `opening_balance_accounts` - the join table, carrying everything that describes *this link*
  rather than either side: `debit_or_credit` VARCHAR(20),
  `currency_id` VARCHAR(50), `currency_code` VARCHAR(3), `exchange_rate` DECIMAL(19,6),
  `amount`/`bcy_amount` DECIMAL(19,4). `onDelete: CASCADE` towards the balance, which the link
  cannot outlive, and plain RESTRICT towards the account, which is shared and must survive.
  Unique on `(opening_balance_id, account_id)`.

Note `account_name` sits on `accounts`, not on the join table - repeating it per link is exactly
the duplication the many-to-many split exists to remove.

The only foreign keys are the join table's two, because `accounts` is nested inside the header
and nothing else in the payload points anywhere. In particular this fragment names no contact and
no organization, so both entities are root tables: this does **not** become
`contact_opening_balances` hanging off `contacts`, and it gets no `organization_id`.
`4_contact.yml` has its own flat `opening_balance_amount` column on a contact - a different thing
that happens to share a word, not this payload's parent.

No upstream id is stored: `opening_balance_id` and `account_split_id` would only restate each
row's own `id`. `location_id` and `currency_id` stay, because they name a branch and a currency
elsewhere rather than this row.

## Reporting back

Say which files were created, which `include` lines were added to master, which payload fields
were deliberately not stored and why, and whether the app was started against a database or only
compiled.
