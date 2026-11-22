"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormControllerField } from "@/components/form/form-controller-field"
import { FormTextField } from "@/components/form/form-field"
import { PhoneInput } from "@/components/form/phone-input"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import { useSyndicate } from "@/features/syndicate/me/syndicate-provider"
import {
  createSyndicateSchema,
  SYNDICATE_FIELDS,
  toSyndicateRequest,
  toSyndicateValues,
  type SyndicateValues,
} from "@/features/syndicate/settings/settings-schema"
import { syndicatesApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

/**
 * The syndicate's name and how specialists and patients reach it. The
 * owner edits them; the rest of the staff see them read-only.
 */
export function SettingsForm() {
  const t = useTranslations("Syndicate.settings")
  const tFormErrors = useTranslations("Form.errors")
  const { syndicate, setSyndicate, isOwner } = useSyndicate()
  const schema = React.useMemo(
    () =>
      createSyndicateSchema({ name: t("name") }, tFormErrors, {
        phoneFormat: t("phoneFormat"),
      }),
    [t, tFormErrors]
  )
  const form = useForm<SyndicateValues>({
    resolver: zodResolver(schema),
    defaultValues: toSyndicateValues(syndicate),
    mode: "onTouched",
  })
  const save = useFormRequest(
    (signal, values: SyndicateValues) =>
      syndicatesApi.update(syndicate.id ?? "", toSyndicateRequest(values), {
        signal,
      }),
    {
      onSuccess: (saved) => {
        setSyndicate(saved)
        form.reset(toSyndicateValues(saved))
      },
      onError: (error) =>
        applyFieldErrors(form, error, SYNDICATE_FIELDS, tFormErrors),
    }
  )
  const { errors, isDirty } = form.formState
  // Not loaded (see createSyndicateSchema): say that blank keeps them
  const contactHint =
    isOwner && !syndicate.contact_email ? t("keepHint") : undefined

  return (
    <FormCard
      title={t("detailsTitle")}
      description={isOwner ? t("detailsDescription") : t("readOnly")}
      onSubmit={form.handleSubmit((values) => save.run(values))}
      formError={save.formError}
      savedMessage={save.succeeded && !isDirty ? t("saved") : undefined}
      submit={
        isOwner ? (
          <SubmitButton
            pending={save.isPending}
            pendingLabel={t("saving")}
            size="default"
          >
            {t("save")}
          </SubmitButton>
        ) : undefined
      }
    >
      <fieldset disabled={!isOwner} className="contents">
        <FormTextField
          id="name"
          required
          label={t("name")}
          placeholder={t("namePlaceholder")}
          error={errors.name}
          {...form.register("name")}
        />
        <div className="grid gap-5 sm:grid-cols-2">
          <FormTextField
            id="contact_email"
            type="email"
            dir="ltr"
            autoComplete="email"
            label={t("email")}
            description={contactHint}
            placeholder={t("emailPlaceholder")}
            error={errors.contact_email}
            {...form.register("contact_email")}
          />
          <FormControllerField
            name="contact_phone"
            control={form.control}
            label={t("phone")}
            description={contactHint}
            error={errors.contact_phone}
            render={(props) => <PhoneInput {...props} disabled={!isOwner} />}
          />
        </div>
      </fieldset>
    </FormCard>
  )
}
