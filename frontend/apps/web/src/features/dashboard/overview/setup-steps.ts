import type {
  CredentialResponse,
  SpecialistAvailability,
  SpecialistLocation,
  SpecialistResponse,
} from "@workspace/api"

import { DASHBOARD_SECTIONS } from "@/features/dashboard/sections"
import { hasBio } from "@/features/specialists/about/about-schema"
import type { ContentByLocale } from "@/features/specialists/about/use-profile-content"

export interface SetupStep {
  key: "credentials" | "location" | "photo" | "details" | "about" | "conditions"
  done: boolean
  /** Where to complete it, without locale */
  href: string
}

interface SetupData {
  specialist: SpecialistResponse
  content: ContentByLocale
  credentials: CredentialResponse[]
  locations: SpecialistLocation[]
  hours: SpecialistAvailability[]
  /** Ids of the conditions treated */
  conditions: string[]
}

/**
 * What a specialist still has to do for a complete public profile, in the
 * order we suggest: credentials first, as verification depends on them. Each
 * new dashboard section adds its step here.
 */
export function setupSteps({
  specialist,
  content,
  credentials,
  locations,
  hours,
  conditions,
}: SetupData): SetupStep[] {
  const profile = DASHBOARD_SECTIONS.profile.href
  return [
    {
      key: "credentials",
      done: credentials.length > 0,
      href: DASHBOARD_SECTIONS.credentials.href,
    },
    {
      key: "location",
      // Patients need both: where to go, and when
      done: locations.length > 0 && hours.length > 0,
      href: DASHBOARD_SECTIONS.locations.href,
    },
    {
      key: "photo",
      done: Boolean(specialist.profile_picture_url),
      href: `${profile}#photos`,
    },
    {
      key: "details",
      // What patients need to choose and reach a specialist
      done:
        specialist.years_of_experience != null &&
        specialist.primary_language != null &&
        specialist.whatsapp_number != null,
      href: profile,
    },
    {
      key: "about",
      done: Object.values(content).some(hasBio),
      href: `${profile}#about`,
    },
    {
      key: "conditions",
      // What patients search by
      done: conditions.length > 0,
      href: DASHBOARD_SECTIONS.conditions.href,
    },
  ]
}
