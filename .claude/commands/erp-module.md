---
description: Scaffold a new ERP feature package (entity, DTOs, mapper, DAO, service, controller, liquibase changelog) following the organization module
argument-hint: <feature> [field:type[:notnull,unique,max=N,min=N] ...]
allowed-tools: Bash(./scripts/gen-erp-module.sh:*), Bash(./mvnw:*), Read, Edit, Glob, Grep
---

# Scaffold an ERP feature package

The generator is [scripts/gen-erp-module.sh](scripts/gen-erp-module.sh). It mirrors the
reference module [erp/.../organization/](erp/src/main/java/com/example/erp/organization/):
entity on `AbstractAuditModel`, four DTO records, static mapper, `JpaRepository` DAO,
`@Transactional(readOnly = true)` service with a sort allowlist, `ApiResponse`-wrapped
controller, plus a numbered liquibase changelog registered in `changelog-master.yaml`.

Arguments: `$ARGUMENTS`

## Steps

1. Read the script's own usage if the arguments look unusual: `./scripts/gen-erp-module.sh --help`.
2. **Fields are the user's to give — never invent a schema.** If `$ARGUMENTS` carries a
   feature name but no field, ask for the fields with one `AskUserQuestion` (or plain
   question) before running anything. Do not pass a guessed field list, and do not run the
   script's interactive prompt — it reads from a terminal you do not have.
3. Preview first: `./scripts/gen-erp-module.sh --dry-run <feature> <fields...>`.
4. Write it: same command without `--dry-run`, then `--compile` or a separate
   `./mvnw -q -pl erp -am compile`.
5. Report the created files, the endpoints and the changelog entry. If the feature needs
   anything the generator does not cover — an extra derived query, a business rule such as
   organization's "exactly one default row", a lazy `@OneToMany` collection, seed data —
   add it by hand afterwards and say so.

## Field syntax

`name:type[:constraint,...]`

- type: `string` `text` `int` `long` `decimal` `bool` `date` `datetime` `ref(OtherEntity)`
- constraint: `notnull` `unique` `max=N` `min=N`
- `max=N` sets the VARCHAR length and `@Size(max = N)` for a string, `@Max(N)` for a number.
- `ref(Other)` generates the `@ManyToOne` owning side, a `<name>Id` field in every DTO, an
  `addForeignKeyConstraint` changeSet, and service-side resolution through `<Other>Dao`
  (assumed at `com.example.erp.<other>.repository.<Other>Dao` — check it exists).
- A NOT NULL boolean stays `Boolean` in the DTO and defaults to false in the mapper rather
  than being rejected.

Example:

```
./scripts/gen-erp-module.sh product \
    name:string:notnull,unique,max=255 \
    code:string:unique,max=50 \
    description:text \
    price:decimal:min=0 \
    active:bool:notnull \
    organization:'ref(Organization)':notnull
```

## House rules the generator already follows — keep them when editing afterwards

- A feature is a package, not a Maven module: do not touch `erp/pom.xml` unless the feature
  genuinely needs a new library.
- Entities never appear in a controller signature; mapping happens in the service, so lazy
  fields are still inside the transaction.
- Never renumber or edit an existing changeSet — they have already run on real databases.
- Comments and javadoc in English, explaining the *why*, and explicit imports (no wildcards).
