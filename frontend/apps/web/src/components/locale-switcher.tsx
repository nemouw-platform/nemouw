"use client"

import { localeNames, localeShortNames, locales } from "@workspace/i18n"
import { buttonVariants } from "@workspace/ui/components/button"
import { cn } from "@workspace/ui/lib/utils"
import { useLocale, useTranslations } from "next-intl"

import { getPathname, usePathname } from "@/i18n/navigation"

interface LocaleSwitcherProps {
  /** Short labels (EN / FR / ع) for tight spaces; the full name stays the accessible name */
  compact?: boolean
}

// Plain <a>, not <Link>: the locale is the root layout's param, so switching
// it replaces the whole document (lang, dir, fonts). A full page load does that
// cleanly; a client-side navigation re-renders the root layout in the browser,
// which makes React warn about next-themes' inline <script>.
export function LocaleSwitcher({ compact = false }: LocaleSwitcherProps) {
  const current = useLocale()
  const pathname = usePathname()
  const t = useTranslations("Common")

  return (
    <nav aria-label={t("language")} className="flex gap-1">
      {locales.map((locale) => (
        <a
          key={locale}
          href={getPathname({ href: pathname, locale })}
          lang={locale}
          hrefLang={locale}
          aria-label={compact ? localeNames[locale] : undefined}
          aria-current={locale === current ? "page" : undefined}
          className={cn(
            buttonVariants({
              variant: locale === current ? "secondary" : "ghost",
              size: "sm",
            })
          )}
        >
          {compact ? localeShortNames[locale] : localeNames[locale]}
        </a>
      ))}
    </nav>
  )
}
