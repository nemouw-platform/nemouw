"use client"

import { useTranslations } from "next-intl"

import { NavList } from "@/components/nav-list"
import {
  DASHBOARD_SECTIONS,
  type DashboardSection,
} from "@/features/dashboard/sections"

/** Links to every dashboard section, for the sidebar and the drawer */
export function DashboardNav({ variant }: { variant: "sidebar" | "menu" }) {
  const t = useTranslations("Dashboard.nav")
  const items = Object.entries(DASHBOARD_SECTIONS).map(([key, section]) => ({
    ...section,
    label: t(key as DashboardSection),
  }))

  return <NavList items={items} variant={variant} />
}
