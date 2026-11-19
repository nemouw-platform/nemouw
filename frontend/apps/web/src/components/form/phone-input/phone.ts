import {
  AsYouType,
  getCountryCallingCode,
  getExampleNumber,
  isValidPhoneNumber,
  parsePhoneNumberFromString,
  type CountryCode,
} from "libphonenumber-js"
import examples from "libphonenumber-js/examples.mobile.json"

// The form's value is the international number ("+96170123456"), or "" when
// empty. The input shows it as a country plus the national part ("70 123 456").

/** What the input shows: the country picked and the national number typed */
export interface PhoneParts {
  country: CountryCode
  national: string
}

/** True for a complete, valid international number such as "+96170123456" */
export function isPhoneNumber(value: string): boolean {
  return isValidPhoneNumber(value)
}

/** The national part written the way the country writes it ("70 123 456") */
export function formatNational(national: string, country: CountryCode): string {
  return new AsYouType(country).input(national)
}

/**
 * A saved value as country and national number. An empty value, or one we
 * cannot place, starts on `fallback` (the visitor's country).
 */
export function splitPhone(value: string, fallback: CountryCode): PhoneParts {
  const parsed = value ? parsePhoneNumberFromString(value) : undefined
  if (parsed?.country) {
    return {
      country: parsed.country,
      national: formatNational(parsed.nationalNumber, parsed.country),
    }
  }
  return { country: fallback, national: value }
}

/**
 * What was typed, read against the picked country. A number typed or pasted
 * with its "+" prefix picks its own country ("+33 6 12 34 56 78" -> France).
 */
export function readTyped(text: string, country: CountryCode): PhoneParts {
  if (text.trim().startsWith("+")) {
    const parsed = parsePhoneNumberFromString(text)
    if (parsed?.country) {
      return { country: parsed.country, national: parsed.formatNational() }
    }
  }
  return { country, national: text }
}

/**
 * The form value for a country and national number: the international number
 * (a leading national 0 dropped), "+<code><digits>" when it does not parse
 * yet (so validation can say it is incomplete), or "" when nothing is typed.
 */
export function joinPhone(national: string, country: CountryCode): string {
  const digits = national.replace(/\D/g, "")
  if (!digits) return ""
  const parsed = parsePhoneNumberFromString(national, country)
  return parsed?.number ?? `+${getCountryCallingCode(country)}${digits}`
}

/** A saved number written for people ("+961 1 123 456"), or as stored */
export function formatPhone(value: string): string {
  return parsePhoneNumberFromString(value)?.formatInternational() ?? value
}

/** A sample mobile number in national format, as a placeholder ("71 123 456") */
export function examplePhone(country: CountryCode): string {
  return getExampleNumber(country, examples)?.formatNational() ?? ""
}
