import { ApiError, CLIENT_ERROR_CODES, type ApiErrorBody } from "../errors"

const MAX_MESSAGE_LENGTH = 200

/** JSON body of a successful response; undefined for empty ones (202 / 204) */
export async function readBody(response: Response): Promise<unknown> {
  const text = await response.text()
  return text ? JSON.parse(text) : undefined
}

/** Turn a failed response into an ApiError, whatever its body looks like */
export async function toApiError(response: Response): Promise<ApiError> {
  const retryAfter = parseRetryAfter(response.headers.get("Retry-After"))
  const text = await response.text().catch(() => "")
  const body = parseErrorBody(text)

  if (body) {
    return new ApiError({ ...body, status: response.status }, retryAfter)
  }
  return new ApiError(
    {
      status: response.status,
      code: CLIENT_ERROR_CODES.unexpectedResponse,
      message:
        text.slice(0, MAX_MESSAGE_LENGTH) ||
        response.statusText ||
        "Unexpected response",
    },
    retryAfter
  )
}

/**
 * Seconds to wait from a Retry-After header, which is either a number of
 * seconds or an HTTP date. Null when absent or unreadable.
 */
export function parseRetryAfter(value: string | null): number | null {
  if (!value) return null

  const seconds = Number(value)
  if (Number.isFinite(seconds)) return Math.max(0, Math.ceil(seconds))

  const date = Date.parse(value)
  if (Number.isNaN(date)) return null
  return Math.max(0, Math.ceil((date - Date.now()) / 1000))
}

/** The backend's standard error body, or null for anything else (HTML, plain text) */
function parseErrorBody(text: string): ApiErrorBody | null {
  try {
    const body = JSON.parse(text) as Partial<ApiErrorBody>
    const isStandard =
      typeof body.code === "string" && typeof body.message === "string"
    return isStandard ? (body as ApiErrorBody) : null
  } catch {
    return null
  }
}
