# Error Handling

Nemouw uses consistent, machine-readable errors across `/v1`. The OpenAPI contract is the source of truth for response schemas, individual error codes, and endpoint-specific responses. This document describes conventions rather than duplicating the contract.

## Response format

Every error has the same JSON shape: an HTTP `status`, a stable `code`, a human-readable `message` and a `trace_id`. The backend sets `trace_id` on every error and also sends it in the `X-Trace-Id` response header, but the contract marks it nullable, so clients should tolerate its absence.

```json
{
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "trace_id": "4642bbc4-bca0-45f9-b509-e46c2dbfe7ab"
}
```

For `422 VALIDATION_ERROR`, an additional `errors` array identifies the invalid fields:

```json
{
  "status": 422,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "trace_id": "...",
  "errors": [
    { "field": "email", "code": "MUST_BE_EMAIL", "message": "must be a valid email address" }
  ]
}
```

The `errors` array appears only on `422` responses and always has at least one entry. `field` is the `snake_case` name of the request field (dot notation for nested fields). JSON keys use `snake_case`.

The shape is the same whether the error comes from a controller, the security layer (401, 403) or the rate limiter (429).

## Conventions

- **HTTP status** describes the category of failure; **`code`** identifies the specific condition.
- Treat `code` as the stable interface. Do not parse or depend on `message` text.
- Use `422` for field validation, `409` for state conflicts, `401` for authentication failures, and `403` for permission or account restrictions.
- Errors raised by the framework (unknown route, wrong HTTP method, unsupported media type) keep their own status and use the HTTP status name as `code`, for example `405` with `METHOD_NOT_ALLOWED`.
- Unexpected failures return `500 INTERNAL_ERROR` with a generic message. Stack traces, internal details, credentials and tokens are never exposed.
- Use the same error shape for all modules. Domain-specific codes belong in the OpenAPI contract, not in separate competing catalogs.

## Rate limiting

`429 TOO_MANY_REQUESTS` includes a `Retry-After` header in seconds. Clients should respect it rather than immediately retrying. Limits may be global or endpoint-specific; consult the contract for each operation.

## Client handling

- Use the HTTP status for broad handling and `code` for specific actions.
- Map field-level `errors[].code` to localized validation messages and associate them with `errors[].field`.
- Tell the two 401s apart: `UNAUTHENTICATED` means no access cookie was sent (go to login), while `INVALID_TOKEN` means a cookie was sent but is expired or revoked (try the refresh flow once, then go to login). Do not retry indefinitely.
- Show safe, user-friendly messages for unexpected errors and keep the `trace_id` for support.
- Never assume every `403`, `404`, or `409` has the same business meaning.

For authentication flows and token behavior, see [Authentication](../auth/README.md).