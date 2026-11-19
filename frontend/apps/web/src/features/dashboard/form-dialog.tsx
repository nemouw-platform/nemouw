"use client"

import { Dialog, DialogContent } from "@workspace/ui/components/dialog"
import { useTranslations } from "next-intl"

import { FormFrame } from "@/features/dashboard/form-card"

interface FormDialogProps {
  open: boolean
  /** Called with false on Escape or the close button */
  onClose: () => void
  /** One form built on FormCard, which then lays itself out for the dialog */
  children: React.ReactNode
}

/**
 * A dashboard form in a popup, e.g. adding a credential. A click outside
 * does not close it, so a half-filled form is never lost by accident.
 */
export function FormDialog({ open, onClose, children }: FormDialogProps) {
  const t = useTranslations("Common")

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => !next && onClose()}
      disablePointerDismissal
    >
      <DialogContent
        closeLabel={t("close")}
        // Wide enough for two columns of fields; the body scrolls between
        // the pinned title and buttons on short screens
        className="flex max-h-[calc(100svh-2rem)] flex-col gap-0 p-0 sm:max-w-2xl"
      >
        <FormFrame value="dialog">{children}</FormFrame>
      </DialogContent>
    </Dialog>
  )
}
