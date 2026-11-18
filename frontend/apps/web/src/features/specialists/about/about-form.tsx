"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import type { ApiLocale, SpecialistProfileResponse } from "@workspace/api"
import { localeDirection } from "@workspace/i18n"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm, useWatch } from "react-hook-form"

import { FormTextareaField, FormTextField } from "@/components/form/form-field"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  ABOUT_FIELDS,
  ABOUT_MAX_LENGTH,
  createAboutSchema,
  toAboutValues,
  toWriteRequest,
  type AboutValues,
} from "@/features/specialists/about/about-schema"
import { specialistsApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

interface AboutFormProps {
  /** The language this text is written in (not the page's language) */
  locale: ApiLocale
  content: SpecialistProfileResponse | undefined
  onSaved: (content: SpecialistProfileResponse) => void
}

/** The public text for one language: title, headline and bio */
export function AboutForm({ locale, content, onSaved }: AboutFormProps) {
  const t = useTranslations("Profile.about")
  const tLanguages = useTranslations("Profile.languages")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () => createAboutSchema(tFormErrors),
    [tFormErrors]
  )
  const form = useForm<AboutValues>({
    resolver: zodResolver(schema),
    defaultValues: toAboutValues(content),
    mode: "onTouched",
  })

  const save = useFormRequest(
    (signal, values: AboutValues) =>
      specialistsApi.saveContent(locale, toWriteRequest(values), { signal }),
    {
      onSuccess: (saved) => {
        onSaved(saved)
        form.reset(toAboutValues(saved))
      },
      onError: (error) =>
        applyFieldErrors(form, error, ABOUT_FIELDS, tFormErrors),
    }
  )
  const { errors } = form.formState
  const bioLength = useWatch({ control: form.control, name: "bio" }).length
  const language = tLanguages(locale)
  // The fields are written in their own language, whatever the page's is
  const textProps = { lang: locale, dir: localeDirection(locale) }

  return (
    <FormCard
      title={t("formTitle", { language })}
      description={t("formDescription", { language })}
      onSubmit={form.handleSubmit((values) => save.run(values))}
      formError={save.formError}
      savedMessage={save.succeeded && !form.formState.isDirty && t("saved")}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("saving")}
          size="default"
        >
          {t("save", { language })}
        </SubmitButton>
      }
    >
      <FormTextField
        id={`title-${locale}`}
        label={t("titleLabel")}
        placeholder={t("titlePlaceholder")}
        description={t("titleHint")}
        error={errors.title}
        {...textProps}
        {...form.register("title")}
      />
      <FormTextField
        id={`headline-${locale}`}
        label={t("headline")}
        placeholder={t("headlinePlaceholder")}
        description={t("headlineHint")}
        error={errors.headline}
        {...textProps}
        {...form.register("headline")}
      />
      <FormTextareaField
        id={`bio-${locale}`}
        label={t("bio")}
        placeholder={t("bioPlaceholder")}
        description={t("bioCount", {
          count: bioLength,
          max: ABOUT_MAX_LENGTH.bio,
        })}
        rows={8}
        error={errors.bio}
        {...textProps}
        {...form.register("bio")}
      />
    </FormCard>
  )
}
