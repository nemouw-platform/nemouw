import type { components } from "./generated/openapi"

/**
 * Error shape returned by every Nemouw backend endpoint under /v1.
 * See docs/backend/errors/README.md. Branch on `code`, never on `message`.
 */
export type FieldErrorCode = components["schemas"]["FieldError"]["code"]

export interface FieldError {
  /** snake_case request field, dot notation for nested fields */
  field: string
  code: FieldErrorCode
  message: string
}

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  /** Nullable in the contract: tolerate its absence. */
  trace_id?: string | null
  /** Present only on 422 VALIDATION_ERROR, with at least one entry. */
  errors?: FieldError[]
}

/**
 * Codes the client itself produces when there is no backend error body:
 * - NETWORK_ERROR: the request never got a response (offline, DNS, CORS rejection)
 * - UNEXPECTED_RESPONSE: a response without the standard error body (e.g. a proxy's 502 page)
 */
export const CLIENT_ERROR_CODES = {
  network: "NETWORK_ERROR",
  unexpectedResponse: "UNEXPECTED_RESPONSE",
} as const

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId: string | null
  readonly fieldErrors: FieldError[]
  /** Seconds to wait before retrying, from the Retry-After header of a 429 */
  readonly retryAfter: number | null

  constructor(body: ApiErrorBody, retryAfter: number | null = null) {
    super(body.message)
    this.name = "ApiError"
    this.status = body.status
    this.code = body.code
    this.traceId = body.trace_id ?? null
    this.fieldErrors = body.errors ?? []
    this.retryAfter = retryAfter
  }

  /** The first error for a request field (snake_case), if any */
  fieldError(field: string): FieldError | undefined {
    return this.fieldErrors.find((error) => error.field === field)
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}
