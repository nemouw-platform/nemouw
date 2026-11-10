import type { Locale } from "@workspace/i18n"
import { Geist, Geist_Mono, IBM_Plex_Sans_Arabic } from "next/font/google"
import { cn } from "@workspace/ui/lib/utils"

const geist = Geist({ subsets: ["latin"], variable: "--font-sans" })

// Geist has no Arabic glyphs. Plex Sans Arabic also covers Latin, so mixed
// Arabic/Latin text (names, emails) stays in one consistent typeface.
const plexArabic = IBM_Plex_Sans_Arabic({
  subsets: ["arabic", "latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-sans",
  // Loaded only where used (/ar); preloading it would cost every locale 8 files
  preload: false,
})

const fontMono = Geist_Mono({ subsets: ["latin"], variable: "--font-mono" })

// Class names for <html>: sets --font-sans / --font-mono for the locale
export function fontClassNames(locale: Locale) {
  return cn(
    fontMono.variable,
    locale === "ar" ? plexArabic.variable : geist.variable
  )
}
