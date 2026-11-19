"use client"

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
          className="flex items-center gap-1.5 text-sm text-muted-foreground"
        >
          <CircleCheckIcon aria-hidden className="size-4 text-primary" />
          {savedMessage}
        </p>
      )}
      {submit}
    </>
  )

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
        {/* One row, as on the page cards (the dialog's own footer stacks on phones) */}
        <DialogFooter className="m-0 flex-row flex-wrap items-center justify-end px-5 py-4">
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
        <CardFooter className="justify-end gap-3 border-t py-4">
          {actions}
        </CardFooter>
      </form>
    </Card>
  )
}
