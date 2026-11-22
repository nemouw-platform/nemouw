/** Lowercase without accents or marks, so "cote" finds "Côte d'Ivoire" */
export function fold(text: string): string {
  return text
    .normalize("NFD")
    .replace(/\p{Mark}/gu, "")
    .toLowerCase()
}

/** Whether `text` contains what was typed, ignoring case and accents */
export function includesFolded(text: string, query: string): boolean {
  return fold(text).includes(fold(query.trim()))
}
