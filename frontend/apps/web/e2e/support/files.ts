import type { Page } from "@playwright/test"

/** A real PNG to upload: a screenshot of part of the page */
export async function pngFile(page: Page, name = "photo.png") {
  const buffer = await page.screenshot({
    clip: { x: 0, y: 0, width: 240, height: 240 },
  })
  return { name, mimeType: "image/png", buffer }
}

/** The smallest PDF the backend accepts (a header and the end marker) */
export function pdfFile(name = "licence.pdf") {
  const buffer = Buffer.from(
    "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n",
    "latin1"
  )
  return { name, mimeType: "application/pdf", buffer }
}

/** A file of a type no upload accepts */
export function textFile(name = "notes.txt") {
  return { name, mimeType: "text/plain", buffer: Buffer.from("hello") }
}
