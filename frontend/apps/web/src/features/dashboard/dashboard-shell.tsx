import { SheetFooter } from "@workspace/ui/components/sheet"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { useTranslations } from "next-intl"
import { Suspense } from "react"

import { LocaleSwitcher } from "@/components/locale-switcher"
import { MobileMenu } from "@/components/navbar"
import { SiteHeader } from "@/components/site-header"
import { SignOutButton } from "@/features/auth/components/sign-out-button"
import { DashboardNav } from "@/features/dashboard/dashboard-nav"
import { SpecialistProvider } from "@/features/specialists/me/specialist-provider"
import { VisitorCountryProvider } from "@/lib/visitor-country/visitor-country-provider"

/**
 * Frame of every dashboard page: the top bar, the section links (a sidebar
 * from the lg breakpoint, the burger drawer below it) and the page, which
 * renders once the specialist's profile has loaded.
 */
export function DashboardShell({ children }: { children: React.ReactNode }) {
  const t = useTranslations()

  return (
    <div className="min-h-svh bg-background">
      <SiteHeader>
        <div className="ms-auto hidden items-center gap-1 lg:flex">
          <LocaleSwitcher compact />
          <ThemeToggle label={t("Common.toggleTheme")} />
          <SignOutButton className="ms-2" />
        </div>

        <MobileMenu className="ms-auto lg:hidden">
          <nav aria-label={t("Dashboard.nav.label")} className="px-4">
            <DashboardNav variant="menu" />
          </nav>
          <SheetFooter className="gap-4 border-t">
            <SignOutButton size="lg" className="justify-start" />
            <div className="flex items-center justify-between">
              <LocaleSwitcher />
              <ThemeToggle label={t("Common.toggleTheme")} />
            </div>
          </SheetFooter>
        </MobileMenu>
      </SiteHeader>

      <div className="mx-auto flex max-w-6xl gap-10 px-4 py-8 sm:px-6 lg:py-10">
        <nav
          aria-label={t("Dashboard.nav.label")}
          className="sticky top-26 hidden w-56 shrink-0 self-start lg:block"
        >
          <DashboardNav variant="sidebar" />
        </nav>

        <main className="min-w-0 flex-1">
          {/* The visitor's country is read from the request (for phone
              fields); the shell around it still prerenders */}
          <Suspense fallback={<PageSkeleton />}>
            <VisitorCountryProvider>
              <SpecialistProvider fallback={<PageSkeleton />}>
                {children}
              </SpecialistProvider>
            </VisitorCountryProvider>
          </Suspense>
        </main>
      </div>
    </div>
  )
}

/** Stands in for a page while the profile loads */
function PageSkeleton() {
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
