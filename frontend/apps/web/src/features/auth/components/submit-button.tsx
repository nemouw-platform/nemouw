"use client"

import { Button } from "@workspace/ui/components/button"
import { LoaderCircleIcon } from "lucide-react"

interface SubmitButtonProps {
  pending: boolean
  children: React.ReactNode
  /** Label while the request runs, e.g. "Sending code…" */
  pendingLabel: React.ReactNode
}

/** Full-width primary submit button with a spinner while the request runs */
export function SubmitButton({
  pending,
  children,
  pendingLabel,
}: SubmitButtonProps) {
  return (
    <Button
      type="submit"
      size="lg"
      className="w-full"
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
