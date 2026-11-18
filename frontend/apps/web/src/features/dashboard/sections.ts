import { FileBadgeIcon, LayoutDashboardIcon, UserRoundIcon } from "lucide-react"

import { DASHBOARD_PATH } from "@/features/auth/lib/constants"

/** The dashboard's sections, in menu order. Paths have no locale */
export const DASHBOARD_SECTIONS = {
  overview: { href: DASHBOARD_PATH, icon: LayoutDashboardIcon },
  profile: { href: `${DASHBOARD_PATH}/profile`, icon: UserRoundIcon },
  credentials: { href: `${DASHBOARD_PATH}/credentials`, icon: FileBadgeIcon },
} as const

export type DashboardSection = keyof typeof DASHBOARD_SECTIONS
