import { isSupportedCountry, type CountryCode } from "libphonenumber-js"

/** Nemouw serves Lebanon first: the country when the request does not say */
export const DEFAULT_COUNTRY: CountryCode = "LB"

/**
 * Two-letter country codes that hosts and CDNs add from the visitor's IP:
 * Vercel, Cloudflare, then CloudFront. The first one present wins.
 */
const GEO_HEADERS = [
  "x-vercel-ip-country",
  "cf-ipcountry",
  "cloudfront-viewer-country",
] as const

/**
 * The visitor's country from the request, found on the server from their IP
 * (no browser location prompt). Unknown values, such as Cloudflare's XX
 * (unknown) or T1 (Tor), fall back to the default.
 */
export function countryFromHeaders(headers: Pick<Headers, "get">): CountryCode {
  for (const name of GEO_HEADERS) {
    const code = headers.get(name)?.trim().toUpperCase()
    if (code && isSupportedCountry(code)) return code
  }
  return DEFAULT_COUNTRY
}
