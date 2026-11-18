const byLocale = new Map<string, Intl.DisplayNames>()

/**
 * A country or region's name in the page's language, from its ISO code
 * ("LB" -> "Liban" on a French page), or undefined when unknown
 */
export function regionName(code: string, locale: string): string | undefined {
  let names = byLocale.get(locale)
  if (!names) {
    names = new Intl.DisplayNames([locale], { type: "region" })
    byLocale.set(locale, names)
  }
  try {
    return names.of(code.toUpperCase())
  } catch {
    // Not a valid region code
    return undefined
  }
}
