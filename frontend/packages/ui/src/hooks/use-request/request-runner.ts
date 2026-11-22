import type { RequestFn } from "@workspace/ui/hooks/use-request/types"

/** What the runner reports; only the latest call ever reports success or failure */
export interface RequestRunnerEvents<TData> {
  started(): void
  succeeded(data: TData): void
  failed(error: unknown): void
  cancelled(): void
}

export interface RequestRunnerConfig<TArgs extends unknown[], TData> {
  /** Read when sending, so the latest function is used */
  getRequest(): RequestFn<TArgs, TData>
  /** Read on every call, so the latest delay is used */
  getDelayMs(): number
  events: RequestRunnerEvents<TData>
}

export interface RequestRunner<TArgs extends unknown[], TData> {
  /** Debounce, abort the previous request, then send; resolves with the winning result */
  run(...args: TArgs): Promise<TData>
  /**
   * Send the last call's arguments again, right away (no debounce).
   * Undefined when there is nothing to repeat yet.
   */
  restart(): Promise<TData> | undefined
  /** Stop everything pending and report `cancelled` */
  cancel(): void
  /** cancel() and forget the last arguments, so restart() has nothing to repeat */
  reset(): void
  /** Stop everything pending silently (unmount); the runner stays usable */
  dispose(): void
}

interface Waiter<TData> {
  resolve: (data: TData) => void
  reject: (reason: unknown) => void
}

/**
 * Framework-free core of useRequest. Every call bumps a generation number;
 * a request that finishes for an older generation is ignored, and its callers
 * wait for the newer call instead.
 */
export function createRequestRunner<TArgs extends unknown[], TData>(
  config: RequestRunnerConfig<TArgs, TData>
): RequestRunner<TArgs, TData> {
  const { events } = config
  let generation = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  let controller: AbortController | null = null
  let waiters: Waiter<TData>[] = []
  let lastArgs: TArgs | undefined

  function settle(notify: (waiter: Waiter<TData>) => void) {
    const pending = waiters
    waiters = []
    pending.forEach(notify)
  }

  /** Drop the scheduled send and abort the request in flight */
  function stopPending() {
    clearTimeout(timer)
    timer = undefined
    controller?.abort()
    controller = null
  }

  async function send(args: TArgs, callGeneration: number) {
    controller = new AbortController()
    const { signal } = controller
    try {
      const data = await config.getRequest()(signal, ...args)
      if (callGeneration !== generation) return
      events.succeeded(data)
      settle((waiter) => waiter.resolve(data))
    } catch (error) {
      if (callGeneration !== generation) return
      events.failed(error)
      settle((waiter) => waiter.reject(error))
    }
  }

  /** Start a call: supersede the previous one, then send after delayMs */
  function start(args: TArgs, delayMs: number): Promise<TData> {
    const result = new Promise<TData>((resolve, reject) => {
      waiters.push({ resolve, reject })
    })
    const callGeneration = ++generation
    lastArgs = args
    stopPending()
    events.started()

    if (delayMs > 0) {
      timer = setTimeout(() => void send(args, callGeneration), delayMs)
    } else {
      void send(args, callGeneration)
    }
    return result
  }

  function stop(reason: string) {
    generation++
    stopPending()
    settle((waiter) => waiter.reject(createAbortError(reason)))
  }

  function cancel() {
    stop("Request cancelled")
    events.cancelled()
  }

  return {
    run: (...args) => start(args, config.getDelayMs()),
    restart: () => (lastArgs ? start(lastArgs, 0) : undefined),
    cancel,
    reset: () => {
      cancel()
      lastArgs = undefined
    },
    dispose: () => stop("Component unmounted"),
  }
}

const ABORT_ERROR_NAME = "AbortError"

/** The same error type fetch throws when its signal is aborted */
export function createAbortError(message: string): DOMException {
  return new DOMException(message, ABORT_ERROR_NAME)
}

/** True for cancelled requests, which callers should ignore rather than report */
export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === ABORT_ERROR_NAME
}
