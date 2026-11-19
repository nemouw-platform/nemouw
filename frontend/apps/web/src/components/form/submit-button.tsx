"use client"

import { Button } from "@workspace/ui/components/button"
import { cn } from "@workspace/ui/lib/utils"
import { LoaderCircleIcon } from "lucide-react"

interface SubmitButtonProps {
  pending: boolean
  children: React.ReactNode
  /** Label while the request runs, e.g. "Sending code…" */
  pendingLabel: React.ReactNode
  /** "lg" fills the form's width (auth screens); "default" hugs its label (cards) */
  size?: "lg" | "default"
  className?: string
}

/** Primary submit button with a spinner while the request runs */
export function SubmitButton({
  pending,
  children,
  pendingLabel,
  size = "lg",
  className,
}: SubmitButtonProps) {
  return (
    <Button
      type="submit"
      size={size}
      className={cn(size === "lg" && "w-full", className)}
      disabled={pending}
      // Keep focus in the field on press. Otherwise the field's blur shows
      // its error, the button moves down, and the release misses it: no click
      onMouseDown={(event) => event.preventDefault()}
    >
      {pending && (
        <LoaderCircleIcon className="animate-spin" data-icon="inline-start" />
      )}
      {pending ? pendingLabel : children}
    </Button>
  )
}
