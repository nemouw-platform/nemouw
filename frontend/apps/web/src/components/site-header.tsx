import { Logo } from "@workspace/ui/components/logo"
import { useLocale, useTranslations } from "next-intl"

import { Link } from "@/i18n/navigation"

/**
 * The sticky top bar every page shares: the logo (a link home) at the start,
 * then whatever the page puts in it (links, settings, account actions).
 */
export function SiteHeader({ children }: { children: React.ReactNode }) {
  const t = useTranslations("Metadata")
  const locale = useLocale()

  return (
    <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur supports-backdrop-filter:bg-background/60">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6 xl:gap-8">
        <Link href="/" aria-label={t("title")} className="shrink-0">
          <Logo locale={locale === "ar" ? "ar" : "en"} className="h-12" />
        </Link>
        {children}
      </div>
    </header>
  )
}
