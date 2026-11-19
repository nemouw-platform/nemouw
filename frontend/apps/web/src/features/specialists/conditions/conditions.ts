import type { Condition } from "@workspace/api"

import { includesFolded } from "@/lib/search"

/** A condition's name in the page's language, else in English */
export function conditionLabel(condition: Condition, locale: string): string {
  const translated =
    locale === "fr"
      ? condition.label_fr
      : locale === "ar"
        ? condition.label_ar
        : undefined
  return translated || condition.label || condition.name || ""
}

/** The conditions in the order that language sorts their names */
export function sortConditions(
  conditions: Condition[],
  locale: string
): Condition[] {
  return [...conditions].sort((a, b) =>
    conditionLabel(a, locale).localeCompare(conditionLabel(b, locale), locale)
  )
}

/**
 * Whether a condition matches the search: by its name in the page's
 * language or in English, ignoring case and accents
 */
export function matchesCondition(
  condition: Condition,
  query: string,
  locale: string
): boolean {
  if (!query.trim()) return true
  return [conditionLabel(condition, locale), condition.label ?? ""].some(
    (text) => includesFolded(text, query)
  )
}

/** Whether two selections hold the same conditions, in any order */
export function sameSelection(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false
  const set = new Set(a)
  return b.every((id) => set.has(id))
}
