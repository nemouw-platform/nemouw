import { isApiError } from "@workspace/api"

// Facts about files to upload, without React: what kind a file is, whether
// it may be sent, and how to name sizes and types to people.

export const MB = 1024 * 1024

/** Photos the backend accepts (it stores them as WebP) */
export const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"] as const
/** Credential documents: a photo or scan, or a PDF */
export const DOCUMENT_TYPES = [...IMAGE_TYPES, "application/pdf"] as const

export type FileKind =
  "image" | "pdf" | "document" | "spreadsheet" | "archive" | "other"

/** The kind of file a MIME type stands for, to pick its icon */
export function fileKind(mimeType: string): FileKind {
  const type = mimeType.toLowerCase()
  if (type.startsWith("image/")) return "image"
  if (type === "application/pdf") return "pdf"
  if (/spreadsheet|excel|csv/.test(type)) return "spreadsheet"
  if (/word|opendocument\.text|rtf|^text\//.test(type)) return "document"
  if (/zip|rar|7z|tar|gzip/.test(type)) return "archive"
  return "other"
}

/** Why a file cannot be uploaded */
export type FileProblem =
  /** Not one of the accepted types */
  | "type"
  /** Over the size limit */
  | "size"
  /** Empty, or the server could not read it */
  | "unreadable"
  /** The server is processing too many files right now */
  | "busy"

export interface FileRules {
  accept: readonly string[]
  maxBytes: number
}

/** Checked before sending, so a wrong file never costs an upload */
export function checkFile(
  file: Pick<File, "type" | "size">,
  { accept, maxBytes }: FileRules
): FileProblem | null {
  if (!accept.includes(file.type)) return "type"
  if (file.size === 0) return "unreadable"
  if (file.size > maxBytes) return "size"
  return null
}

/** The server's answer to a refused upload, or null for any other failure */
export function uploadProblem(error: unknown): FileProblem | null {
  if (!isApiError(error)) return null
  switch (error.code) {
    case "FILE_TOO_LARGE":
      return "size"
    case "UNSUPPORTED_FILE_TYPE":
      return "type"
    case "MEDIA_BUSY":
      return "busy"
    case "VALIDATION_ERROR":
      // Missing or unreadable file; other fields are the form's business
      return error.fieldErrors.some(({ field }) =>
        ["file", "document"].includes(field)
      )
        ? "unreadable"
        : null
    default:
      return null
  }
}

/** "2 MB", "350 kB", "45 byte": sizes as people read them, in the page's language */
export function formatBytes(bytes: number, locale: string): string {
  const [value, unit] =
    bytes >= MB
      ? [bytes / MB, "megabyte"]
      : bytes >= 1024
        ? [bytes / 1024, "kilobyte"]
        : [bytes, "byte"]
  return new Intl.NumberFormat(locale, {
    style: "unit",
    unit,
    maximumFractionDigits: value < 10 ? 1 : 0,
  }).format(value)
}

const TYPE_NAMES: Partial<Record<string, string>> = {
  "image/jpeg": "JPEG",
  "image/png": "PNG",
  "image/webp": "WebP",
  "application/pdf": "PDF",
}

/** The accepted types as a list people read: "JPEG, PNG or WebP" */
export function formatTypes(accept: readonly string[], locale: string): string {
  const names = accept.map((type) => TYPE_NAMES[type] ?? type)
  return new Intl.ListFormat(locale, { type: "disjunction" }).format(names)
}
