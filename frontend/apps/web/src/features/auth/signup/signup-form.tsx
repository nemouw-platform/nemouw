"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Alert, AlertDescription } from "@workspace/ui/components/alert"
import { Button } from "@workspace/ui/components/button"
import { FieldGroup } from "@workspace/ui/components/field"
import { CircleAlertIcon, LoaderCircleIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormTextField } from "@/features/auth/components/form-text-field"
import {
  createSignupSchema,
  type SignupValues,
} from "@/features/auth/signup/signup-schema"
import { EMAIL_TAKEN, useSignup } from "@/features/auth/signup/use-signup"
import { Link } from "@/i18n/navigation"

export function SignupForm() {
  const t = useTranslations("Signup")
  const tErrors = useTranslations("Signup.errors")
  const schema = React.useMemo(() => createSignupSchema(tErrors), [tErrors])

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
            label={t("firstName")}
            autoComplete="given-name"
            placeholder={t("firstNamePlaceholder")}
            dir="auto"
            error={errors.first_name}
            {...form.register("first_name")}
          />
          <FormTextField
            id="last_name"
            label={t("lastName")}
            autoComplete="family-name"
            placeholder={t("lastNamePlaceholder")}
            dir="auto"
            error={errors.last_name}
            {...form.register("last_name")}
          />
        {/* </div> */}

        <FormTextField
          id="email"
          label={t("email")}
          type="email"
          inputMode="email"
          autoComplete="email"
          // Email addresses read left to right, even on Arabic pages
          dir="ltr"
          placeholder={t("emailPlaceholder")}
          error={errors.email}
          errorAction={
            errors.email?.type === EMAIL_TAKEN && (
              <Link
                href="/login"
                className="font-medium underline underline-offset-4"
              >
                {tErrors("logInInstead")}
              </Link>
            )
          }
          {...form.register("email")}
        />

        {formError && (
          <Alert variant="destructive">
            <CircleAlertIcon />
            <AlertDescription>{formError}</AlertDescription>
          </Alert>
        )}

        <Button type="submit" size="lg" className="w-full" disabled={isPending}>
          {isPending && (
            <LoaderCircleIcon
              className="animate-spin"
              data-icon="inline-start"
            />
          )}
          {isPending ? t("submitting") : t("submit")}
        </Button>
      </FieldGroup>
    </form>
  )
}
