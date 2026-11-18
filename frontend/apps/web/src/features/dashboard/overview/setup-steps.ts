import type { SpecialistResponse } from "@workspace/api"

import { DASHBOARD_SECTIONS } from "@/features/dashboard/sections"
import { hasBio } from "@/features/specialists/about/about-schema"
import type { ContentByLocale } from "@/features/specialists/about/use-profile-content"

export interface SetupStep {
  key: "details" | "about"
  done: boolean
  /** Where to complete it, without locale */
  href: string
}

/**
 * What a specialist still has to fill in for a complete public profile, in
 * the order we suggest. Each new dashboard section adds its step here.
 */
export function setupSteps(
  specialist: SpecialistResponse,
  content: ContentByLocale
): SetupStep[] {
  const profile = DASHBOARD_SECTIONS.profile.href
  return [
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
  ]
}
