"use client"

import { cn } from "@workspace/ui/lib/utils"
import type { LucideIcon } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import type { ControlProps } from "@/components/form/form-field"
import {
  checkFile,
  formatBytes,
  formatTypes,
  type FileProblem,
  type FileRules,
} from "@/components/upload/files"

/**
 * The upload limits in words ("JPEG, PNG or WebP", "2 MB") for hints, and
 * each problem's message, the same everywhere a file is picked
 */
export function useUploadTexts({ accept, maxBytes }: FileRules) {
  const t = useTranslations("Upload")
  const locale = useLocale()
  const limits = {
    types: formatTypes(accept, locale),
    size: formatBytes(maxBytes, locale),
  }
  return {
    hint: t("hint", limits),
    problem: (problem: FileProblem) => t(`problems.${problem}`, limits),
  }
}

interface FilePickerOptions {
  rules: FileRules
  /** A file that passed the rules */
  onPick: (file: File) => void
  /** A file that did not */
  onProblem: (problem: FileProblem) => void
}

/**
 * Choosing one file: by the browser's picker (`open`, or a label for the
 * input) or by dropping it on `dropProps`. Checks it against the rules first.
 */
export function useFilePicker({ rules, onPick, onProblem }: FilePickerOptions) {
  const inputRef = React.useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = React.useState(false)

  function take(files: FileList | null) {
    const file = files?.[0]
    if (!file) return
    const problem = checkFile(file, rules)
    if (problem) onProblem(problem)
    else onPick(file)
  }

  return {
    dragging,
    open: () => inputRef.current?.click(),
    inputProps: {
      ref: inputRef,
      type: "file",
      accept: rules.accept.join(","),
      className: "sr-only",
      onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
        take(event.target.files)
        // Picking the same file again should still count
        event.target.value = ""
      },
    },
    dropProps: {
      onDragOver: (event: React.DragEvent) => {
        event.preventDefault()
        setDragging(true)
      },
      onDragLeave: () => setDragging(false),
      onDrop: (event: React.DragEvent) => {
        event.preventDefault()
        setDragging(false)
        take(event.dataTransfer.files)
      },
      "data-dragging": dragging || undefined,
    },
  }
}

/**
 * A temporary URL showing a local file (a preview), released when done.
 * Made and released in one effect, so a re-run (Strict Mode) makes a new one
 * instead of keeping one it already released.
 */
export function useObjectUrl(file: Blob | null): string | null {
  const [url, setUrl] = React.useState<string | null>(null)
  React.useEffect(() => {
    if (!file) return
    const objectUrl = URL.createObjectURL(file)
    setUrl(objectUrl)
    return () => {
      URL.revokeObjectURL(objectUrl)
      setUrl(null)
    }
  }, [file])
  return url
}

interface DropZoneProps extends ControlProps, FilePickerOptions {
  icon: LucideIcon
  title: string
  hint: string
  className?: string
}

/**
 * An empty upload area: click (or Enter on the focused input) to browse,
 * or drop a file on it. The file input itself is the labelled control.
 */
export function DropZone({
  rules,
  onPick,
  onProblem,
  icon: Icon,
  title,
  hint,
  className,
  ...control
}: DropZoneProps) {
  const picker = useFilePicker({ rules, onPick, onProblem })

  return (
    <label
      htmlFor={control.id}
      {...picker.dropProps}
      className={cn(
        "flex cursor-pointer flex-col items-center gap-1.5 rounded-lg border border-dashed border-input bg-card px-4 py-6 text-center transition-colors hover:bg-muted/50",
        "has-[input:focus-visible]:border-ring has-[input:focus-visible]:ring-3 has-[input:focus-visible]:ring-ring/50",
        "has-[input[aria-invalid=true]]:border-destructive data-dragging:border-primary data-dragging:bg-primary/5",
        className
      )}
    >
      <input {...picker.inputProps} {...control} />
      <Icon aria-hidden className="mb-1 size-6 text-muted-foreground" />
      <span className="text-sm font-medium">{title}</span>
      <span className="text-xs text-muted-foreground">{hint}</span>
    </label>
  )
}
