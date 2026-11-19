import type { ApiClient } from "../client"
import type { Condition } from "../types"
import type { CallOptions } from "./types"

/** The conditions specialists can treat (public, one list for every specialty) */
export function createConditionsApi(client: ApiClient) {
  return {
    /** Only the ones in use, unless `activeOnly` is false */
    list: (activeOnly = true, options?: CallOptions) =>
      client.get<Condition[]>(
        activeOnly ? "/conditions?is_active=true" : "/conditions",
        options
      ),
  }
}

export type ConditionsApi = ReturnType<typeof createConditionsApi>
