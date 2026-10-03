---
name: sql-to-gen-command
description: Convert SQL table definitions or JSON entity schemas into gen-erp-module.sh bash commands. Generates the exact command syntax to scaffold an ERP feature module from a database schema or entity specification.
---

# SQL to Gen-ERP-Module Command

Convert a `CREATE TABLE` statement, SQL schema or JSON entity payload into an executable
`scripts/gen-erp-module.sh` command that scaffolds an ERP feature package (entity, four DTO
records, mapper, DAO, service, controller and a Liquibase changelog registered in
`changelog-master.yaml`).

The generator covers only what is listed below. Anything it cannot express - composite unique
keys, indexes, column defaults - has to be hand-added to the generated changelog afterwards.
Always say so in the output instead of silently dropping it.

## Command Structure

```bash
./scripts/gen-erp-module.sh [options] <feature> [field ...]
```

### Feature Name

- Singular: `product`, `customer`, `purchase-order`, or PascalCase `PurchaseOrder`.
- The script derives everything else from it and **the table name cannot be overridden**:
  - Java package `com.example.erp.<snakewithoutunderscores>` (`purchase-order` -> `purchaseorder`)
  - entity `PurchaseOrder`, table `purchase_orders` (naive pluralizer), route `/api/purchase-orders`
- Source tables in this project carry a `0_hrm_` style prefix that the repo convention drops:
  `0_hrm_legal_entities` is modelled as table `legal_entities`. Strip the prefix when picking
  the feature name.

### Field Format

Each field is one argument: `name:type[:constraint,...]`

**Types** (exactly these, nothing else is accepted):

| type | Java | SQL emitted |
|------|------|-------------|
| `string` | `String` | `VARCHAR(max, default 255)` |
| `text` | `String` | `TEXT` |
| `int` | `Integer` | `INT` |
| `long` | `Long` | `BIGINT` |
| `decimal` | `BigDecimal` | `DECIMAL(19, 2)` - precision is **hardcoded**, not taken from the SQL |
| `bool` | `Boolean` | `BOOLEAN` |
| `date` | `LocalDate` | `DATE` |
| `datetime` | `LocalDateTime` | `DATETIME` |
| `ref(Other)` | `Other` (`@ManyToOne`) | `BIGINT` + `addForeignKeyConstraint` |

**Constraints:** `notnull`, `unique`, `max=N`, `min=N`, comma-separated.

- On strings `max=N` sets the `VARCHAR` length and `@Size(max = N)`; on numbers it is `@Max(N)`.
- `min=N` is `@Min(N)`, numbers only.
- `notnull` gives `nullable = false` plus `@NotBlank` (String) / `@NotNull` (others) in the
  create and update DTOs. A `notnull` boolean is defaulted by the mapper, not validated.
- `unique` is **single-column only**.

### Two rules that are easy to get wrong

**1. Quote every `ref(...)` - parentheses are bash syntax.**

```bash
owner:'ref(Organization)':notnull     # correct
owner:ref(Organization):notnull       # bash: syntax error near unexpected token `('
```

**2. Name a ref field after the relation, not after the FK column.**
The script appends `_id` itself: `owner:'ref(Organization)'` produces the column `owner_id`,
the entity field `private Organization owner`, and the DTO field `Long ownerId`.
Passing `owner_id:'ref(Organization)'` would produce the column `owner_id_id`.

The ref target must already exist as a feature package - the entity is imported from
`com.example.erp.<target>.entity.<Target>` and the FK points at `plural(snake(Target))`.

### Options

- `--dry-run` - print every file that would be written, write nothing
- `--compile` - run `./mvnw -q -pl erp -am compile` afterwards
- `--force` - overwrite an existing feature package (required if the package already exists)
- `-h`, `--help`

## What the generator adds or ignores by itself

- `id BIGINT AUTO_INCREMENT PRIMARY KEY` is always created - never pass it.
- `created_at` / `updated_at` come from `AbstractAuditModel` and are emitted into the changelog -
  never pass them.
- `created_by` / `updated_by` are **not** generated. Existing changelogs in this repo carry them,
  so add them by hand if the table needs them.
- The changelog is written as `db/changelog/<next number>_<pkg>.yml` and appended to
  `changelog-master.yaml`.
- List/search: the first `string`/`text` field carries the keyword search, unless one field is
  literally named `name`, which then wins. If no string field exists there is no keyword search.

## Not supported - hand-edit the changelog afterwards

- composite `UNIQUE KEY (a, b)`
- secondary `KEY` / indexes
- `DEFAULT` values
- `smallint` / `tinyint` / `unsigned` (use `int`, adjust the column type by hand)
- `CHECK` constraints, enums, `ON DELETE`/`ON UPDATE` actions (FKs are emitted without them)
- custom `DECIMAL` precision

## Conversion Logic

1. **Entity name** - table name, prefix stripped, singular, kebab-case or PascalCase.
2. **Fields** - every column except the auto-increment PK and the audit timestamps, in schema
   order. Field names may be given in `snake_case` or `camelCase`; the script normalises to
   `camelCase` for Java and `snake_case` for the column.
3. **Types** - use the table above.
4. **Constraints** - `notnull` first, then `unique`, then `max=`/`min=`.
5. **FKs** - relation name + quoted `ref(Target)`.
6. Report anything from the "not supported" list that the source schema contained.

## Examples

### Simple product

```sql
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    price DECIMAL(19,4) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE
);
```

```bash
./scripts/gen-erp-module.sh product \
    name:string:notnull,unique,max=255 \
    price:decimal:notnull \
    description:text \
    active:bool
```

Hand-edit afterwards: `price` becomes `DECIMAL(19, 2)`; `active` loses `DEFAULT TRUE`.

### Purchase order with a foreign key

```sql
CREATE TABLE purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL,
    status VARCHAR(20),
    total_amount DECIMAL(19,4),
    order_date DATE NOT NULL
);
```

```bash
./scripts/gen-erp-module.sh purchase-order \
    order_number:string:notnull,unique,max=50 \
    vendor:'ref(Vendor)':notnull \
    status:string:max=20 \
    total_amount:decimal \
    order_date:date:notnull
```

`vendor` -> column `vendor_id`, FK to `vendors(id)`, DTO field `Long vendorId`.

### Table with a composite unique key

```sql
CREATE TABLE `0_hrm_jobs` (
  `job_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `legal_entity_id` bigint unsigned NOT NULL,
  `job_code` varchar(32) NOT NULL,
  `job_name` varchar(120) NOT NULL,
  `job_status` varchar(16) NOT NULL DEFAULT 'active',
  ...
  PRIMARY KEY (`job_id`),
  UNIQUE KEY `hrm_job_entity_code_uq` (`legal_entity_id`,`job_code`),
  KEY `hrm_job_status_idx` (`legal_entity_id`,`job_status`,`job_id`),
  CONSTRAINT `0_hrm_jobs_legal_entity_fk` FOREIGN KEY (`legal_entity_id`)
      REFERENCES `0_hrm_legal_entities` (`legal_entity_id`)
);
```

```bash
./scripts/gen-erp-module.sh --dry-run job \
    legalEntity:'ref(LegalEntity)':notnull \
    jobCode:string:notnull,max=32 \
    jobName:string:notnull,max=120 \
    jobStatus:string:notnull,max=16
```

Note: `job_code` is **not** marked `unique` - the uniqueness is composite
`(legal_entity_id, job_code)` and has to be added to the changelog by hand, together with
`hrm_job_status_idx` and the `DEFAULT 'active'`. The keyword search lands on `jobCode`,
since no field is named `name`.

### From a JSON payload

```json
{
  "entityName": "customer",
  "fields": [
    { "name": "name", "type": "string", "length": 255, "nullable": false, "unique": true },
    { "name": "email", "type": "string", "length": 100, "nullable": false, "unique": true },
    { "name": "phone", "type": "string", "length": 20 },
    { "name": "credit_limit", "type": "decimal" },
    { "name": "is_active", "type": "boolean" }
  ]
}
```

```bash
./scripts/gen-erp-module.sh customer \
    name:string:notnull,unique,max=255 \
    email:string:notnull,unique,max=100 \
    phone:string:max=20 \
    credit_limit:decimal \
    is_active:bool
```

## Output

Report, in this order:

1. the formatted bash command, `--dry-run` first when the module is new
2. the field breakdown (column -> Java field : type), including the `_id` suffix on refs
3. anything the generator cannot express, as an explicit follow-up list of manual changelog edits
4. whether `--force` is needed because the feature package already exists

---

**Reference:** `scripts/gen-erp-module.sh` at the repository root (`--help` prints the same grammar).
