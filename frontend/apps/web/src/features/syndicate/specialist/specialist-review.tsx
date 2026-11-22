"use client"

import { buttonVariants } from "@workspace/ui/components/button"
import { isApiError } from "@workspace/api"
import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { ArrowLeftIcon, FileTextIcon } from "lucide-react"
import { useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"

import { FormAlert } from "@/components/form/form-alert"
import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { useSyndicate } from "@/features/syndicate/me/syndicate-provider"
import { SYNDICATE_SECTIONS } from "@/features/syndicate/sections"
import { CredentialReview } from "@/features/syndicate/specialist/credential-review"
import { SpecialistDecision } from "@/features/syndicate/specialist/specialist-decision"
import { useSpecialistReview } from "@/features/syndicate/specialist/use-specialist-review"
import { Link } from "@/i18n/navigation"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/** /syndicate/specialist?id=…: the decision on a specialist and their credentials */
export function SpecialistReview() {
  const t = useTranslations("Syndicate.specialist")
  const id = useSearchParams().get("id")

  return (
    <div className="flex flex-col gap-6">
      <Link
        href={SYNDICATE_SECTIONS.review.href}
        className={buttonVariants({
          variant: "ghost",
          size: "sm",
          className: "-ms-2 self-start",
        })}
      >
        <ArrowLeftIcon data-icon="inline-start" className="rtl:-scale-x-100" />
        {t("back")}
      </Link>
      <PageHeader title={t("title")} description={t("description")} />
      {id ? (
        <ReviewBody key={id} specialistId={id} />
      ) : (
        <FormAlert>{t("notFound")}</FormAlert>
      )}
    </div>
  )
}

/** The specialist's decision card, then each credential to review */
function ReviewBody({ specialistId }: { specialistId: string }) {
  const t = useTranslations("Syndicate.specialist")
  const tFormErrors = useTranslations("Form.errors")
  const id = specialistId
  const { syndicate } = useSyndicate()
  const load = useSpecialistReview(syndicate.id ?? "", id)
  const review = load.review

  let body: React.ReactNode
  if (load.failed && isApiError(load.error) && load.error.status === 404) {
    // Not a specialist of this syndicate (or no longer one)
    body = <FormAlert>{t("notFound")}</FormAlert>
  } else if (load.failed) {
    body = (
      <LoadError
        message={generalErrorMessage(load.error, tFormErrors)}
        onRetry={load.retry}
      />
    )
  } else if (!review) {
    body = (
      <>
        <Skeleton className="h-48 w-full rounded-xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </>
    )
  } else {
    body = (
      <>
        <SpecialistDecision
          specialist={review.specialist}
          onChange={load.setSpecialist}
        />
        <section className="flex flex-col gap-3" aria-labelledby="credentials">
          <div className="flex flex-col gap-1">
            <h2 id="credentials" className="text-lg font-semibold">
              {t("credentialsTitle")}
            </h2>
            <p className="text-sm text-pretty text-muted-foreground">
              {t("credentialsDescription")}
            </p>
          </div>
          {review.credentials.length === 0 ? (
            <Card>
              <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
                <FileTextIcon
                  aria-hidden
                  className="size-8 text-muted-foreground"
                />
                <p className="text-sm text-muted-foreground">
                  {t("credentialsEmpty")}
                </p>
              </CardContent>
            </Card>
          ) : (
            review.credentials.map((credential) => (
              <CredentialReview
                key={credential.id}
                credential={credential}
                onChange={load.setCredential}
              />
            ))
          )}
        </section>
      </>
    )
  }

  return body
}
