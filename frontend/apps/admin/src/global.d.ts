import type { Locale } from "@workspace/i18n"

import type messages from "../messages/en.json"

// Types useTranslations / getTranslations keys and useLocale against en.json
declare module "next-intl" {
  interface AppConfig {
    Locale: Locale
    Messages: typeof messages
  }
}
