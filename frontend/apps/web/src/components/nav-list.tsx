"use client"

import { cn } from "@workspace/ui/lib/utils"
import type { LucideIcon } from "lucide-react"

import { Link, usePathname } from "@/i18n/navigation"

export interface NavItem {
  /** Path without locale, e.g. "/dashboard/profile" or "/#how-it-works" */
  href: string
  label: string
  icon?: LucideIcon
}

const focusRing = "outline-none focus-visible:ring-3 focus-visible:ring-ring/50"

const variants = {
  /** In a row, inside a desktop bar */
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
      "flex items-center gap-3 rounded-md px-3 py-2.5 text-base font-medium transition-colors hover:bg-muted aria-[current=page]:bg-muted"
    ),
  },
  /** Stacked, in a side column next to the page */
  sidebar: {
    list: "flex flex-col gap-1",
    link: cn(
      focusRing,
      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-card hover:text-foreground aria-[current=page]:bg-card aria-[current=page]:text-foreground aria-[current=page]:shadow-xs"
    ),
  },
}

interface NavListProps {
  items: readonly NavItem[]
  variant: keyof typeof variants
  className?: string
}

/** A list of links that marks the current page; one list for every layout */
export function NavList({ items, variant, className }: NavListProps) {
  const pathname = usePathname()
  const styles = variants[variant]

  return (
    <ul className={cn(styles.list, className)}>
      {items.map(({ label, href, icon: Icon }) => (
        <li key={href}>
          <Link
            href={href}
            aria-current={pathname === href ? "page" : undefined}
            className={styles.link}
          >
            {Icon && <Icon aria-hidden className="size-4 shrink-0" />}
            {label}
          </Link>
        </li>
      ))}
    </ul>
  )
}
