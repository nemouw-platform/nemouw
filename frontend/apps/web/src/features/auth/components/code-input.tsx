"use client"

import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@workspace/ui/components/input-otp"

import type { ControlProps } from "@/features/auth/components/form-field"

interface CodeInputProps extends ControlProps {
  length: number
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  /** Called once every box is filled, typed or pasted */
  onComplete?: (value: string) => void
  autoFocus?: boolean
  ref?: React.Ref<HTMLInputElement>
}

/** Digits only, matched against the whole value */
const DIGITS_PATTERN = "^\\d*$"

/**
 * One box per digit for emailed codes. Supports paste and the phone's
 * one-time-code autofill, and always reads left to right, even on Arabic pages.
 */
export function CodeInput({
  length,
  "aria-invalid": invalid,
  ...props
}: CodeInputProps) {
  return (
    <div dir="ltr" className="flex">
      <InputOTP
        maxLength={length}
        pattern={DIGITS_PATTERN}
        inputMode="numeric"
        autoComplete="one-time-code"
        aria-invalid={invalid}
        {...props}
      >
        <InputOTPGroup className="gap-2">
          {Array.from({ length }, (_, index) => (
            <InputOTPSlot
              key={index}
              index={index}
              aria-invalid={invalid}
              className="size-12 rounded-lg border bg-card text-lg font-semibold first:rounded-lg last:rounded-lg"
            />
          ))}
        </InputOTPGroup>
      </InputOTP>
    </div>
  )
}
