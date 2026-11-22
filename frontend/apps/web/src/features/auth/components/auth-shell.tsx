import { Logo } from "@workspace/ui/components/logo"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { cacheLife } from "next/cache"
import { useLocale, useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"

import { LocaleSwitcher } from "@/components/locale-switcher"
import { BrandPanel } from "@/features/auth/components/brand-panel"
import { Link } from "@/i18n/navigation"

/**
 * Layout shared by every auth screen: the form column (logo, language and
 * theme controls, the step, a footer) beside the brand panel. The panel is
 * hidden below the lg breakpoint, so phones get the form alone.
 */
export function AuthShell({ children }: { children: React.ReactNode }) {
  const t = useTranslations()
  const locale = useLocale()

  return (
    <div className="min-h-svh bg-background lg:grid lg:grid-cols-2 lg:gap-4 lg:p-4">
      {/* Pinned on desktop, so the form stays put while the taller panel scrolls */}
      <div className="flex min-h-svh flex-col px-4 py-5 sm:px-10 lg:sticky lg:top-4 lg:h-[calc(100svh-2rem)] lg:min-h-0 lg:self-start lg:px-12">
        <header className="flex items-center justify-between gap-3">
          <Link href="/" aria-label={t("Metadata.title")}>
            <Logo
              locale={locale === "ar" ? "ar" : "en"}
              className="h-12 sm:h-14"
            />
          </Link>
          <div className="flex items-center gap-1">
            <LocaleSwitcher compact />
            <ThemeToggle label={t("Common.toggleTheme")} />
          </div>
        </header>

        <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10">
          {children}
        </main>

        <footer className="text-xs text-muted-foreground">
          <Copyright />
        </footer>
      </div>

      <BrandPanel className="hidden lg:flex lg:min-h-[calc(100svh-2rem)]" />
    </div>
  )
}

async function Copyright() {
  const t = await getTranslations("Auth")
  return t("copyright", { year: await currentYear() })
}

// The static shell is prerendered, where reading the clock is only allowed in
// cached code; refreshing daily keeps the year right after New Year.
async function currentYear() {
  "use cache"
  cacheLife("days")
  return new Date().getFullYear()
}
