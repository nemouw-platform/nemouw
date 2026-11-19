"use client"

import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
} from "@workspace/ui/components/card"
import {
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@workspace/ui/components/dialog"
import { cn } from "@workspace/ui/lib/utils"
import { CircleCheckIcon } from "lucide-react"
import * as React from "react"

import { FormAlert } from "@/components/form/form-alert"
import { CardHeading } from "@/features/dashboard/card-heading"

/** Where a FormCard is shown: on the page (a card) or in a FormDialog */
const FrameContext = React.createContext<"card" | "dialog">("card")

/** Set by FormDialog, so the same form fits either place unchanged */
export const FormFrame = FrameContext.Provider

interface FormCardProps {
  title: React.ReactNode
  description?: React.ReactNode
  onSubmit: React.FormEventHandler<HTMLFormElement>
  /** The fields */
  children: React.ReactNode
  /** The save button */
  submit: React.ReactNode
  /** A button that closes the form without saving, before the save button */
  cancel?: { label: string; onClick: () => void }
  /** A failure no field explains */
  formError?: React.ReactNode
  /** Shown beside the button after a successful save, e.g. "Saved" */
  savedMessage?: React.ReactNode
  className?: string
}

/**
 * One dashboard form: what it is for, its fields, then the save button with
 * the outcome of the last save. A card on the page, or the content of a
 * FormDialog (title and buttons pinned, the fields scrolling between them).
 */
export function FormCard({
  title,
  description,
  onSubmit,
  children,
  submit,
  cancel,
  formError,
  savedMessage,
  className,
}: FormCardProps) {
  const frame = React.useContext(FrameContext)
  const fields = (
    <>
      {children}
      <FormAlert>{formError}</FormAlert>
    </>
  )
  const actions = (
    <>
      {savedMessage && (
        <p
          role="status"
          // Its own line above the buttons on phones, beside them otherwise
          className="flex basis-full items-center justify-center gap-1.5 text-sm text-muted-foreground sm:basis-auto"
        >
          <CircleCheckIcon aria-hidden className="size-4 text-primary" />
          {savedMessage}
        </p>
      )}
      {cancel && (
        <Button type="button" variant="outline" onClick={cancel.onClick}>
          {cancel.label}
        </Button>
      )}
      {submit}
    </>
  )
  /**
   * Phones: the buttons share one centred row, equally wide. Wider screens:
   * at the end, as wide as their labels.
   */
  const footer =
    "flex-row flex-wrap items-center justify-center gap-2 *:data-[slot=button]:flex-1 sm:justify-end sm:*:data-[slot=button]:flex-none"

  if (frame === "dialog") {
    return (
      <form
        noValidate
        onSubmit={onSubmit}
        className={cn("flex min-h-0 flex-1 flex-col", className)}
      >
        <DialogHeader className="border-b p-5 pe-12">
          <DialogTitle>{title}</DialogTitle>
          {description && <DialogDescription>{description}</DialogDescription>}
        </DialogHeader>
        <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto p-5">
          {fields}
        </div>
        <DialogFooter className={cn("m-0 px-5 py-4", footer)}>
          {actions}
        </DialogFooter>
      </form>
    )
  }

  return (
    <Card className={cn("[--card-spacing:--spacing(6)]", className)}>
      <form noValidate onSubmit={onSubmit} className="contents">
        <CardHeader>
          <CardHeading>{title}</CardHeading>
          {description && <CardDescription>{description}</CardDescription>}
        </CardHeader>
        <CardContent className="flex flex-col gap-5">{fields}</CardContent>
        <CardFooter className={cn("border-t py-4", footer)}>
          {actions}
        </CardFooter>
      </form>
    </Card>
  )
}
