export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export interface ApiClientOptions {
  /** Backend origin without the /v1 prefix, e.g. "http://localhost:8080" */
  baseUrl: string
  /**
   * Refresh the session once and retry when a call returns 401.
   * Browser only: on a server the rotated cookies would never reach the user.
   * Default: true.
   */
  autoRefresh?: boolean
  /** Extra headers for every request, e.g. a forwarded Cookie header on the server */
  headers?: () => HeadersInit | Promise<HeadersInit>
  /** Called when a 401 could not be fixed by refreshing: the session is over */
  onSessionExpired?: () => void
  /** Custom fetch (tests, instrumentation). Default: globalThis.fetch */
  fetch?: typeof fetch
}

export interface RequestOptions {
  /** JSON-serialized unless it is FormData */
  body?: unknown
  headers?: HeadersInit
  signal?: AbortSignal
}

export interface ApiClient {
  request<T>(
    method: HttpMethod,
    path: string,
    options?: RequestOptions
  ): Promise<T>
  get<T>(path: string, options?: Omit<RequestOptions, "body">): Promise<T>
  post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T>
  put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T>
  patch<T>(path: string, body?: unknown, options?: RequestOptions): Promise<T>
  delete<T>(path: string, options?: RequestOptions): Promise<T>
  /** Rotate the session cookies. Concurrent callers share one request. */
  refreshSession(): Promise<boolean>
}
