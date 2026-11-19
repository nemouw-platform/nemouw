"use client"

import { Button } from "@workspace/ui/components/button"
import {
  ExternalLinkIcon,
  LoaderCircleIcon,
  UploadIcon,
  XIcon,
} from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import type { ControlProps } from "@/components/form/form-field"
import { FileTypeIcon } from "@/components/upload/file-icon"
import {
  DropZone,
  useFilePicker,
  useObjectUrl,
  useUploadTexts,
} from "@/components/upload/file-picker"
import {
  fileKind,
  formatBytes,
  type FileProblem,
} from "@/components/upload/files"

/** A file already saved on the server, shown until a new one is picked */
export interface StoredFile {
  mimeType: string
  /** What to call it, e.g. "PDF document" (the server keeps no file name) */
  label: string
  /** A second line, e.g. "Uploaded 3 Nov 2026" */
  detail?: string
  /** Opens it (e.g. a short-lived link); omit when it cannot be viewed */
  onView?: () => void
  /** The link for onView is on its way */
  viewing?: boolean
}

export interface FileUploadProps extends ControlProps {
  /** A newly picked file, or null */
  value: File | null
  onChange: (file: File | null) => void
  onBlur?: () => void
  /** The saved file this one would replace */
  current?: StoredFile | null
  accept: readonly string[]
  maxBytes: number
  disabled?: boolean
}

/**
 * One file for a form, sent with it on submit. Shows the picked file (a
 * thumbnail for images, an icon by type otherwise), else the saved one with
 * View and Replace, else a drop zone.
 */
export function FileUpload({
  value,
  onChange,
  onBlur,
  current,
  accept,
  maxBytes,
  disabled,
  ...control
}: FileUploadProps) {
  const t = useTranslations("Upload")
  const locale = useLocale()
  const [problem, setProblem] = React.useState<FileProblem | null>(null)
  const texts = useUploadTexts({ accept, maxBytes })
  const pickerOptions = {
    rules: { accept, maxBytes },
    onPick: (file: File) => {
      setProblem(null)
      onChange(file)
      onBlur?.()
    },
    onProblem: setProblem,
  }
  const picker = useFilePicker(pickerOptions)

  let body: React.ReactNode
  if (!value && !current) {
    body = (
      <DropZone
        {...control}
        {...pickerOptions}
        icon={UploadIcon}
        title={t("dropTitle")}
        hint={texts.hint}
      />
    )
  } else {
    // A file is shown: its first button is the labelled control
    const hiddenInput = (
      <input {...picker.inputProps} tabIndex={-1} aria-hidden />
    )
    body = value ? (
      <FileRow
        preview={<PickedPreview file={value} />}
        name={value.name}
        detail={formatBytes(value.size, locale)}
      >
        {hiddenInput}
        <Button
          {...control}
          // Named by its text, not by the field's label (which still clicks it)
          aria-label={t("change")}
          type="button"
          variant="outline"
          size="sm"
          disabled={disabled}
          onClick={picker.open}
        >
          <UploadIcon data-icon="inline-start" />
          {t("change")}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          disabled={disabled}
          aria-label={t("removeFile", { name: value.name })}
          onClick={() => onChange(null)}
        >
          <XIcon />
        </Button>
      </FileRow>
    ) : (
      <FileRow
        preview={<FileTypeIcon mimeType={current!.mimeType} />}
        name={current!.label}
        detail={current!.detail}
      >
        {hiddenInput}
        {current!.onView && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={current!.viewing}
            onClick={current!.onView}
          >
            {current!.viewing ? (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            ) : (
              <ExternalLinkIcon data-icon="inline-start" />
            )}
            {t("view")}
          </Button>
        )}
        <Button
          {...control}
          aria-label={t("replace")}
          type="button"
          variant="outline"
          size="sm"
          disabled={disabled}
          onClick={picker.open}
        >
          <UploadIcon data-icon="inline-start" />
          {t("replace")}
        </Button>
      </FileRow>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      {body}
      {problem && (
        <p role="alert" className="text-sm text-destructive">
          {texts.problem(problem)}
        </p>
      )}
    </div>
  )
}

/** A file as a row: its preview or icon, name and detail, then actions */
function FileRow({
  preview,
  name,
  detail,
  children,
}: {
  preview: React.ReactNode
  name: string
  detail?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg border bg-card p-2.5">
      {preview}
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{name}</span>
        {detail && (
          <span className="text-xs text-muted-foreground">{detail}</span>
        )}
      </div>
      <div className="flex shrink-0 items-center gap-1">{children}</div>
    </div>
  )
}

/** A picked image as a thumbnail; any other file as its type's icon */
function PickedPreview({ file }: { file: File }) {
  const isImage = fileKind(file.type) === "image"
  const url = useObjectUrl(isImage ? file : null)
  if (!url) return <FileTypeIcon mimeType={file.type} />
  return (
    // A local preview (blob:), not something next/image can optimize
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt=""
      className="size-10 shrink-0 rounded-md object-cover ring-1 ring-foreground/10"
    />
  )
}
