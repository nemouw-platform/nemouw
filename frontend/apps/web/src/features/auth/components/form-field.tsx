import { Field, FieldError, FieldLabel } from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@workspace/ui/lib/utils"
import type { FieldError as FormFieldError } from "react-hook-form"

/** Accessibility props a control needs to point at its label and error */
export interface ControlProps {
  id: string
  "aria-invalid": true | undefined
  "aria-describedby": string | undefined
  "aria-required": true | undefined
}

export interface FormFieldProps {
  id: string
  label: string
  /** Marks the label with * and the control as required for assistive tech */
  required?: boolean
  error?: FormFieldError
  /** Extra content after the error, e.g. a "Log in instead" link */
  errorAction?: React.ReactNode
  /** Content under the control when there is no error, e.g. a hint */
  description?: React.ReactNode
  /** Shown at the end of the label row, e.g. a "Forgot password?" link */
  labelAction?: React.ReactNode
  /** Renders the control with the ids and aria state wired up */
  children: (control: ControlProps) => React.ReactNode
  /** Classes for the field wrapper, e.g. `w-fit` to hug a compact control */
  className?: string
}

/** Label, any control, and its error, for one react-hook-form field */
export function FormField({
  id,
  label,
  required,
  error,
  errorAction,
  description,
  labelAction,
  children,
  className,
}: FormFieldProps) {
  const errorId = `${id}-error`
  const descriptionId = `${id}-description`
  const invalid = Boolean(error)
  // Screen readers announce the error first, then the hint
  const describedBy =
    [invalid && errorId, description && descriptionId]
      .filter(Boolean)
      .join(" ") || undefined

  const fieldLabel = (
    <FieldLabel htmlFor={id}>
      {label}
      {/* Visual only: aria-required already tells screen readers */}
      {required && (
        <span aria-hidden className="-ms-1.5 text-destructive">
          *
        </span>
      )}
    </FieldLabel>
  )

  return (
    <Field data-invalid={invalid || undefined} className={className}>
      {labelAction ? (
        <div className="flex items-center justify-between gap-2">
          {fieldLabel}
          <span className="text-sm">{labelAction}</span>
        </div>
      ) : (
        fieldLabel
      )}
      {children({
        id,
        "aria-invalid": invalid || undefined,
        "aria-describedby": describedBy,
        "aria-required": required || undefined,
      })}
      {invalid && (
        <FieldError id={errorId}>
          {error?.message}
          {errorAction && <> {errorAction}</>}
        </FieldError>
      )}
      {/* A div, not a <p>: hints may be lists (e.g. password rules) */}
      {description && (
        <div id={descriptionId} className="text-sm text-muted-foreground">
          {description}
        </div>
      )}
    </Field>
  )
}

/** FormField's own props, for wrappers that pair it with one input */
export type FieldOwnProps = Omit<FormFieldProps, "children" | "className">

const FIELD_OWN_KEYS = [
  "id",
  "label",
  "required",
  "error",
  "errorAction",
  "description",
  "labelAction",
] as const satisfies ReadonlyArray<keyof FieldOwnProps>

/**
 * Splits a wrapper's props into FormField's and the input's, so wrappers such
 * as FormTextField never list (and forget) FormField props one by one.
 */
export function splitFieldProps<T extends FieldOwnProps>(
  props: T
): [FieldOwnProps, Omit<T, keyof FieldOwnProps>] {
  const field: Record<string, unknown> = {}
  const rest: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(props)) {
    const isFieldKey = (FIELD_OWN_KEYS as readonly string[]).includes(key)
    ;(isFieldKey ? field : rest)[key] = value
  }
  return [field as FieldOwnProps, rest as Omit<T, keyof FieldOwnProps>]
}

/** FormField with a text input; `className` styles the input, like on <Input> */
export function FormTextField(
  props: FieldOwnProps & Omit<React.ComponentProps<typeof Input>, "id">
) {
  const [field, { className, ...inputProps }] = splitFieldProps(props)
  return (
    <FormField {...field}>
      {(control) => (
        <Input
          {...control}
          // White like the cards, so fields stand out on the lavender page
          className={cn("h-10 bg-card", className)}
          {...inputProps}
        />
      )}
    </FormField>
  )
}
