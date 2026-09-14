---
name: sql-to-gen-command
description: Convert SQL table definitions or JSON entity schemas into gen-erp-module.sh bash commands. Generates the exact command syntax to scaffold an ERP feature module from a database schema or entity specification.
---

# SQL to Gen-ERP-Module Command

Convert database table definitions, SQL schemas, or JSON entity payloads into executable bash commands using `gen-erp-module.sh`. This skill generates the exact command-line syntax needed to scaffold a complete ERP feature package with entity, DTOs, mapper, DAO, service, and controller.

## Command Structure

The `gen-erp-module.sh` script uses this format:

```bash
./scripts/gen-erp-module.sh [options] <feature> [field ...]
```

### Feature Name

- **Singular form**: `product`, `customer`, `purchase-order`, or PascalCase: `PurchaseOrder`
- Should match the database table name (singular form, kebab-case)

### Field Format

Each field follows: `name:type[:constraint,...]`

**Types:**
- `string` - VARCHAR column (default length 255)
- `text` - TEXT column for longer content
- `int` - INT column for integers
- `long` - BIGINT column for large numbers
- `decimal` - DECIMAL column for money/precise numbers
- `bool` - BOOLEAN column
- `date` - DATE column
- `datetime` - DATETIME/TIMESTAMP column
- `ref(OtherEntity)` - Foreign key reference to another entity

**Constraints:**
- `notnull` - NOT NULL constraint
- `unique` - UNIQUE constraint
- `max=N` - String length limit (VARCHAR) or maximum value (numbers)
- `min=N` - Minimum value for numbers
- Constraints separated by commas: `name:string:notnull,unique,max=255`

### Options

- `--dry-run` - Print files that would be written, don't create them
- `--compile` - Run Maven compile on the erp module after generation
- `--force` - Overwrite existing feature package
- `-h`, `--help` - Show help text

## Conversion Logic

### From SQL to Field Syntax

| SQL | Gen Command Field |
|-----|-------------------|
| `VARCHAR(255) NOT NULL UNIQUE` | `fieldName:string:notnull,unique,max=255` |
| `DECIMAL(19,4)` | `fieldName:decimal` |
| `INT NOT NULL` | `fieldName:int:notnull` |
| `BIGINT AUTO_INCREMENT PRIMARY KEY` | Auto-generated as `id` (no need to specify) |
| `DATE` | `fieldName:date` |
| `DATETIME` | `fieldName:datetime` |
| `TEXT` | `fieldName:text` |
| `BOOLEAN` | `fieldName:bool` |
| `BIGINT` (FK to another table) | `fieldName:ref(OtherEntity)` |

### Field Priority

When extracting fields from SQL:
1. Skip auto-increment `id` primary key - it's always created
2. Skip `created_at`, `updated_at` if present - handled by JPA `@CreationTimestamp`, `@UpdateTimestamp`
3. Extract constraint information: NULL/NOT NULL, UNIQUE, CHECK, DEFAULT values
4. Map SQL types to gen-command types
5. Preserve field order from schema

## Examples

### Simple Product Entity

**SQL:**
```sql
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    price DECIMAL(19,4) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE
);
```

**Generated Command:**
```bash
./scripts/gen-erp-module.sh product \
    name:string:notnull,unique,max=255 \
    price:decimal:notnull \
    description:text \
    active:bool
```

### Complex Purchase Order

**SQL:**
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

**Generated Command:**
```bash
./scripts/gen-erp-module.sh purchase-order \
    order_number:string:notnull,unique,max=50 \
    vendor_id:ref(Vendor):notnull \
    status:string:max=20 \
    total_amount:decimal \
    order_date:date:notnull
```

### From JSON Entity Payload

**JSON Payload:**
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

**Generated Command:**
```bash
./scripts/gen-erp-module.sh customer \
    name:string:notnull,unique,max=255 \
    email:string:notnull,unique,max=100 \
    phone:string:max=20 \
    credit_limit:decimal \
    is_active:bool
```

## Step-by-Step Process

When given a SQL table definition, JSON schema, or entity description:

1. **Identify Entity Name**
   - Extract table/entity name
   - Convert to singular form if needed
   - Use kebab-case or PascalCase format

2. **Extract Fields**
   - List all non-PK columns
   - Note NULL/NOT NULL constraints
   - Identify UNIQUE constraints
   - Extract VARCHAR lengths, DECIMAL precision
   - Mark foreign keys with `ref(EntityName)`

3. **Map Data Types**
   - Use conversion table above
   - Preserve type fidelity (text vs string, decimal vs int)
   - Handle precision (DECIMAL(19,4) stays decimal, no suffix)

4. **Assemble Constraints**
   - Combine multiple constraints with commas
   - Order: notnull first, then unique, then size constraints
   - Format: `name:type:constraint1,constraint2,constraint3`

5. **Format Command**
   - Single line or multi-line for readability
   - Backslash continuation for multiple fields
   - Place feature name first, fields in order

6. **Optional Flags**
   - Add `--dry-run` to preview without creating files
   - Add `--compile` if Maven compilation is needed
   - Add `--force` only if overwriting existing module

## Best Practices

- **Keep field order consistent** with source SQL/schema
- **Use max constraints** for strings - extract VARCHAR length
- **Reference other entities** only when they're separate tables (use `ref()`)
- **Test with `--dry-run`** before actual generation
- **Follow naming conventions**: kebab-case for features with hyphens, PascalCase without hyphens
- **Consider semantic meaning** - `DECIMAL(19,4)` for money, not just any decimal
- **Validate field names** - use camelCase, no spaces or special chars except underscores

## Output

The skill outputs:
- **Formatted bash command** ready to copy and paste
- **Field breakdown** (name → type mapping)
- **Constraint summary** (NOT NULL, UNIQUE, sizes)
- **Optional flags** suggested based on use case
- **File preview** when `--dry-run` is included (showing what files would be created)

---

**Reference:** [gen-erp-module.sh](./scripts/gen-erp-module.sh) in the project root
