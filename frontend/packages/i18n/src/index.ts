// Locales shared by every app. Plain TypeScript only (no next-intl, no React),
// so apps stay free to choose how they translate.
// Must match the backend's LocaleCode (en, fr, ar): the locale is sent with
// sign-up and password-reset requests to pick the email language.

export const locales = ["en", "fr", "ar"] as const

export type Locale = (typeof locales)[number]

export const defaultLocale: Locale = "en"

// Each language written in itself, for language pickers.
export const localeNames: Record<Locale, string> = {
  en: "English",
  fr: "Français",
  ar: "العربية",
}

// Short labels for tight spaces (mobile headers); pair them with localeNames as the accessible name.
export const localeShortNames: Record<Locale, string> = {
  en: "EN",
  fr: "FR",
  ar: "ع",
}

const rtlLocales: ReadonlySet<Locale> = new Set(["ar"])

export function isLocale(value: unknown): value is Locale {
  return (
    typeof value === "string" && (locales as readonly string[]).includes(value)
  )
}

export function localeDirection(locale: Locale): "ltr" | "rtl" {
  return rtlLocales.has(locale) ? "rtl" : "ltr"
}
