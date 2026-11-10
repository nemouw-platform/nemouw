"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import {
  AuthPrompt,
  AuthPromptLink,
} from "@/features/auth/components/auth-prompt"
import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { FormEmailField } from "@/features/auth/components/email-field"
import { FormAlert } from "@/features/auth/components/form-alert"
import { ResendPrompt } from "@/features/auth/components/resend-prompt"
import { SubmitButton } from "@/features/auth/components/submit-button"
import {
  createForgotSchema,
  type ForgotValues,
} from "@/features/auth/forgot/forgot-schema"
import { useForgotPassword } from "@/features/auth/forgot/use-forgot-password"
import { richText } from "@/features/auth/lib/rich-text"

/** Ask for the email, then "check your inbox" with a resend countdown */
export function ForgotFlow() {
  const t = useTranslations("ForgotPassword")
  const tAuth = useTranslations("Auth")
  const tAuthErrors = useTranslations("Auth.errors")
  const schema = React.useMemo(
    () => createForgotSchema({ email: tAuth("fields.email") }, tAuthErrors),
    [tAuth, tAuthErrors]
  )
  const form = useForm<ForgotValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "" },
    mode: "onTouched",
  })
  const forgot = useForgotPassword(form)

  return (
    <div className="flex flex-col gap-8">
      {forgot.sentTo ? (
        <>
          <AuthStepHeader
            title={t("sentTitle")}
            description={t.rich("sentDescription", {
              email: forgot.sentTo,
              ...richText,
            })}
          />
          <p className="text-sm text-muted-foreground">{tAuth("spamHint")}</p>
          <FormAlert>{forgot.formError}</FormAlert>
          <FormAlert tone="success">
            {forgot.resent &&
              t.rich("resent", { email: forgot.sentTo, ...richText })}
          </FormAlert>
          <ResendPrompt
            prompt={t("resendPrompt")}
            action={t("resend")}
            pendingLabel={t("resending")}
            waitingLabel={(time) => t("resendIn", { time })}
            onResend={forgot.resend}
            pending={forgot.isPending}
            secondsLeft={forgot.secondsLeft}
          />
        </>
      ) : (
        <>
          <AuthStepHeader title={t("title")} description={t("description")} />
          <form noValidate onSubmit={forgot.submit}>
            <FieldGroup>
              <FormEmailField
                id="email"
                required
                autoFocus
                error={form.formState.errors.email}
                {...form.register("email")}
              />
              <FormAlert>{forgot.formError}</FormAlert>
              <SubmitButton
                pending={forgot.isPending}
                pendingLabel={t("submitting")}
              >
                {t("submit")}
              </SubmitButton>
            </FieldGroup>
          </form>
        </>
      )}

      <AuthPrompt prompt={t("rememberPassword")}>
        <AuthPromptLink href="/login">{t("logIn")}</AuthPromptLink>
      </AuthPrompt>
    </div>
  )
}
