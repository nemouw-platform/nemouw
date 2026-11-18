"use client"

import { useTranslations } from "next-intl"

import { NavList } from "@/components/nav-list"

/** The site's main sections, in menu order. Paths have no locale */
const NAV_ITEMS = [
  { label: "findSpecialist", href: "/specialists" },
  { label: "howItWorks", href: "/#how-it-works" },
  { label: "forSpecialists", href: "/#for-specialists" },
] as const

interface NavLinksProps {
  variant: "bar" | "menu"
  className?: string
}

/** The public site's main links, for the bar and the drawer */
export function NavLinks({ variant, className }: NavLinksProps) {
  const t = useTranslations("Navbar")
  const items = NAV_ITEMS.map(({ label, href }) => ({ href, label: t(label) }))

  return <NavList items={items} variant={variant} className={className} />
}
