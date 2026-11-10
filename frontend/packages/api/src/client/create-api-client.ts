import { createHttpTransport, type HttpTransport } from "./http-transport"
import { readBody, toApiError } from "./responses"
import {
  createSessionRefresher,
  type SessionRefresher,
  shouldRefreshAndRetry,
} from "./session-refresh"
import type {
  ApiClient,
  ApiClientOptions,
  HttpMethod,
  RequestOptions,
} from "./types"

/**
 * Client for the Spring Boot API. Cookies carry the session (HttpOnly), so
 * tokens never touch JavaScript. A 401 is retried once after a session refresh.
 */
export function createApiClient(options: ApiClientOptions): ApiClient {
  const transport = createHttpTransport(options)
  const refresher = createSessionRefresher(transport)
  const autoRefresh = options.autoRefresh ?? true

  async function request<T>(
    method: HttpMethod,
    path: string,
    init: RequestOptions = {}
  ): Promise<T> {
    const response = await transport.send(method, path, init)
    if (response.ok) return (await readBody(response)) as T

    const error = await toApiError(response)
    if (autoRefresh && shouldRefreshAndRetry(error, path)) {
      return retryAfterRefresh<T>({ transport, refresher, options }, error, {
        method,
        path,
        init,
      })
    }
    throw error
  }

  return {
    request,
    get: (path, init) => request("GET", path, init),
    post: (path, body, init) => request("POST", path, { ...init, body }),
    put: (path, body, init) => request("PUT", path, { ...init, body }),
    patch: (path, body, init) => request("PATCH", path, { ...init, body }),
    delete: (path, init) => request("DELETE", path, init),
    refreshSession: refresher.refresh,
  }
}

interface RetryContext {
  transport: HttpTransport
  refresher: SessionRefresher
  options: Pick<ApiClientOptions, "onSessionExpired">
}

interface OriginalCall {
  method: HttpMethod
  path: string
  init: RequestOptions
}

/** Refresh once, replay the call, and report the session as over if that fails */
async function retryAfterRefresh<T>(
  { transport, refresher, options }: RetryContext,
  originalError: Error,
  { method, path, init }: OriginalCall
): Promise<T> {
  if (!(await refresher.refresh())) {
    options.onSessionExpired?.()
    throw originalError
  }

  const retry = await transport.send(method, path, init)
  if (retry.ok) return (await readBody(retry)) as T

  const retryError = await toApiError(retry)
  if (retryError.status === 401) options.onSessionExpired?.()
  throw retryError
}
