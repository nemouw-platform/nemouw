"use client"

import { FormAlert } from "@/components/form/form-alert"
import { FormField, FormTextField } from "@/components/form/form-field"
import { SubmitButton } from "@/components/form/submit-button"
import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { Controller, useForm } from "react-hook-form"

import { AuthPromptButton } from "@/features/auth/components/auth-prompt"
import {
  createOnboardingSchema,
  type OnboardingValues,
} from "@/features/auth/onboarding/onboarding-schema"
import { SyndicateSelect } from "@/features/auth/onboarding/syndicate-select"
import {
  useOnboarding,
  useRedirectIfOnboarded,
} from "@/features/auth/onboarding/use-onboarding"
import { useSyndicates } from "@/features/auth/onboarding/use-syndicates"

export function OnboardingForm() {
  const t = useTranslations("Onboarding")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () =>
      createOnboardingSchema(
        {
          username: t("username"),
          syndicate: t("syndicate"),
          memberNumber: t("memberNumber"),
        },
        tFormErrors,
        t("errors.usernameFormat")
      ),
    [t, tFormErrors]
  )

  const form = useForm<OnboardingValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      username: "",
      syndicate_id: "",
      syndicate_member_number: "",
    },
    mode: "onTouched",
  })
  useRedirectIfOnboarded()
  const syndicates = useSyndicates()
  const { submit, isPending, formError } = useOnboarding(form)
  const { errors } = form.formState

  return (
    <form noValidate onSubmit={submit}>
      <FieldGroup>
        <FormTextField
          id="username"
          required
          label={t("username")}
          placeholder={t("usernamePlaceholder")}
          description={t("usernameHint")}
          autoComplete="off"
          autoCapitalize="none"
          spellCheck={false}
          // Usernames are Latin characters, even on Arabic pages
          dir="ltr"
          autoFocus
          error={errors.username}
          {...form.register("username")}
        />

        <FormField
          id="syndicate_id"
          required
          label={t("syndicate")}
          error={errors.syndicate_id}
          description={<SyndicatesStatus {...syndicates} />}
        >
          {(control) => (
            <Controller
              name="syndicate_id"
              control={form.control}
              render={({ field }) => (
                <SyndicateSelect
                  {...control}
                  syndicates={syndicates.syndicates}
                  isLoading={syndicates.isLoading}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  ref={field.ref}
                />
              )}
            />
          )}
        </FormField>

        <FormTextField
          id="syndicate_member_number"
          required
          label={t("memberNumber")}
          placeholder={t("memberNumberPlaceholder")}
          description={t("memberNumberHint")}
          autoComplete="off"
          dir="ltr"
          error={errors.syndicate_member_number}
          {...form.register("syndicate_member_number")}
        />

        <FormAlert>{formError}</FormAlert>

        <SubmitButton pending={isPending} pendingLabel={t("submitting")}>
          {t("submit")}
        </SubmitButton>
      </FieldGroup>
    </form>
  )
}

/** Only says something when the list could not load or is empty */
function SyndicatesStatus({
  syndicates,
  isLoading,
  failed,
  retry,
}: ReturnType<typeof useSyndicates>) {
  const t = useTranslations("Onboarding")

  if (failed) {
    return (
      <>
        {t("syndicatesFailed")}{" "}
        <AuthPromptButton onClick={retry}>{t("retry")}</AuthPromptButton>
      </>
    )
  }
  if (!isLoading && syndicates.length === 0) return t("syndicatesEmpty")
  return null
}
