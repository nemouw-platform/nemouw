"use client"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@workspace/ui/components/alert-dialog"
import { Button } from "@workspace/ui/components/button"
import { LoaderCircleIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { FormAlert } from "@/components/form/form-alert"
import { generalErrorMessage } from "@/lib/forms/api-errors"

interface ConfirmButtonProps {
  /** The button's content, e.g. an icon and "Withdraw" */
  children: React.ReactNode
  title: string
  description: React.ReactNode
  /** The confirming button's label, e.g. "Withdraw" */
  confirmLabel: string
  cancelLabel: string
  /** Runs on confirm; the dialog stays open, showing the error, if it throws */
  onConfirm: () => Promise<unknown>
  /** Explains a failure the general messages do not cover, or returns null */
  errorMessage?: (error: unknown) => string | null
  disabled?: boolean
}

/**
 * A ghost button that asks before doing something that cannot be undone
 * (withdrawing, removing). The dialog closes once the action succeeds.
 */
export function ConfirmButton({
  children,
  title,
  description,
  confirmLabel,
  cancelLabel,
  onConfirm,
  errorMessage,
  disabled,
}: ConfirmButtonProps) {
  const tFormErrors = useTranslations("Form.errors")
  const [open, setOpen] = React.useState(false)
  const [pending, setPending] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  async function confirm() {
    setPending(true)
    setError(null)
    try {
      await onConfirm()
      setOpen(false)
    } catch (failure) {
      setError(
        errorMessage?.(failure) ?? generalErrorMessage(failure, tFormErrors)
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (pending) return
        setOpen(next)
        setError(null)
      }}
    >
      <AlertDialogTrigger
        render={<Button variant="ghost" size="sm" disabled={disabled} />}
      >
        {children}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <FormAlert>{error}</FormAlert>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>
            {cancelLabel}
          </AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={pending}
            onClick={() => void confirm()}
          >
            {pending && (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            )}
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
