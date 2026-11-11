import { useTranslations } from "next-intl"

import {
  FormTextField,
  type FieldOwnProps,
} from "@/features/auth/components/form-field"
import type { Input } from "@workspace/ui/components/input"

type EmailFieldProps = Omit<FieldOwnProps, "label"> &
  Omit<React.ComponentProps<typeof Input>, "id" | "type"> & {
    /** Defaults to the shared "Email address" label */
    label?: string
  }

/**
 * The email field every auth form uses: email keyboard, left-to-right even on
 * Arabic pages, shared label and placeholder. Pass `autoComplete="username"`
 * where a password manager should fill a saved login (the login form).
 */
export function FormEmailField({ label, ...props }: EmailFieldProps) {
  const t = useTranslations("Auth.fields")

  return (
    <FormTextField
      label={label ?? t("email")}
      type="email"
      inputMode="email"
      autoComplete="email"
      dir="ltr"
      placeholder={t("emailPlaceholder")}
      {...props}
    />
  )
}
