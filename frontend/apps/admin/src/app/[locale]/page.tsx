import { Logo } from "@workspace/ui/components/logo"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { useLocale, useTranslations } from "next-intl"

import { LocaleSwitcher } from "@/components/locale-switcher"

export default function Page() {
  const t = useTranslations()
  const locale = useLocale()

  return (
    <div className="flex min-h-svh p-6">
      <div className="m-auto flex max-w-fit min-w-0 flex-col gap-4 text-sm leading-loose">
        <div>
          <h1 className="mb-2">
            <Logo locale={locale === "ar" ? "ar" : "en"} className="h-16" />
          </h1>
          <p>{t("Dashboard.tagline")}</p>
          <div className="mt-2 flex items-center gap-2">
            <ThemeToggle label={t("Common.toggleTheme")} />
            <LocaleSwitcher />
          </div>
        </div>
      </div>
    </div>
  )
}
