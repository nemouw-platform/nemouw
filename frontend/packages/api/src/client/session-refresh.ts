import type { HttpTransport } from "./http-transport"
import type { ApiError } from "../errors"

const AUTH_PATH_PREFIX = "/auth/"
const REFRESH_PATH = "/auth/refresh"

/** Rotates the session cookies; concurrent callers share one request */
export interface SessionRefresher {
  refresh(): Promise<boolean>
}

export function createSessionRefresher(
  transport: HttpTransport
): SessionRefresher {
  let inFlight: Promise<boolean> | null = null

  function refresh(): Promise<boolean> {
    inFlight ??= transport
      .send("POST", REFRESH_PATH)
      .then((response) => response.ok)
      .catch(() => false)
      .finally(() => {
        inFlight = null
      })
    return inFlight
  }

  return { refresh }
}

/**
 * Whether a failed call is worth one session refresh and a retry.
 *
 * Any 401 outside /auth/ qualifies: the access cookie is either missing
 * (UNAUTHENTICATED: the browser drops it when its 15-minute Max-Age ends) or
 * expired / revoked (INVALID_TOKEN), and the 90-day refresh cookie may still be
 * valid in both cases. Auth endpoints answer 401 for their own reasons (wrong
 * password, missing setup cookie), which a refresh never fixes.
 */
export function shouldRefreshAndRetry(error: ApiError, path: string): boolean {
  return error.status === 401 && !path.startsWith(AUTH_PATH_PREFIX)
}
