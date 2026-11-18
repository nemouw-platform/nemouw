"use client"

import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@workspace/ui/components/input-group"
import { cn } from "@workspace/ui/lib/utils"
import type { CountryCode } from "libphonenumber-js"
import * as React from "react"

import type { ControlProps } from "@/components/form/form-field"
import { CountrySelect } from "@/components/form/phone-input/country-select"
import {
  examplePhone,
  formatNational,
  joinPhone,
  readTyped,
  splitPhone,
  type PhoneParts,
} from "@/components/form/phone-input/phone"
import { useVisitorCountry } from "@/lib/visitor-country/visitor-country-context"

export interface PhoneInputProps extends ControlProps {
  /** The international number ("+96170123456"), or "" when empty */
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  ref?: React.Ref<HTMLInputElement>
  /**
   * The country an empty field starts on. Default: the visitor's country,
   * found on the server from their IP (VisitorCountryProvider).
   */
  defaultCountry?: CountryCode
  disabled?: boolean
  className?: string
}

/**
 * A phone number field: a searchable country picker with flags, then the
 * national number. The value is always international, so it never depends on
 * the country the field happened to start on.
 */
export function PhoneInput({
  value,
  onChange,
  onBlur,
  ref,
  defaultCountry,
  disabled,
  className,
  ...control
}: PhoneInputProps) {
  const visitorCountry = useVisitorCountry()
  const fallback = defaultCountry ?? visitorCountry
  const [parts, setParts] = React.useState(() => splitPhone(value, fallback))
  // The value we last sent up; any other value comes from outside (a reset)
  const [sent, setSent] = React.useState(value)
  const inputRef = React.useRef<HTMLInputElement>(null)

  if (value !== sent) {
    setSent(value)
    setParts(splitPhone(value, fallback))
  }

  function update(next: PhoneParts) {
    const joined = joinPhone(next.national, next.country)
    setParts(next)
    setSent(joined)
    onChange(joined)
  }

  return (
    // Phone numbers read left to right, even on Arabic pages
    <InputGroup dir="ltr" className={cn("h-10 bg-card", className)}>
      <InputGroupAddon align="inline-start" className="ps-1">
        <CountrySelect
          value={parts.country}
          disabled={disabled}
          focusAfterPick={inputRef}
          onChange={(country) => update({ ...parts, country })}
        />
      </InputGroupAddon>
      <InputGroupInput
        {...control}
        ref={(node) => {
          inputRef.current = node
          if (typeof ref === "function") ref(node)
          else if (ref) ref.current = node
        }}
        type="tel"
        inputMode="tel"
        autoComplete="tel-national"
        placeholder={examplePhone(parts.country)}
        disabled={disabled}
        value={parts.national}
        onChange={(event) =>
          update(readTyped(event.target.value, parts.country))
        }
        onBlur={() => {
          // Tidy the spacing once typing is done (not while typing, which
          // would move the caret). From the latest state: a country picked
          // in the same tick may not have rendered yet
          setParts((current) => ({
            ...current,
            national: formatNational(current.national, current.country),
          }))
          onBlur?.()
        }}
      />
    </InputGroup>
  )
}
