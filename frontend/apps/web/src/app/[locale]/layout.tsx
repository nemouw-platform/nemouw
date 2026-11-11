import type { Metadata } from "next"
import { NextIntlClientProvider } from "next-intl"
import { getLocale, getTranslations } from "next-intl/server"

import "@workspace/ui/globals.css"
import { localeDirection } from "@workspace/i18n"
import { cn } from "@workspace/ui/lib/utils"

import { ThemeProvider } from "@/components/theme-provider"
import { routing } from "@/i18n/routing"
import { fontClassNames } from "@/lib/fonts"

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }))
}

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Metadata")
  return {
    // Pages set their own title; the brand follows it ("Sign up · Nemouw")
    title: { default: t("title"), template: `%s · ${t("title")}` },
    description: t("description"),
  }
}

export default async function LocaleLayout({
  children,
}: LayoutProps<"/[locale]">) {
  const locale = await getLocale()

  return (
    <html
      lang={locale}
      dir={localeDirection(locale)}
      suppressHydrationWarning
      className={cn("font-sans antialiased", fontClassNames(locale))}
    >
      <body>
        <NextIntlClientProvider>
          <ThemeProvider>{children}</ThemeProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  )
}
