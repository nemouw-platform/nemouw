"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormControllerField } from "@/components/form/form-controller-field"
import { FormTextField } from "@/components/form/form-field"
import { PhoneInput } from "@/components/form/phone-input"
import { SelectControl } from "@/components/form/select-control"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  createDetailsSchema,
  GENDERS,
  PRIMARY_LANGUAGES,
  toDetailsValues,
  type DetailsValues,
} from "@/features/specialists/details/details-schema"
import { useSaveDetails } from "@/features/specialists/details/use-save-details"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"

/** Practical facts patients filter and decide by */
export function DetailsForm() {
  const t = useTranslations("Profile.details")
  const tLanguages = useTranslations("Profile.languages")
  const tFormErrors = useTranslations("Form.errors")
  const { specialist } = useSpecialist()

  const schema = React.useMemo(
    () =>
      createDetailsSchema(tFormErrors, {
        phoneFormat: t("phoneFormat"),
        ageRange: t("ageRange"),
      }),
    [t, tFormErrors]
  )
  const form = useForm<DetailsValues>({
    resolver: zodResolver(schema),
    defaultValues: toDetailsValues(specialist),
    mode: "onTouched",
  })
  const { submit, isPending, formError, saved } = useSaveDetails(form)
  const { errors } = form.formState

  const genders = GENDERS.map((value) => ({
    value,
    label: t(`genders.${value}`),
  }))
  const languages = PRIMARY_LANGUAGES.map((value) => ({
    value,
    label: tLanguages(value),
  }))

  return (
    <FormCard
      title={t("title")}
      description={t("description")}
      onSubmit={submit}
      formError={formError}
      savedMessage={saved && t("saved")}
      submit={
        <SubmitButton
          pending={isPending}
          pendingLabel={t("saving")}
          size="default"
        >
          {t("save")}
        </SubmitButton>
      }
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <FormTextField
          id="years_of_experience"
          label={t("experience")}
          placeholder={t("experiencePlaceholder")}
          description={t("experienceHint")}
          inputMode="numeric"
          error={errors.years_of_experience}
          {...form.register("years_of_experience")}
        />
        <FormControllerField
          name="whatsapp_number"
          control={form.control}
          label={t("whatsapp")}
          description={t("whatsappHint")}
          error={errors.whatsapp_number}
          render={(props) => <PhoneInput {...props} />}
        />
        <FormControllerField
          name="gender"
          control={form.control}
          label={t("gender")}
          error={errors.gender}
          render={(props) => (
            <SelectControl
              {...props}
              options={genders}
              placeholder={t("notSpecified")}
              noneLabel={t("notSpecified")}
            />
          )}
        />
        <FormControllerField
          name="primary_language"
          control={form.control}
          label={t("language")}
          description={t("languageHint")}
          error={errors.primary_language}
          render={(props) => (
            <SelectControl
              {...props}
              options={languages}
              placeholder={t("notSpecified")}
              noneLabel={t("notSpecified")}
            />
          )}
        />
        <FormTextField
          id="min_age_treated"
          label={t("minAge")}
          placeholder={t("minAgePlaceholder")}
          inputMode="numeric"
          error={errors.min_age_treated}
          {...form.register("min_age_treated")}
        />
        <FormTextField
          id="max_age_treated"
          label={t("maxAge")}
          placeholder={t("maxAgePlaceholder")}
          description={t("ageHint")}
          inputMode="numeric"
          error={errors.max_age_treated}
          {...form.register("max_age_treated")}
        />
      </div>
    </FormCard>
  )
}
