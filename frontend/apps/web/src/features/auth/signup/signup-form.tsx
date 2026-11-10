"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { FieldGroup } from "@workspace/ui/components/field"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { AuthPromptLink } from "@/features/auth/components/auth-prompt"
import { FormAlert } from "@/features/auth/components/form-alert"
import { FormEmailField } from "@/features/auth/components/email-field"
import { FormTextField } from "@/features/auth/components/form-field"
import { SubmitButton } from "@/features/auth/components/submit-button"
import {
  createSignupSchema,
  type SignupValues,
} from "@/features/auth/signup/signup-schema"
import { EMAIL_TAKEN, useSignup } from "@/features/auth/signup/use-signup"

export function SignupForm() {
  const t = useTranslations("Signup")
  const tErrors = useTranslations("Signup.errors")
  const tAuthErrors = useTranslations("Auth.errors")
  const tFields = useTranslations("Auth.fields")
  const schema = React.useMemo(
    () =>
      createSignupSchema(
        {
          firstName: t("firstName"),
          lastName: t("lastName"),
          email: tFields("email"),
        },
        tAuthErrors
      ),
    [t, tFields, tAuthErrors]
  )

  const form = useForm<SignupValues>({
    resolver: zodResolver(schema),
    defaultValues: { first_name: "", last_name: "", email: "" },
    mode: "onTouched",
  })
  const { submit, isPending, formError } = useSignup(form)
  const { errors } = form.formState

  return (
    <form noValidate onSubmit={submit}>
      <FieldGroup>
        {/* <div className="grid gap-4 sm:grid-cols-2"> */}
        <FormTextField
          id="first_name"
          required
          label={t("firstName")}
          autoComplete="given-name"
          placeholder={t("firstNamePlaceholder")}
          dir="auto"
          error={errors.first_name}
          {...form.register("first_name")}
        />
        <FormTextField
          id="last_name"
          required
          label={t("lastName")}
          autoComplete="family-name"
          placeholder={t("lastNamePlaceholder")}
          dir="auto"
          error={errors.last_name}
          {...form.register("last_name")}
        />
        {/* </div> */}

        <FormEmailField
          id="email"
          required
          error={errors.email}
          errorAction={
            errors.email?.type === EMAIL_TAKEN && (
              <AuthPromptLink href="/login">
                {tErrors("logInInstead")}
              </AuthPromptLink>
            )
          }
          {...form.register("email")}
        />

        <FormAlert>{formError}</FormAlert>

        <SubmitButton pending={isPending} pendingLabel={t("submitting")}>
          {t("submit")}
        </SubmitButton>
      </FieldGroup>
    </form>
  )
}
