"use client"

import { useTranslations } from "next-intl"

import { NavList } from "@/components/nav-list"
import {
  SYNDICATE_SECTIONS,
  type SyndicateSection,
} from "@/features/syndicate/sections"

/** Links to every section of the syndicate area */
export function SyndicateNav({ variant }: { variant: "sidebar" | "menu" }) {
  const t = useTranslations("Syndicate.nav")
  const items = Object.entries(SYNDICATE_SECTIONS).map(([key, section]) => ({
    ...section,
    label: t(key as SyndicateSection),
  }))
  return <NavList items={items} variant={variant} />
}
