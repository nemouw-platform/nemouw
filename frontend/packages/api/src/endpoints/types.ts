import type { RequestOptions } from "../client"

/** Per-call options every endpoint function accepts, e.g. { signal } from useRequest */
export type CallOptions = Pick<RequestOptions, "signal">
