import { notFound } from "next/navigation"

// Unknown paths under a locale render the localized [locale]/not-found.tsx
// instead of the unlocalized global 404.
export default function CatchAll() {
  notFound()
}
