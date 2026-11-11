"use client"

import { cn } from "@workspace/ui/lib/utils"
import { useTranslations } from "next-intl"

import { Link, usePathname } from "@/i18n/navigation"

/** The site's main sections, in menu order. Paths have no locale */
const NAV_ITEMS = [
  { label: "findSpecialist", href: "/specialists" },
  { label: "howItWorks", href: "/#how-it-works" },
  { label: "forSpecialists", href: "/#for-specialists" },
] as const

const focusRing =
  "outline-none focus-visible:ring-3 focus-visible:ring-ring/50"

const variants = {
  /** In a row, inside the desktop bar */
  bar: {
    list: "flex items-center",
    link: cn(
      focusRing,
      "rounded-md px-2.5 py-2 text-sm font-medium whitespace-nowrap text-muted-foreground transition-colors hover:text-foreground aria-[current=page]:text-foreground"
    ),
  },
  /** Stacked, inside the mobile drawer */
  menu: {
    list: "flex flex-col gap-1",
    link: cn(
      focusRing,
      "block rounded-md px-3 py-2.5 text-base font-medium transition-colors hover:bg-muted aria-[current=page]:bg-muted"
    ),
  },
}

interface NavLinksProps {
  variant: keyof typeof variants
  className?: string
}

/** The main links, marking the current page; one list for both layouts */
export function NavLinks({ variant, className }: NavLinksProps) {
  const t = useTranslations("Navbar")
  const pathname = usePathname()
  const styles = variants[variant]

  return (
    <ul className={cn(styles.list, className)}>
      {NAV_ITEMS.map(({ label, href }) => (
        <li key={href}>
          <Link
            href={href}
            aria-current={pathname === href ? "page" : undefined}
            className={styles.link}
          >
            {t(label)}
          </Link>
        </li>
      ))}
    </ul>
  )
}
