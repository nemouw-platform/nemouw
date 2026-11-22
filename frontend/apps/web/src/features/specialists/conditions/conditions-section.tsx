"use client"

import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { StethoscopeIcon } from "lucide-react"
import { useTranslations } from "next-intl"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { ConditionsForm } from "@/features/specialists/conditions/conditions-form"
import {
  useConditionList,
  useTreatedConditions,
} from "@/features/specialists/conditions/use-conditions"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/** The conditions page: the specialist's picks from the platform's list */
export function ConditionsSection() {
  const t = useTranslations("Conditions")
  const tFormErrors = useTranslations("Form.errors")
  const list = useConditionList()
  const treated = useTreatedConditions()
  const failure = [list, treated].find((load) => load.failed)

  let body: React.ReactNode
  if (failure) {
    body = (
      <LoadError
        message={generalErrorMessage(failure.error, tFormErrors)}
        onRetry={failure.retry}
      />
    )
  } else if (list.isLoading || treated.isLoading) {
    body = <Skeleton className="h-80 w-full rounded-xl" />
  } else if (list.conditions.length === 0) {
    body = (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
          <StethoscopeIcon
            aria-hidden
            className="size-8 text-muted-foreground"
          />
          <p className="max-w-sm text-sm text-pretty text-muted-foreground">
            {t("empty")}
          </p>
        </CardContent>
      </Card>
    )
  } else {
    body = (
      <ConditionsForm
        conditions={list.conditions}
        saved={treated.ids}
        onSaved={treated.setIds}
      />
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("description")} />
      {body}
    </div>
  )
}
