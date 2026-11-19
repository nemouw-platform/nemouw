"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { LockIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { useUpdateSpecialist } from "@/features/specialists/me/use-update-specialist"
import {
  MemberNumberField,
  SyndicateField,
} from "@/features/specialists/membership/membership-fields"
import { membershipRules } from "@/features/specialists/membership/membership-schema"
import { useMembershipErrors } from "@/features/specialists/membership/use-membership-errors"

/**
 * Changes the syndicate and member number. Once the syndicate has verified
 * the profile it is locked (the backend refuses: SYNDICATE_LOCKED); the
 * member number can still be corrected.
 */
export function MembershipForm() {
  const t = useTranslations("Settings.membership")
  const tMembership = useTranslations("Membership")
  const tFormErrors = useTranslations("Form.errors")
  const { specialist } = useSpecialist()
  const locked = specialist.verification_status === "active"

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
    return z.object({
      syndicate_id: rules.syndicate_id,
      syndicate_member_number: rules.syndicate_member_number,
    })
  }, [tMembership, tFormErrors])

  type Values = z.infer<typeof schema>
  const toValues = (saved: typeof specialist): Values => ({
    syndicate_id: saved.syndicate_id ?? "",
    syndicate_member_number: saved.syndicate_member_number ?? "",
  })
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: toValues(specialist),
    mode: "onTouched",
  })
  const membershipErrors = useMembershipErrors(form.setError, {
    fields: ["syndicate_id", "syndicate_member_number"],
  })
  const { submit, isPending, formError, saved } = useUpdateSpecialist(form, {
    // A new syndicate needs its member number in the same request: send both
    toRequest: (values) => values,
    toValues,
    onError: membershipErrors,
  })
  const { errors } = form.formState

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
      <SyndicateField
        name="syndicate_id"
        control={form.control}
        error={errors.syndicate_id}
        disabled={locked}
        description={
          locked && (
            <span className="flex items-center gap-1.5">
              <LockIcon aria-hidden className="size-3.5" />
              {t("locked")}
            </span>
          )
        }
      />
      <MemberNumberField
        registration={form.register("syndicate_member_number")}
        error={errors.syndicate_member_number}
      />
    </FormCard>
  )
}
