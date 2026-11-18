"use client"

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select"

import type { ControlProps } from "@/components/form/form-field"

export interface SelectOption {
  value: string
  label: string
}

interface SelectControlProps extends ControlProps {
  options: readonly SelectOption[]
  /** "" while nothing is picked */
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  ref?: React.Ref<HTMLButtonElement>
  placeholder: string
  /**
   * For optional fields: an extra first option that clears the pick (value
   * ""), e.g. "Prefer not to say". Without it a pick cannot be undone.
   */
  noneLabel?: string
  disabled?: boolean
}

/**
 * A select for a form field (FormField's control): full width, white like the
 * text fields, with "" standing for "nothing picked" as react-hook-form expects.
 */
export function SelectControl({
  options,
  value,
  onChange,
  onBlur,
  ref,
  placeholder,
  noneLabel,
  disabled,
  ...control
}: SelectControlProps) {
  // Base UI uses null for "no value"; the form uses ""
  const items = [
    ...(noneLabel ? [{ value: null, label: noneLabel }] : []),
    ...options,
  ]

  return (
    <Select
      items={items}
      value={value || null}
      onValueChange={(next) => onChange(next ?? "")}
      disabled={disabled}
    >
      <SelectTrigger
        {...control}
        ref={ref}
        onBlur={onBlur}
        // As tall as the text fields (the trigger's own size variant is h-8)
        className="w-full bg-card data-[size=default]:h-10"
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {items.map((item) => (
          <SelectItem key={item.value ?? ""} value={item.value}>
            {item.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
