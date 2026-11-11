"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { AuthPromptLink } from "@/features/auth/components/auth-prompt"
import { FormAlert } from "@/features/auth/components/form-alert"
import { FormEmailField } from "@/features/auth/components/email-field"
import { FormPasswordField } from "@/features/auth/components/password-input"
import { SubmitButton } from "@/features/auth/components/submit-button"
import {
  createLoginSchema,
  type LoginValues,
} from "@/features/auth/login/login-schema"
import { useLogin, type LoginNotice } from "@/features/auth/login/use-login"
import { useResumeSession } from "@/features/auth/login/use-resume-session"

export function LoginForm() {
  const t = useTranslations("Login")
  const tFields = useTranslations("Auth.fields")
  const tAuthErrors = useTranslations("Auth.errors")
  const schema = React.useMemo(
    () =>
      createLoginSchema(
        { email: tFields("email"), password: tFields("password") },
        tAuthErrors
      ),
    [tFields, tAuthErrors]
  )

  const form = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { identifier: "", password: "" },
    mode: "onTouched",
  })
  const { submit, isPending, formError, notice } = useLogin(form)
  useResumeSession()
  const { errors } = form.formState

  return (
    <form noValidate onSubmit={submit}>
      <FieldGroup>
        <FormEmailField
          id="identifier"
          required
          // username: password managers fill the saved login here
          autoComplete="username"
          error={errors.identifier}
          {...form.register("identifier")}
        />
        <FormPasswordField
          id="password"
          required
          label={tFields("password")}
          autoComplete="current-password"
          labelAction={
            <AuthPromptLink href="/forgot-password">
              {t("forgotPassword")}
            </AuthPromptLink>
          }
          error={errors.password}
          {...form.register("password")}
        />

        <FormAlert>
          {notice ? (
            <LoginNoticeMessage
              notice={notice}
              email={form.getValues("identifier")}
            />
          ) : (
            formError
          )}
        </FormAlert>

        <SubmitButton pending={isPending} pendingLabel={t("submitting")}>
          {t("submit")}
        </SubmitButton>
      </FieldGroup>
    </form>
  )
}

/** The message for each login refusal, with the link that helps */
function LoginNoticeMessage({
  notice,
  email,
}: {
  notice: LoginNotice
  email: string
}) {
  const t = useTranslations("Login.errors")

  switch (notice) {
    case "invalidCredentials":
      return (
        <>
          {t("invalidCredentials")}{" "}
          <AuthPromptLink href="/forgot-password">
            {t("resetPassword")}
          </AuthPromptLink>
        </>
      )
    case "notVerified":
      return (
        <>
          {t("notVerified")}{" "}
          <AuthPromptLink
            href={{ pathname: "/signup/verify", query: { email } }}
          >
            {t("verifyEmail")}
          </AuthPromptLink>
        </>
      )
    case "suspended":
      return t("suspended")
  }
}
