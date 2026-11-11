"use client"

import { useTranslations } from "next-intl"
import {
  useWatch,
  type FieldPath,
  type FieldValues,
  type UseFormReturn,
} from "react-hook-form"

import { FormPasswordField } from "@/features/auth/components/password-input"
import { PasswordRequirements } from "@/features/auth/components/password-requirements"
import { PASSWORD_RULES_ATTRIBUTE } from "@/features/auth/lib/password-rules"

interface NewPasswordFieldProps<T extends FieldValues> {
  form: UseFormReturn<T>
  /** Form field holding the password, e.g. "password" or "new_password" */
  name: FieldPath<T>
  /** When known, the checklist also flags passwords that contain it */
  email?: string
  autoFocus?: boolean
}

/**
 * Field for choosing a password: the live checklist under it, and the hints
 * browsers need to offer "Suggest strong password" (new-password, plus Safari's
 * passwordrules so its suggestion fits our 8-64 characters).
 */
export function NewPasswordField<T extends FieldValues>({
  form,
  name,
  email,
  autoFocus,
}: NewPasswordFieldProps<T>) {
  const t = useTranslations("Auth.fields")
  const value = useWatch({ control: form.control, name })

  return (
    <FormPasswordField
      id={name}
      required
      label={t("password")}
      autoComplete="new-password"
      passwordrules={PASSWORD_RULES_ATTRIBUTE}
      autoFocus={autoFocus}
      error={form.getFieldState(name, form.formState).error}
      description={
        <PasswordRequirements password={String(value ?? "")} email={email} />
      }
      {...form.register(name)}
    />
  )
}
