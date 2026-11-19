import type { ApiClient } from "../client"
import type { Country } from "../types"
import type { CallOptions } from "./types"

/** Countries (public). Names are stored in English: name them by `iso_code`. */
export function createCountriesApi(client: ApiClient) {
  return {
    /** Only the countries in service, unless `activeOnly` is false */
    list: (activeOnly = true, options?: CallOptions) =>
      client.get<Country[]>(
        activeOnly ? "/countries?is_active=true" : "/countries",
        options
      ),
  }
}

export type CountriesApi = ReturnType<typeof createCountriesApi>
