"use client"

import { useTranslations } from "next-intl"
import type { FieldValues, Path, UseFormSetError } from "react-hook-form"

import { classifyMembershipError } from "@/features/specialists/membership/membership-errors"
import type { MembershipField } from "@/features/specialists/membership/membership-schema"
import { fieldErrorMessage } from "@/lib/forms/api-errors"

interface Options {
  /** The membership fields this form has */
  fields: readonly MembershipField[]
  /** For onboarding: the profile exists already (e.g. made in another tab) */
  onAlreadyOnboarded?: () => void
}

/**
 * The onError for forms that set the username, syndicate or member number:
 * puts each failure on its field, or says it above the button (with
 * `setFormError`, e.g. the syndicate is locked). Returns false for failures
 * the general message covers.
 */
export function useMembershipErrors<TValues extends FieldValues>(
  setError: UseFormSetError<TValues>,
  { fields, onAlreadyOnboarded }: Options
) {
  const t = useTranslations("Membership.errors")
  const tFormErrors = useTranslations("Form.errors")

  return (
    error: unknown,
    setFormError?: (message: string) => void
  ): boolean => {
    const failure = classifyMembershipError(error, fields)
    const onField = (name: MembershipField, message: string) =>
      setError(name as Path<TValues>, { message }, { shouldFocus: true })

    switch (failure.kind) {
      case "usernameTaken":
        onField("username", t("usernameTaken"))
        return true
      case "usernameUnavailable":
        onField("username", t("usernameUnavailable"))
        return true
      case "memberNumberTaken":
        onField("syndicate_member_number", t("memberNumberTaken"))
        return true
      case "syndicateLocked":
        if (!setFormError) return false
        setFormError(t("syndicateLocked"))
        return true
      case "alreadyOnboarded":
        if (!onAlreadyOnboarded) return false
        onAlreadyOnboarded()
        return true
      case "fields":
        failure.fields.forEach(({ name, code }) =>
          setError(name as Path<TValues>, {
            message: fieldErrorMessage(code, tFormErrors),
          })
        )
        return true
      case "general":
        return false
    }
  }
}
