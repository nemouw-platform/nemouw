import { Field, FieldError, FieldLabel } from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@workspace/ui/lib/utils"
import type { FieldError as FormFieldError } from "react-hook-form"

type InputProps = React.ComponentProps<typeof Input>

interface FormTextFieldProps extends Omit<InputProps, "id"> {
  id: string
  label: string
  error?: FormFieldError
  /** Extra content under the error, e.g. a "Log in instead" link */
  errorAction?: React.ReactNode
}

/** Label, input and error for one react-hook-form field */
export function FormTextField({
  id,
  label,
  error,
  errorAction,
  className,
  ...inputProps
}: FormTextFieldProps) {
  const errorId = `${id}-error`
  const invalid = Boolean(error)

  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        aria-invalid={invalid || undefined}
        aria-describedby={invalid ? errorId : undefined}
        // White like the cards, so fields stand out on the lavender page
        className={cn("h-10 bg-card", className)}
        {...inputProps}
      />
      {invalid && (
        <FieldError id={errorId}>
          {error?.message}
          {errorAction && <> {errorAction}</>}
        </FieldError>
      )}
    </Field>
  )
}
