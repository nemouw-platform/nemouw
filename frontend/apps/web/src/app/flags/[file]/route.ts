import * as flags from "country-flag-icons/string/3x2"
import { getCountries } from "libphonenumber-js"

// Flags as files (/flags/LB.svg), so a page downloads only those it shows
// instead of bundling every flag. All are prerendered at build; the proxy
// skips paths with a dot, so no locale is added.

const EXTENSION = ".svg"
const svgByCountry: Partial<Record<string, string>> = flags

export function generateStaticParams() {
  return getCountries()
    .filter((country) => svgByCountry[country])
    .map((country) => ({ file: `${country}${EXTENSION}` }))
}

export async function GET(
  _request: Request,
  { params }: RouteContext<"/flags/[file]">
) {
  const { file } = await params
  const country = file.endsWith(EXTENSION)
    ? file.slice(0, -EXTENSION.length)
    : ""
  const svg = svgByCountry[country]
  if (!svg) return new Response(null, { status: 404 })

  return new Response(svg, {
    headers: {
      "Content-Type": "image/svg+xml",
      // A flag never changes for a given URL
      "Cache-Control": "public, max-age=31536000, immutable",
    },
  })
}
