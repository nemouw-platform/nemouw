"use client"

import { Button } from "@workspace/ui/components/button"
import { LoaderCircleIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { generalErrorMessage } from "@/lib/forms/api-errors"

type ActionButtonProps = Omit<
  React.ComponentProps<typeof Button>,
  "onClick"
> & {
  /** Runs on click; the button shows a spinner until it settles */
  onAction: () => Promise<unknown>
  /** Called with a message to show when it fails (null when it starts again) */
  onFailure: (message: string | null) => void
  /** Shown before the label while idle */
  icon?: React.ReactNode
}

/** A button that does one thing at once (verify, lift a suspension) */
export function ActionButton({
  onAction,
  onFailure,
  icon,
  children,
  disabled,
  ...props
}: ActionButtonProps) {
  const tFormErrors = useTranslations("Form.errors")
  const [pending, setPending] = React.useState(false)

  async function run() {
    setPending(true)
    onFailure(null)
    try {
      await onAction()
    } catch (error) {
      onFailure(generalErrorMessage(error, tFormErrors))
    } finally {
      setPending(false)
    }
  }

  return (
    <Button
      {...props}
      disabled={disabled || pending}
      onClick={() => void run()}
    >
      {pending ? (
        <LoaderCircleIcon className="animate-spin" data-icon="inline-start" />
      ) : (
        icon
      )}
      {children}
    </Button>
  )
}
