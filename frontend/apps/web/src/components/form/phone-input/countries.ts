import { hasFlag } from "country-flag-icons"
import {
  getCountries,
  getCountryCallingCode,
  type CountryCode,
} from "libphonenumber-js"

import { regionName } from "@/lib/region-names"

export interface CountryOption {
  code: CountryCode
  /** In the page's language, e.g. "Liban" on a French page */
  name: string
  /** Without the "+", e.g. "961" */
  callingCode: string
}

const byLocale = new Map<string, CountryOption[]>()

/**
 * Every country with a dialing code and a flag, named in `locale` and sorted
 * the way that language sorts. Built once per locale.
 */
export function countryOptions(locale: string): CountryOption[] {
  let options = byLocale.get(locale)
  if (!options) {
    options = getCountries()
      .filter((code) => hasFlag(code))
      .map((code) => ({
        code,
        name: regionName(code, locale) ?? code,
        callingCode: getCountryCallingCode(code),
      }))
      .sort((a, b) => a.name.localeCompare(b.name, locale))
    byLocale.set(locale, options)
  }
  return options
}

/** Lowercase without accents, so "cote" finds "Côte d'Ivoire" */
function fold(text: string): string {
  return text
    .normalize("NFD")
    .replace(/\p{Mark}/gu, "")
    .toLowerCase()
}

/** Search by name ("leb"), ISO code ("lb") or dialing code ("961", "+961") */
export function matchesCountry(option: CountryOption, query: string): boolean {
  const search = fold(query.trim())
  if (!search) return true
  const digits = search.replace(/^\+/, "")
  return (
    fold(option.name).includes(search) ||
    option.code.toLowerCase() === search ||
    (/^\d+$/.test(digits) && option.callingCode.startsWith(digits))
  )
}
