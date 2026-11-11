"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { AuthPromptLink } from "@/features/auth/components/auth-prompt"
import { FormAlert } from "@/features/auth/components/form-alert"
import { NewPasswordField } from "@/features/auth/components/new-password-field"
import { SubmitButton } from "@/features/auth/components/submit-button"
import {
  createPasswordSchema,
  type PasswordValues,
} from "@/features/auth/password/password-schema"
import { useSetupPassword } from "@/features/auth/password/use-setup-password"

export function PasswordForm({ email }: { email?: string }) {
  const t = useTranslations("Password")
  const tAuthErrors = useTranslations("Auth.errors")
  const tFields = useTranslations("Auth.fields")
  const schema = React.useMemo(
    () =>
      createPasswordSchema(
        { password: tFields("password") },
        tAuthErrors,
        email
      ),
    [tFields, tAuthErrors, email]
  )

  const form = useForm<PasswordValues>({
    resolver: zodResolver(schema),
    defaultValues: { password: "" },
    mode: "onTouched",
  })
  const { submit, isPending, formError, setupExpired } = useSetupPassword(form)

  return (
    <form noValidate onSubmit={submit}>
      {/* For password managers: they save the new password under this
          username and offer to generate one more reliably when it is present */}
      {email && (
        <input
          type="email"
          name="username"
          autoComplete="username"
          value={email}
          readOnly
          hidden
        />
      )}
      <FieldGroup>
        <NewPasswordField form={form} name="password" email={email} autoFocus />

        <FormAlert>
          {setupExpired ? (
            <>
              {t("errors.setupExpired")}{" "}
              <AuthPromptLink
                href={
                  email
                    ? { pathname: "/signup/verify", query: { email } }
                    : "/signup"
                }
              >
                {t("errors.getNewCode")}
              </AuthPromptLink>
            </>
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
