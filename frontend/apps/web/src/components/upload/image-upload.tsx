"use client"

import { Button } from "@workspace/ui/components/button"
import { isAbortError } from "@workspace/ui/hooks/use-request"
import { cn } from "@workspace/ui/lib/utils"
import {
  ImageIcon,
  LoaderCircleIcon,
  Trash2Icon,
  UploadIcon,
  UserRoundIcon,
} from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import {
  useFilePicker,
  useObjectUrl,
  useUploadTexts,
} from "@/components/upload/file-picker"
import { IMAGE_TYPES, uploadProblem } from "@/components/upload/files"
import { generalErrorMessage } from "@/lib/forms/api-errors"

const SHAPES = {
  /** A round profile picture beside its buttons */
  avatar: {
    layout: "flex flex-wrap items-center gap-5",
    frame: "size-24 rounded-full",
    icon: UserRoundIcon,
  },
  /** A wide banner (8:3, as the backend crops it) above its buttons */
  cover: {
    layout: "flex flex-col gap-3",
    frame: "aspect-[8/3] w-full rounded-lg",
    icon: ImageIcon,
  },
} as const

interface ImageUploadProps {
  /** The saved image, if any */
  src: string | null | undefined
  alt: string
  shape: keyof typeof SHAPES
  maxBytes: number
  /** Sends the picked image; reject to show what went wrong */
  onUpload: (file: File) => Promise<unknown>
  onRemove: () => Promise<unknown>
}

/**
 * A photo that saves as soon as it is picked (or dropped on it). The picked
 * file shows at once, under a spinner, until the upload is done.
 */
export function ImageUpload({
  src,
  alt,
  shape,
  maxBytes,
  onUpload,
  onRemove,
}: ImageUploadProps) {
  const t = useTranslations("Upload")
  const tFormErrors = useTranslations("Form.errors")
  const hintId = React.useId()
  const [pending, setPending] = React.useState<File | null>(null)
  const [busy, setBusy] = React.useState(false)
  const [message, setMessage] = React.useState<string | null>(null)
  const preview = useObjectUrl(pending)

  const rules = { accept: IMAGE_TYPES, maxBytes }
  const texts = useUploadTexts(rules)

  async function run(action: () => Promise<unknown>) {
    setMessage(null)
    setBusy(true)
    try {
      await action()
    } catch (error) {
      if (isAbortError(error)) return
      const problem = uploadProblem(error)
      setMessage(
        problem
          ? texts.problem(problem)
          : generalErrorMessage(error, tFormErrors)
      )
    } finally {
      setPending(null)
      setBusy(false)
    }
  }

  const picker = useFilePicker({
    rules,
    onPick: (file) => {
      setPending(file)
      void run(() => onUpload(file))
    },
    onProblem: (problem) => setMessage(texts.problem(problem)),
  })

  const { layout, frame, icon: Placeholder } = SHAPES[shape]
  const shown = preview ?? src

  return (
    <div className={layout}>
      <div
        {...(busy ? {} : picker.dropProps)}
        className={cn(
          "relative shrink-0 overflow-hidden bg-muted ring-1 ring-foreground/10 data-dragging:ring-2 data-dragging:ring-primary",
          frame
        )}
      >
        {shown ? (
          // A stored image on the CDN, or a local preview (blob:)
          // eslint-disable-next-line @next/next/no-img-element
          <img src={shown} alt={alt} className="size-full object-cover" />
        ) : (
          <Placeholder
            aria-hidden
            className="absolute inset-0 m-auto size-1/3 max-h-12 max-w-12 text-muted-foreground"
          />
        )}
        {busy && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/60">
            <LoaderCircleIcon
              role="status"
              aria-label={t("saving")}
              className="size-6 animate-spin text-primary"
            />
          </div>
        )}
      </div>

      <div className="flex min-w-0 flex-col gap-2">
        <input {...picker.inputProps} tabIndex={-1} aria-hidden />
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={busy}
            onClick={picker.open}
            aria-describedby={hintId}
          >
            <UploadIcon data-icon="inline-start" />
            {src ? t("changePhoto") : t("choosePhoto")}
          </Button>
          {src && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => void run(onRemove)}
            >
              <Trash2Icon data-icon="inline-start" />
              {t("remove")}
            </Button>
          )}
        </div>
        <p id={hintId} className="text-xs text-muted-foreground">
          {texts.hint}
        </p>
        {message && (
          <p role="alert" className="text-sm text-destructive">
            {message}
          </p>
        )}
      </div>
    </div>
  )
}
