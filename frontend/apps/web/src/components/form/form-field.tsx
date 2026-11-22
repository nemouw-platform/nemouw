import { Field, FieldError, FieldLabel } from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
  InputGroupText,
} from "@workspace/ui/components/input-group"
import { Textarea } from "@workspace/ui/components/textarea"
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
  /** Content under the control, e.g. a hint. An error takes its place */
  description?: React.ReactNode
  /**
   * Keep the description under the error, for one the error refers to (the
   * password checklist). The field then grows by the error's line.
   */
  keepDescription?: boolean
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
  keepDescription = false,
  labelAction,
  children,
  className,
}: FormFieldProps) {
  const errorId = `${id}-error`
  const descriptionId = `${id}-description`
  const invalid = Boolean(error)
  const showDescription = Boolean(description) && (!invalid || keepDescription)
  // Screen readers announce the error first, then the hint
  const describedBy =
    [invalid && errorId, showDescription && descriptionId]
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
      {/* One line is always kept under the control, and the error takes the
          hint's place, so an error appearing never moves the fields below */}
      <div className="flex min-h-5 flex-col gap-2">
        {invalid && (
          <FieldError id={errorId}>
            {error?.message}
            {errorAction && <> {errorAction}</>}
          </FieldError>
        )}
        {/* A div, not a <p>: hints may be lists (e.g. password rules) */}
        {showDescription && (
          <div id={descriptionId} className="text-sm text-muted-foreground">
            {description}
          </div>
        )}
      </div>
    </Field>
  )
}

/** FormField's own props, for wrappers that pair it with one input */
export type FieldOwnProps = Omit<FormFieldProps, "children" | "className">

/**
 * Every text box shows a placeholder (an example value or a short prompt),
 * so the field wrappers require one
 */
export interface PlaceholderProps {
  placeholder: string
}

const FIELD_OWN_KEYS = [
  "id",
  "label",
  "required",
  "error",
  "errorAction",
  "description",
  "keepDescription",
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
export function FormTextField({
  suffix,
  ...props
}: FieldOwnProps &
  PlaceholderProps &
  Omit<React.ComponentProps<typeof Input>, "id" | "placeholder"> & {
    /** Shown at the end of the box, e.g. a unit or currency ("USD") */
    suffix?: React.ReactNode
  }) {
  const [field, { className, ...inputProps }] = splitFieldProps(props)
  return (
    <FormField {...field}>
      {(control) =>
        suffix ? (
          // White like the cards, so fields stand out on the lavender page
          // A dir (e.g. "ltr" for amounts) covers the suffix too: "40 USD"
          <InputGroup
            dir={inputProps.dir}
            className={cn("h-10 bg-card", className)}
          >
            <InputGroupInput {...control} {...inputProps} />
            <InputGroupAddon align="inline-end">
              <InputGroupText>{suffix}</InputGroupText>
            </InputGroupAddon>
          </InputGroup>
        ) : (
          <Input
            {...control}
            className={cn("h-10 bg-card", className)}
            {...inputProps}
          />
        )
      }
    </FormField>
  )
}

/** FormField with a multi-line text box; `className` styles the textarea */
export function FormTextareaField(
  props: FieldOwnProps &
    PlaceholderProps &
    Omit<React.ComponentProps<typeof Textarea>, "id" | "placeholder">
) {
  const [field, { className, ...textareaProps }] = splitFieldProps(props)
  return (
    <FormField {...field}>
      {(control) => (
        <Textarea
          {...control}
          className={cn("min-h-32 bg-card", className)}
          {...textareaProps}
        />
      )}
    </FormField>
  )
}
