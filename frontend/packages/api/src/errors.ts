/**
 * Error shape returned by every Nemouw backend endpoint under /v1.
 * See docs/backend/errors/README.md. Branch on `code`, never on `message`.
 */
export interface FieldError {
  /** snake_case request field, dot notation for nested fields */
  field: string
  code: string
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

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId: string | null
  readonly fieldErrors: FieldError[]

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = "ApiError"
    this.status = body.status
    this.code = body.code
    this.traceId = body.trace_id ?? null
    this.fieldErrors = body.errors ?? []
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}