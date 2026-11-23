import { useTranslations } from "next-intl"
import { Suspense } from "react"

import { DashboardNav } from "@/features/dashboard/dashboard-nav"
import {
  DashboardShell,
  PageSkeleton,
} from "@/features/dashboard/dashboard-shell"
import { SpecialistProvider } from "@/features/specialists/me/specialist-provider"
import { VisitorCountryProvider } from "@/lib/visitor-country/visitor-country-provider"

/** Every dashboard page; the proxy lets only signed-in specialists in */
export default function DashboardLayout({
  children,
}: LayoutProps<"/[locale]/dashboard">) {
  const t = useTranslations("Dashboard.nav")

  return (
    <DashboardShell nav={DashboardNav} navLabel={t("label")}>
      {/* The visitor's country is read from the request (for phone
          fields); the shell around it still prerenders */}
      <Suspense fallback={<PageSkeleton />}>
        <VisitorCountryProvider>
          <SpecialistProvider fallback={<PageSkeleton />}>
            {children}
          </SpecialistProvider>
        </VisitorCountryProvider>
      </Suspense>
    </DashboardShell>
  )
}
