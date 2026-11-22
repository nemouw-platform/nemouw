"use client"

import { localeNames, localeShortNames, locales } from "@workspace/i18n"
import { LanguageMenu } from "@workspace/ui/components/language-menu"
import { useLocale, useTranslations } from "next-intl"

import { getPathname, usePathname } from "@/i18n/navigation"

interface LocaleSwitcherProps {
  /** Show the short code (EN / FR / ع) next to the globe, for tight headers */
  compact?: boolean
}

// The options are plain links, not <Link>: the locale is the root layout's
// param, so switching it replaces the whole document (lang, dir, fonts). A full
// page load does that cleanly; a client-side navigation re-renders the root
// layout in the browser, which makes React warn about next-themes' <script>.
export function LocaleSwitcher({ compact = false }: LocaleSwitcherProps) {
  const current = useLocale()
  const pathname = usePathname()
  const t = useTranslations("Common")

  return (
    <LanguageMenu
      label={t("language")}
      value={current}
      triggerText={compact ? localeShortNames[current] : undefined}
      options={locales.map((locale) => ({
        value: locale,
        label: localeNames[locale],
        href: getPathname({ href: pathname, locale }),
      }))}
    />
  )
}
