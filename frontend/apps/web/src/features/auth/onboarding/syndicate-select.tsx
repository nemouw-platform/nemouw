"use client"

import type { SyndicateSummary } from "@workspace/api"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select"
import { useTranslations } from "next-intl"

import type { ControlProps } from "@/features/auth/components/form-field"

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
  value,
  onChange,
  onBlur,
  ref,
  ...control
}: SyndicateSelectProps) {
  const t = useTranslations("Onboarding")
  const items = syndicates.map((syndicate) => ({
    value: syndicate.id ?? "",
    label: syndicate.name ?? "",
  }))

  return (
    <Select
      items={items}
      value={value || null}
      onValueChange={(next) => onChange(next ?? "")}
      disabled={isLoading || items.length === 0}
    >
      <SelectTrigger
        {...control}
        ref={ref}
        onBlur={onBlur}
        // Full width, white like the other fields on the lavender page
        className="h-10 w-full bg-card"
      >
        <SelectValue
          placeholder={
            isLoading ? t("syndicatesLoading") : t("syndicatePlaceholder")
          }
        />
      </SelectTrigger>
      <SelectContent>
        {items.map((item) => (
          <SelectItem key={item.value} value={item.value}>
            {item.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
