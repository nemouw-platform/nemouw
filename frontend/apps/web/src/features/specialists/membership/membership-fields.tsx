"use client"

import { useTranslations } from "next-intl"
import type {
  Control,
  FieldError,
  FieldValues,
  Path,
  UseFormRegisterReturn,
} from "react-hook-form"

import { FormControllerField } from "@/components/form/form-controller-field"
import { FormTextField } from "@/components/form/form-field"
import { AuthPromptButton } from "@/features/auth/components/auth-prompt"
import { SyndicateSelect } from "@/features/specialists/membership/syndicate-select"
import { useSyndicates } from "@/features/specialists/membership/use-syndicates"

interface TextFieldProps {
  /** form.register(...) for this field */
  registration: UseFormRegisterReturn
  error?: FieldError
  autoFocus?: boolean
  disabled?: boolean
}

/** The public username: Latin, as it becomes the profile's link */
export function UsernameField({ registration, ...props }: TextFieldProps) {
  const t = useTranslations("Membership")
  return (
    <FormTextField
      id={registration.name}
      required
      label={t("username")}
      placeholder={t("usernamePlaceholder")}
      description={t("usernameHint")}
      autoComplete="off"
      autoCapitalize="none"
      spellCheck={false}
      // Usernames are Latin characters, even on Arabic pages
      dir="ltr"
      {...props}
      {...registration}
    />
  )
}

/** The member number in the chosen syndicate */
export function MemberNumberField({ registration, ...props }: TextFieldProps) {
  const t = useTranslations("Membership")
  return (
    <FormTextField
      id={registration.name}
      required
      label={t("memberNumber")}
      placeholder={t("memberNumberPlaceholder")}
      description={t("memberNumberHint")}
      autoComplete="off"
      dir="ltr"
      {...props}
      {...registration}
    />
  )
}

interface SyndicateFieldProps<TValues extends FieldValues> {
  name: Path<TValues>
  control: Control<TValues>
  error?: FieldError
  disabled?: boolean
  /** Shown under the picker instead of the list's status, e.g. why it is locked */
  description?: React.ReactNode
}

/** The syndicate picker, with the public list loaded and its status */
export function SyndicateField<TValues extends FieldValues>({
  name,
  control,
  error,
  disabled,
  description,
}: SyndicateFieldProps<TValues>) {
  const t = useTranslations("Membership")
  const syndicates = useSyndicates()

  return (
    <FormControllerField
      name={name}
      control={control}
      required
      label={t("syndicate")}
      error={error}
      description={description ?? <SyndicatesStatus {...syndicates} />}
      render={({ value, onChange, onBlur, ref, ...controlProps }) => (
        <SyndicateSelect
          {...controlProps}
          syndicates={syndicates.syndicates}
          isLoading={syndicates.isLoading}
          disabled={disabled}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          ref={ref}
        />
      )}
    />
  )
}

/** Only says something when the list could not load or is empty */
function SyndicatesStatus({
  syndicates,
  isLoading,
  failed,
  retry,
}: ReturnType<typeof useSyndicates>) {
  const t = useTranslations("Membership")

  if (failed) {
    return (
      <>
        {t("syndicatesFailed")}{" "}
        <AuthPromptButton onClick={retry}>{t("retry")}</AuthPromptButton>
      </>
    )
  }
  if (!isLoading && syndicates.length === 0) return t("syndicatesEmpty")
  return null
}
