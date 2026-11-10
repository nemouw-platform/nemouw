import type { ApiClient } from "../client"
import type { SpecialistResponse } from "../types"
import type { CallOptions } from "./types"

/** The signed-in specialist's own profile (`/specialists/me`) */
export function createSpecialistsApi(client: ApiClient) {
  return {
    /** 200; 404 SPECIALIST_NOT_FOUND until onboarding is complete. */
    getMe: (options?: CallOptions) =>
      client.get<SpecialistResponse>("/specialists/me", options),
  }
}

export type SpecialistsApi = ReturnType<typeof createSpecialistsApi>
