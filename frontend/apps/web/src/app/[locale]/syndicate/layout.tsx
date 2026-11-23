import { useTranslations } from "next-intl"
import { Suspense } from "react"

import {
  DashboardShell,
  PageSkeleton,
} from "@/features/dashboard/dashboard-shell"
import { SyndicateArea } from "@/features/syndicate/me/syndicate-area"
import { SyndicateNav } from "@/features/syndicate/syndicate-nav"
import { VisitorCountryProvider } from "@/lib/visitor-country/visitor-country-provider"

/** Every syndicate page; the proxy lets only signed-in syndicate staff in */
export default function SyndicateLayout({
  children,
}: LayoutProps<"/[locale]/syndicate">) {
  const t = useTranslations("Syndicate.nav")

  return (
    <DashboardShell nav={SyndicateNav} navLabel={t("label")}>
      {/* The session and the visitor's country (for phone fields) are read
          from the request; the shell around them still prerenders */}
      <Suspense fallback={<PageSkeleton />}>
        <VisitorCountryProvider>
          <SyndicateArea fallback={<PageSkeleton />}>{children}</SyndicateArea>
        </VisitorCountryProvider>
      </Suspense>
    </DashboardShell>
  )
}
