import { Building2Icon, ClipboardCheckIcon, UsersIcon } from "lucide-react"

import { SYNDICATE_PATH } from "@/features/auth/lib/constants"

/** The syndicate area's sections, in menu order. Paths have no locale */
export const SYNDICATE_SECTIONS = {
  review: { href: SYNDICATE_PATH, icon: ClipboardCheckIcon },
  staff: { href: `${SYNDICATE_PATH}/staff`, icon: UsersIcon },
  settings: { href: `${SYNDICATE_PATH}/settings`, icon: Building2Icon },
} as const

export type SyndicateSection = keyof typeof SYNDICATE_SECTIONS

/**
 * A specialist's review page. The id rides in the query: a dynamic segment
 * would keep the area's shell (its nav reads the path) from prerendering
 */
export const specialistReviewPath = (specialistId: string) =>
  `${SYNDICATE_PATH}/specialist?id=${encodeURIComponent(specialistId)}`
