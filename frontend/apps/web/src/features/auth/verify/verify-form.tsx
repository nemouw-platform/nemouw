"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { Controller, useForm } from "react-hook-form"

import {
  AuthPrompt,
  AuthPromptLink,
} from "@/features/auth/components/auth-prompt"
import { CodeInput } from "@/features/auth/components/code-input"
import { FormAlert } from "@/features/auth/components/form-alert"
import { FormField } from "@/features/auth/components/form-field"
import { ResendPrompt } from "@/features/auth/components/resend-prompt"
import { SubmitButton } from "@/features/auth/components/submit-button"
import { OTP_LENGTH } from "@/features/auth/lib/constants"
import { richText } from "@/features/auth/lib/rich-text"
import { useResendCode } from "@/features/auth/verify/use-resend-code"
import { useVerifyCode } from "@/features/auth/verify/use-verify-code"
import {
  createVerifySchema,
  type VerifyValues,
} from "@/features/auth/verify/verify-schema"

export function VerifyForm({ email }: { email: string }) {
  const t = useTranslations("Verify")
  const tAuth = useTranslations("Auth")
  const tAuthErrors = useTranslations("Auth.errors")
  const schema = React.useMemo(
    () => createVerifySchema({ code: t("codeLabel") }, tAuthErrors),
    [t, tAuthErrors]
  )

  const form = useForm<VerifyValues>({
    resolver: zodResolver(schema),
    defaultValues: { code: "" },
  })
  const verify = useVerifyCode(email, form)
  const resend = useResendCode(email)
  const codeError = form.formState.errors.code

  return (
    <div className="flex flex-col gap-8">
      <form noValidate onSubmit={verify.submit}>
        <FieldGroup>
          <FormField
            id="code"
            required
            label={t("codeLabel")}
            error={codeError}
            description={tAuth("spamHint")}
            className="w-fit"
          >
            {(control) => (
              <Controller
                name="code"
                control={form.control}
                render={({ field }) => (
                  <CodeInput
                    {...control}
                    length={OTP_LENGTH}
                    ref={field.ref}
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    onComplete={() => verify.submit()}
                    autoFocus
                  />
                )}
              />
            )}
          </FormField>

          <FormAlert>{verify.formError ?? resend.formError}</FormAlert>
          <FormAlert tone="success">
            {resend.resent && t.rich("resent", { email, ...richText })}
          </FormAlert>

          <SubmitButton
            pending={verify.isPending}
            pendingLabel={t("submitting")}
          >
            {t("submit")}
          </SubmitButton>
        </FieldGroup>
      </form>

      <div className="flex flex-col gap-2">
        <ResendPrompt
          prompt={t("resendPrompt")}
          action={t("resend")}
          pendingLabel={t("resending")}
          waitingLabel={(time) => t("resendIn", { time })}
          onResend={resend.resend}
          pending={resend.isPending}
          secondsLeft={resend.secondsLeft}
        />
        <AuthPrompt prompt={t("wrongEmail")}>
          <AuthPromptLink href="/signup">{t("changeEmail")}</AuthPromptLink>
        </AuthPrompt>
      </div>
    </div>
  )
}
