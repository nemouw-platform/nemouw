"use client"

import type { Condition } from "@workspace/api"
import { Checkbox } from "@workspace/ui/components/checkbox"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@workspace/ui/components/input-group"
import { SearchIcon } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  conditionLabel,
  matchesCondition,
  sameSelection,
  sortConditions,
} from "@/features/specialists/conditions/conditions"
import { specialistsApi } from "@/lib/api"
import { useFormRequest } from "@/lib/forms/use-form-request"

interface ConditionsFormProps {
  /** Every condition there is to pick */
  conditions: Condition[]
  /** The ids saved now */
  saved: string[]
  onSaved: (ids: string[]) => void
}

/**
 * The conditions the specialist treats: a searchable list of checkboxes,
 * saved together (the backend replaces the whole selection)
 */
export function ConditionsForm({
  conditions,
  saved,
  onSaved,
}: ConditionsFormProps) {
  const t = useTranslations("Conditions.form")
  const locale = useLocale()
  const [selected, setSelected] = React.useState(saved)
  const [query, setQuery] = React.useState("")
  const save = useFormRequest(
    (signal, ids: string[]) => specialistsApi.saveConditions(ids, { signal }),
    { onSuccess: onSaved }
  )

  const shown = sortConditions(conditions, locale).filter((condition) =>
    matchesCondition(condition, query, locale)
  )
  const unchanged = sameSelection(selected, saved)
  const toggle = (id: string, checked: boolean) =>
    setSelected((ids) =>
      checked ? [...ids, id] : ids.filter((item) => item !== id)
    )

  return (
    <FormCard
      title={t("title")}
      description={t("description")}
      onSubmit={(event) => {
        event.preventDefault()
        save.run(selected)
      }}
      formError={save.formError}
      savedMessage={save.succeeded && unchanged && t("saved")}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("saving")}
          size="default"
        >
          {t("save")}
        </SubmitButton>
      }
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <InputGroup className="h-10 bg-card sm:max-w-xs">
          <InputGroupAddon align="inline-start">
            <SearchIcon aria-hidden />
          </InputGroupAddon>
          <InputGroupInput
            type="search"
            aria-label={t("search")}
            placeholder={t("searchPlaceholder")}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </InputGroup>
        <p role="status" className="text-sm text-muted-foreground">
          {t("selected", { count: selected.length })}
        </p>
      </div>

      {shown.length === 0 ? (
        <p className="py-6 text-center text-sm text-muted-foreground">
          {t("noMatch")}
        </p>
      ) : (
        <ul aria-label={t("title")} className="grid gap-1 sm:grid-cols-2">
          {shown.map((condition) => {
            const id = condition.id ?? ""
            return (
              <li key={id}>
                <label className="flex cursor-pointer items-start gap-3 rounded-lg p-2.5 transition-colors hover:bg-muted has-data-checked:bg-primary/5">
                  <Checkbox
                    className="mt-0.5"
                    checked={selected.includes(id)}
                    onCheckedChange={(checked) => toggle(id, checked === true)}
                  />
                  <span className="flex min-w-0 flex-col gap-0.5">
                    <span className="text-sm font-medium">
                      {conditionLabel(condition, locale)}
                    </span>
                    {/* Descriptions exist in English only */}
                    {locale === "en" && condition.description && (
                      <span className="text-xs text-muted-foreground">
                        {condition.description}
                      </span>
                    )}
                  </span>
                </label>
              </li>
            )
          })}
        </ul>
      )}
    </FormCard>
  )
}
