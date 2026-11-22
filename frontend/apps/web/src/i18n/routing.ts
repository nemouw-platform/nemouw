import { defaultLocale, locales } from "@workspace/i18n"
import { defineRouting } from "next-intl/routing"

// Every URL carries its locale (/en, /fr, /ar). The proxy redirects bare paths
// using the NEXT_LOCALE cookie, then Accept-Language, then the default.
export const routing = defineRouting({
  locales,
  defaultLocale,
  localePrefix: "always",
})
