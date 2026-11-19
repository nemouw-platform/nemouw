"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { useUpdateSpecialist } from "@/features/specialists/me/use-update-specialist"
import { UsernameField } from "@/features/specialists/membership/membership-fields"
import { membershipRules } from "@/features/specialists/membership/membership-schema"
import { useMembershipErrors } from "@/features/specialists/membership/use-membership-errors"

/** Changes the public username (the profile's link) */
export function UsernameForm() {
  const t = useTranslations("Settings.username")
  const tMembership = useTranslations("Membership")
  const tFormErrors = useTranslations("Form.errors")
  const { specialist } = useSpecialist()

  const schema = React.useMemo(() => {
    const rules = membershipRules(
      {
        username: tMembership("username"),
        syndicate: tMembership("syndicate"),
        memberNumber: tMembership("memberNumber"),
      },
      tFormErrors,
      tMembership("errors.usernameFormat")
    )
    return z.object({ username: rules.username })
  }, [tMembership, tFormErrors])

  type Values = z.infer<typeof schema>
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { username: specialist.username ?? "" },
    mode: "onTouched",
  })
  const membershipErrors = useMembershipErrors(form.setError, {
    fields: ["username"],
  })
  const { submit, isPending, formError, saved } = useUpdateSpecialist(form, {
    toRequest: (values) => ({ username: values.username }),
    toValues: (saved) => ({ username: saved.username ?? "" }),
    onError: membershipErrors,
  })

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
      <UsernameField
        registration={form.register("username")}
        error={form.formState.errors.username}
      />
    </FormCard>
  )
}
