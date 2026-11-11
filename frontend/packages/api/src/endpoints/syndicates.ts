import type { ApiClient } from "../client"
import type { SyndicateSummary } from "../types"
import type { CallOptions } from "./types"

/** Professional syndicates (public directory) */
export function createSyndicatesApi(client: ApiClient) {
  return {
    /** Public. The list specialists pick from during onboarding. */
    list: (options?: CallOptions) =>
      client.get<SyndicateSummary[]>("/syndicates", options),
  }
}

export type SyndicatesApi = ReturnType<typeof createSyndicatesApi>
