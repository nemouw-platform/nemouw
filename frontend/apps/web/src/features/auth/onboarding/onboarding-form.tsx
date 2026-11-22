"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormAlert } from "@/components/form/form-alert"
import { SubmitButton } from "@/components/form/submit-button"
import {
  createOnboardingSchema,
  type OnboardingValues,
} from "@/features/auth/onboarding/onboarding-schema"
import {
  useOnboarding,
  useRedirectIfOnboarded,
} from "@/features/auth/onboarding/use-onboarding"
import {
  MemberNumberField,
  SyndicateField,
  UsernameField,
} from "@/features/specialists/membership/membership-fields"

export function OnboardingForm() {
  const t = useTranslations("Onboarding")
  const tMembership = useTranslations("Membership")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () =>
      createOnboardingSchema(
        {
          username: tMembership("username"),
          syndicate: tMembership("syndicate"),
          memberNumber: tMembership("memberNumber"),
        },
        tFormErrors,
        tMembership("errors.usernameFormat")
      ),
    [tMembership, tFormErrors]
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
  const { submit, isPending, formError } = useOnboarding(form)
  const { errors } = form.formState

  return (
    <form noValidate onSubmit={submit}>
      <FieldGroup>
        <UsernameField
          registration={form.register("username")}
          error={errors.username}
          autoFocus
        />
        <SyndicateField
          name="syndicate_id"
          control={form.control}
          error={errors.syndicate_id}
        />
        <MemberNumberField
          registration={form.register("syndicate_member_number")}
          error={errors.syndicate_member_number}
        />

        <FormAlert>{formError}</FormAlert>

        <SubmitButton pending={isPending} pendingLabel={t("submitting")}>
          {t("submit")}
        </SubmitButton>
      </FieldGroup>
    </form>
  )
}
