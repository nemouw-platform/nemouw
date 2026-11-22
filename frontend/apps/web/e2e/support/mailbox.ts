import { readdir, readFile } from "node:fs/promises"
import path from "node:path"
import { fileURLToPath } from "node:url"

/**
 * With MAIL_DELIVERY=file the backend writes each email as an .eml file
 * instead of sending it (backend/target/dev-mails by default).
 */
const MAIL_DIR =
  process.env.E2E_MAIL_DIR ??
  fileURLToPath(
    new URL("../../../../../backend/target/dev-mails", import.meta.url)
  )

const TOKEN_LINK = /reset-password#token=([A-Za-z0-9_%.-]+)/

/** The reset token from the newest email to this address (waits for it) */
export async function readResetToken(email: string, timeoutMs = 10_000) {
  const fileTag = email.replace(/[^a-zA-Z0-9.-]/g, "_")
  const deadline = Date.now() + timeoutMs

  while (Date.now() < deadline) {
    const files = (await readdir(MAIL_DIR)).filter((f) => f.includes(fileTag))
    const newest = files.sort().at(-1)
    if (newest) {
      const token = findToken(
        await readFile(path.join(MAIL_DIR, newest), "utf8")
      )
      if (token) return token
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error(`No reset email for ${email} in ${MAIL_DIR}`)
}

/** Parts of an .eml may be base64: search the raw text and every decoded part */
function findToken(eml: string): string | null {
  const decodedParts = eml
    .split(/\r?\n\r?\n/)
    .map((part) =>
      Buffer.from(part.replace(/\s+/g, ""), "base64").toString("utf8")
    )
  for (const text of [eml, ...decodedParts]) {
    const match = text.match(TOKEN_LINK)
    if (match?.[1]) return decodeURIComponent(match[1])
  }
  return null
}
