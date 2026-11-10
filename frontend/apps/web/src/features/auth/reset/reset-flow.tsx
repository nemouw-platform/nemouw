"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { buttonVariants } from "@workspace/ui/components/button"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { AuthPromptLink } from "@/features/auth/components/auth-prompt"
import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { FormAlert } from "@/features/auth/components/form-alert"
import { NewPasswordField } from "@/features/auth/components/new-password-field"
import { SubmitButton } from "@/features/auth/components/submit-button"
import {
  createResetSchema,
  type ResetValues,
} from "@/features/auth/reset/reset-schema"
import { useResetPassword } from "@/features/auth/reset/use-reset-password"
import { useResetToken } from "@/features/auth/reset/use-reset-token"
import { Link } from "@/i18n/navigation"

/** The page body for the emailed link: form, "link expired", or "done" */
export function ResetFlow() {
  const t = useTranslations("ResetPassword")
  const token = useResetToken()

  // A link without a token can only be asked for again
  if (token === null) return <InvalidLink />

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader title={t("title")} description={t("description")} />
      {/* The form waits for the token, which only the browser can read */}
      {token && <ResetForm token={token} />}
    </div>
  )
}

function ResetForm({ token }: { token: string }) {
  const t = useTranslations("ResetPassword")
  const tAuth = useTranslations("Auth")
  const tAuthErrors = useTranslations("Auth.errors")
  const schema = React.useMemo(
    () =>
      createResetSchema({ password: tAuth("fields.password") }, tAuthErrors),
    [tAuth, tAuthErrors]
  )
  const form = useForm<ResetValues>({
    resolver: zodResolver(schema),
    defaultValues: { new_password: "" },
    mode: "onTouched",
  })
  const reset = useResetPassword(token, form)

  if (reset.done) return <Done />
  if (reset.invalidLink) return <InvalidLinkAlert />

  return (
    <form noValidate onSubmit={reset.submit}>
      <FieldGroup>
        <NewPasswordField form={form} name="new_password" autoFocus />
        <FormAlert>{reset.formError}</FormAlert>
        <SubmitButton pending={reset.isPending} pendingLabel={t("submitting")}>
          {t("submit")}
        </SubmitButton>
      </FieldGroup>
    </form>
  )
}

function InvalidLink() {
  const t = useTranslations("ResetPassword")
  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader title={t("title")} />
      <InvalidLinkAlert />
    </div>
  )
}

function InvalidLinkAlert() {
  const t = useTranslations("ResetPassword.errors")
  return (
    <FormAlert>
      {t("invalidLink")}{" "}
      <AuthPromptLink href="/forgot-password">
        {t("requestNewLink")}
      </AuthPromptLink>
    </FormAlert>
  )
}

function Done() {
  const t = useTranslations("ResetPassword")
  return (
    <div className="flex flex-col gap-6" role="status">
      <FormAlert tone="success">
        <strong className="font-medium">{t("doneTitle")}.</strong>{" "}
        {t("doneDescription")}
      </FormAlert>
      <Link href="/login" className={buttonVariants({ size: "lg" })}>
        {t("logIn")}
      </Link>
    </div>
  )
}
