import { describe, expect, it } from "vitest"

import {
  checkFile,
  DOCUMENT_TYPES,
  fileKind,
  formatBytes,
  formatTypes,
  IMAGE_TYPES,
  MB,
  uploadProblem,
} from "@/components/upload/files"
import { apiError } from "@/lib/forms/testing"

describe("fileKind", () => {
  it.each([
    ["image/webp", "image"],
    ["application/pdf", "pdf"],
    [
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "document",
    ],
    ["text/plain", "document"],
    ["text/csv", "spreadsheet"],
    ["application/vnd.ms-excel", "spreadsheet"],
    ["application/zip", "archive"],
    ["application/octet-stream", "other"],
  ])("%s is %s", (type, kind) => {
    expect(fileKind(type)).toBe(kind)
  })
})

describe("checkFile", () => {
  const rules = { accept: IMAGE_TYPES, maxBytes: 2 * MB }

  it("accepts an image of an accepted type under the limit", () => {
    expect(checkFile({ type: "image/png", size: MB }, rules)).toBeNull()
  })

  it("names what is wrong", () => {
    expect(checkFile({ type: "application/pdf", size: 10 }, rules)).toBe("type")
    expect(checkFile({ type: "image/png", size: 3 * MB }, rules)).toBe("size")
    expect(checkFile({ type: "image/png", size: 0 }, rules)).toBe("unreadable")
  })
})

describe("uploadProblem", () => {
  it("maps the backend's upload refusals", () => {
    expect(uploadProblem(apiError(413, "FILE_TOO_LARGE"))).toBe("size")
    expect(uploadProblem(apiError(415, "UNSUPPORTED_FILE_TYPE"))).toBe("type")
    expect(uploadProblem(apiError(503, "MEDIA_BUSY"))).toBe("busy")
    expect(
      uploadProblem(
        apiError(422, "VALIDATION_ERROR", {
          errors: [{ field: "file", code: "INVALID_FORMAT", message: "x" }],
        })
      )
    ).toBe("unreadable")
  })

  it("leaves other failures to the caller", () => {
    expect(
      uploadProblem(
        apiError(422, "VALIDATION_ERROR", {
          errors: [{ field: "title", code: "REQUIRED", message: "x" }],
        })
      )
    ).toBeNull()
    expect(uploadProblem(apiError(500, "INTERNAL_ERROR"))).toBeNull()
    expect(uploadProblem(new Error("offline"))).toBeNull()
  })
})

describe("formatBytes / formatTypes", () => {
  it("writes sizes in the page's language", () => {
    expect(formatBytes(2 * MB, "en")).toBe("2 MB")
    expect(formatBytes(1.5 * MB, "en")).toBe("1.5 MB")
    expect(formatBytes(350 * 1024, "en")).toBe("350 kB")
    expect(formatBytes(45, "en")).toBe("45 byte")
  })

  it("lists the accepted types", () => {
    expect(formatTypes(IMAGE_TYPES, "en")).toBe("JPEG, PNG, or WebP")
    expect(formatTypes(DOCUMENT_TYPES, "fr")).toBe("JPEG, PNG, WebP ou PDF")
  })
})
