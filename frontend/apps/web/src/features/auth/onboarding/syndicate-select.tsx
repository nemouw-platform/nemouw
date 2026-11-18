"use client"

import type { SyndicateSummary } from "@workspace/api"
import { useTranslations } from "next-intl"

import type { ControlProps } from "@/components/form/form-field"
import { SelectControl } from "@/components/form/select-control"

interface SyndicateSelectProps extends ControlProps {
  syndicates: SyndicateSummary[]
  isLoading: boolean
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  ref?: React.Ref<HTMLButtonElement>
}

/** Picker for the public syndicate list; shows its loading state in place */
export function SyndicateSelect({
  syndicates,
  isLoading,
  ...props
}: SyndicateSelectProps) {
  const t = useTranslations("Onboarding")
  const options = syndicates.map((syndicate) => ({
    value: syndicate.id ?? "",
    label: syndicate.name ?? "",
  }))

  return (
    <SelectControl
      {...props}
      options={options}
      disabled={isLoading || options.length === 0}
      placeholder={
        isLoading ? t("syndicatesLoading") : t("syndicatePlaceholder")
      }
    />
  )
}
