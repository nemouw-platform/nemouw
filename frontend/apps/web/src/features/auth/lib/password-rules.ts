// Mirror of the backend's PasswordPolicy (user/identity/PasswordPolicy.java),
// so the form can check as the user types. The common-password list (100k
// entries) stays on the server; its 422 TOO_COMMON shows on the field.

export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 64

/** Words no password may contain, checked like the backend does */
const SERVICE_WORDS = ["nemouw"]
/** Email fragments shorter than this are ignored */
const MIN_PERSONAL_PART = 4

/** Length in characters as the backend counts them (code points, so emoji count once) */
export function passwordLength(password: string): number {
  return [...password].length
}

export function hasValidLength(password: string): boolean {
  const length = passwordLength(password)
  return length >= PASSWORD_MIN_LENGTH && length <= PASSWORD_MAX_LENGTH
}

/**
 * True when the password contains "nemouw" or the email's local part (4+
 * characters), in any of the backend's forms: lowercase, letters only, and
 * leet-decoded ("N3m0uw!" -> "nemouw").
 */
export function containsPersonalInfo(
  password: string,
  email?: string
): boolean {
  if (!password) return false
  const forms = normalizedForms(password)
  return personalWords(email).some((word) =>
    forms.some((form) => form.includes(word))
  )
}

function normalizedForms(password: string): string[] {
  const lower = password.normalize("NFC").toLowerCase()
  const lettersOnly = lower.replace(/\P{L}/gu, "")
  const leetDecoded = lower
    .replace(/@/g, "a")
    .replace(/0/g, "o")
    .replace(/3/g, "e")
    .replace(/[$5]/g, "s")
    .replace(/7/g, "t")
    .replace(/1/g, "i")
    .replace(/\P{L}/gu, "")
  return [lower, lettersOnly, leetDecoded].filter(Boolean)
}

function personalWords(email?: string): string[] {
  const words = new Set(SERVICE_WORDS)
  const localPart = email?.split("@")[0]?.trim().normalize("NFC").toLowerCase()
  if (localPart) {
    const compact = localPart.replace(/[^\p{L}\p{N}]/gu, "")
    for (const word of [localPart, compact]) {
      if (word.length >= MIN_PERSONAL_PART) words.add(word)
    }
  }
  return [...words]
}

/**
 * Safari's `passwordrules` attribute: its password generator then suggests
 * only passwords our rules accept (https://developer.apple.com/password-rules/)
 */
export const PASSWORD_RULES_ATTRIBUTE = `minlength: ${PASSWORD_MIN_LENGTH}; maxlength: ${PASSWORD_MAX_LENGTH};`
