import { SheetFooter } from "@workspace/ui/components/sheet"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { useTranslations } from "next-intl"

import { LocaleSwitcher } from "@/components/locale-switcher"
import { MobileMenu } from "@/components/navbar"
import { SiteHeader } from "@/components/site-header"
import { SignOutButton } from "@/features/auth/components/sign-out-button"

interface DashboardShellProps {
  /** The area's section links, drawn as a sidebar or in the drawer */
  nav: React.ComponentType<{ variant: "sidebar" | "menu" }>
  /** The navigation's accessible name, e.g. "Dashboard" */
  navLabel: string
  /** The page, inside whatever loads the area's data */
  children: React.ReactNode
}

/**
 * Frame of a signed-in area (the specialist dashboard, the syndicate area):
 * the top bar with log out, the section links (a sidebar from the lg
 * breakpoint, the burger drawer below it) and the page.
 */
export function DashboardShell({
  nav: Nav,
  navLabel,
  children,
}: DashboardShellProps) {
  const t = useTranslations("Common")

  return (
    <div className="min-h-svh bg-background">
      <SiteHeader>
        <div className="ms-auto hidden items-center gap-1 lg:flex">
          <LocaleSwitcher compact />
          <ThemeToggle label={t("toggleTheme")} />
          <SignOutButton className="ms-2" />
        </div>

        <MobileMenu className="ms-auto lg:hidden">
          <nav aria-label={navLabel} className="px-4">
            <Nav variant="menu" />
          </nav>
          <SheetFooter className="gap-4 border-t">
            <SignOutButton size="lg" className="justify-start" />
            <div className="flex items-center justify-between">
              <LocaleSwitcher />
              <ThemeToggle label={t("toggleTheme")} />
            </div>
          </SheetFooter>
        </MobileMenu>
      </SiteHeader>

      <div className="mx-auto flex max-w-6xl gap-10 px-4 py-8 sm:px-6 lg:py-10">
        <nav
          aria-label={navLabel}
          className="sticky top-26 hidden w-56 shrink-0 self-start lg:block"
        >
          <Nav variant="sidebar" />
        </nav>

        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  )
}

/** Stands in for a page while its area's data loads */
export function PageSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy>
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-4 w-80 max-w-full" />
      </div>
      <Skeleton className="h-40 w-full rounded-xl" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  )
}
