import type { ApiClient } from "../client"
import type {
  SpecialistAvailability,
  SpecialistAvailabilityWriteRequest,
  SpecialistLocation,
  SpecialistLocationWriteRequest,
} from "../types"
import type { CallOptions } from "./types"

const LOCATIONS = "/specialists/me/locations"
const HOURS = "/specialists/me/availabilities"

/**
 * Where and when the signed-in specialist works. A specialist with any
 * location has exactly one primary: the first one added, or the one last
 * marked primary. Hours are windows on a weekday, optionally at a location.
 */
export function createLocationsApi(client: ApiClient) {
  return {
    list: (options?: CallOptions) =>
      client.get<SpecialistLocation[]>(LOCATIONS, options),

    /**
     * 201. The first location is primary whatever is sent; a later one only
     * with is_primary: true (it then replaces the old primary). 422 on the
     * fields (an unknown country is INVALID_FORMAT on `country_id`).
     */
    add: (body: SpecialistLocationWriteRequest, options?: CallOptions) =>
      client.post<SpecialistLocation>(LOCATIONS, body, options),

    /**
     * Full replacement (leave is_primary out to keep it). 409
     * PRIMARY_LOCATION_REQUIRED when demoting the primary: make another one
     * primary instead.
     */
    update: (
      id: string,
      body: SpecialistLocationWriteRequest,
      options?: CallOptions
    ) => client.put<SpecialistLocation>(`${LOCATIONS}/${id}`, body, options),

    /**
     * 204; removes its hours too. 409 PRIMARY_LOCATION_REQUIRED for the
     * primary while others exist.
     */
    remove: (id: string, options?: CallOptions) =>
      client.delete<void>(`${LOCATIONS}/${id}`, options),

    /** Every window, Monday first, then by start time */
    listHours: (options?: CallOptions) =>
      client.get<SpecialistAvailability[]>(HOURS, options),

    /** 201. 422: end_time must be after start_time (INVALID_FORMAT on `end_time`) */
    addHours: (
      body: SpecialistAvailabilityWriteRequest,
      options?: CallOptions
    ) => client.post<SpecialistAvailability>(HOURS, body, options),

    updateHours: (
      id: string,
      body: SpecialistAvailabilityWriteRequest,
      options?: CallOptions
    ) => client.put<SpecialistAvailability>(`${HOURS}/${id}`, body, options),

    removeHours: (id: string, options?: CallOptions) =>
      client.delete<void>(`${HOURS}/${id}`, options),
  }
}

export type LocationsApi = ReturnType<typeof createLocationsApi>
