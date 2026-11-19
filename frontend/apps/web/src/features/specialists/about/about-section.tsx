"use client"

import { localeNames, locales, type Locale } from "@workspace/i18n"
import { Skeleton } from "@workspace/ui/components/skeleton"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@workspace/ui/components/tabs"
import { useLocale, useTranslations } from "next-intl"

import { LoadError } from "@/components/load-error"
import { AboutForm } from "@/features/specialists/about/about-form"
import { hasBio } from "@/features/specialists/about/about-schema"
import { useProfileContent } from "@/features/specialists/about/use-profile-content"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/**
 * The public text, one tab per site language. Patients read the version in
 * their language; the tab starts on the page's language.
 */
export function AboutSection() {
  const t = useTranslations("Profile.about")
  const tFormErrors = useTranslations("Form.errors")
  const pageLocale = useLocale() as Locale
  const { content, isLoading, failed, error, retry, replace } =
    useProfileContent()

  return (
    <section
      id="about"
      aria-labelledby="about-heading"
      // Clear of the sticky top bar when opened from a #about link
      className="flex scroll-mt-24 flex-col gap-4"
    >
      <div className="flex flex-col gap-1">
        <h2 id="about-heading" className="text-lg font-semibold">
          {t("title")}
        </h2>
        <p className="text-sm text-pretty text-muted-foreground">
          {t("description")}
        </p>
      </div>

      {failed ? (
        <LoadError
          message={generalErrorMessage(error, tFormErrors)}
          onRetry={retry}
        />
      ) : isLoading ? (
        <Skeleton className="h-96 w-full rounded-xl" />
      ) : (
        <Tabs defaultValue={pageLocale} className="gap-4">
          <TabsList aria-label={t("languages")}>
            {locales.map((locale) => (
              <TabsTrigger key={locale} value={locale} className="px-3">
                {localeNames[locale]}
                {hasBio(content[locale]) && (
                  <span
                    aria-label={t("written")}
                    className="size-1.5 rounded-full bg-primary"
                  />
                )}
              </TabsTrigger>
            ))}
          </TabsList>
          {locales.map((locale) => (
            // Kept mounted, so switching tabs keeps unsaved edits
            <TabsContent key={locale} value={locale} keepMounted>
              <AboutForm
                locale={locale}
                content={content[locale]}
                onSaved={replace}
              />
            </TabsContent>
          ))}
        </Tabs>
      )}
    </section>
  )
}
