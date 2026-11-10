---
name: postman-mcp-request-builder
description: Create, update, and verify Postman requests via Postman MCP from Spring Boot controllers, DTOs, OpenAPI specs, or explicit endpoint lists. Use when adding endpoints to a Postman collection.
---

# Postman MCP Request Builder

Goal: every request added to Postman is accurate (derived from source, never invented) and openable (verified by fetching it back).

Creating a request is not executing it. Never call the real API unless the user explicitly asks.

## Core rules

1. **No invention.** Endpoints, fields, enums, headers, and example data come from source, a spec, or the user. Mark anything you cannot determine as `UNKNOWN`.
2. **No fabricated IDs.** Use only IDs returned by Postman MCP tools.
3. **Sequential writes.** Create one request, verify it, then move to the next.
4. **No destructive changes.** Do not delete or overwrite anything without the user's authorization.
5. **No secrets.** Use `{{baseUrl}}` and `{{accessToken}}`. Do not hardcode `localhost` unless the user or source requires it.
6. **Success = a successful detail fetch,** not the request showing up in a collection listing.

## Workflow

### 1. Target
- Read the Postman MCP tool schemas. Note whether each tool expects an `id` or a `uid` (`ownerId-id`).
- Find the collection by its exact name. If there are 0 or several matches, ask.
- Find the folder. Skip any `METHOD + path` that already exists there. Ask before replacing an existing request unless the user asked for an update.

### 2. Derive from source (Spring Boot)
| Source | Maps to |
|---|---|
| Class `@RequestMapping` + method mapping, `context-path` | Full path |
| `@PathVariable` | `:var` + `url.variable` |
| `@RequestParam` (`required`, `defaultValue`), `@ModelAttribute`, `Pageable` | Query params (optional ones disabled) |
| `@RequestHeader`, `consumes`/`produces` | Headers |
| `@RequestBody` DTO + validation, `@JsonProperty` | Raw JSON body that satisfies the constraints |
| `MultipartFile`, `@RequestPart` | `formdata` body |
| Security config, `@PreAuthorize` | Bearer `{{accessToken}}` or `noauth` |

For an OpenAPI spec, use `paths`, `parameters`, `requestBody`, and `security` directly.

### 3. Create and verify (silently)
For each request:
1. Create it with the complete object: method, url (raw/host/path/query/variable), header, auth, body.
2. Fetch it back using the returned ID, then compare method, URL, params, headers, auth, and body.
3. If it fails, do not re-create it. Check the tool response and the collection listing first.

Do not narrate per-request progress. Stop after 2 consecutive failures of the same kind.

### 4. Fallback (entries listed but `Request not found`)
- Check for an `id`/`uid` mismatch first.
- Otherwise, generate a Collection v2.1 JSON (every item has `request.method` and `request.url`). Create it as a **new** collection through MCP if a tool for that is available, or give the user the file to import. Do not touch the original collection.

## Final output (keep it short)

One summary line:

`✅ 12/12 passed in <Collection>/<Folder>` or `⚠️ 10 passed, 1 failed, 1 skipped in <Collection>/<Folder>`

Then list only what needs attention, one line each:
- Failed: `METHOD path` + the exact error
- Skipped: `METHOD path` + the reason
- UNKNOWN: anything the user must fill in

Do not print a full table of passed requests unless the user asks. Never say everything passed unless every fetch succeeded.
