"use client"

import { Button } from "@workspace/ui/components/button"
import { RotateCwIcon } from "lucide-react"
import { useTranslations } from "next-intl"

import { FormAlert } from "@/components/form/form-alert"

/** In place of content that could not load: what happened and a retry */
export function LoadError({
  message,
  onRetry,
}: {
  message: React.ReactNode
  onRetry: () => void
}) {
  const t = useTranslations("Common")

  return (
    <div className="flex flex-col items-start gap-3">
      <FormAlert>{message}</FormAlert>
      <Button variant="outline" onClick={onRetry}>
        <RotateCwIcon data-icon="inline-start" />
        {t("retry")}
      </Button>
    </div>
  )
}
