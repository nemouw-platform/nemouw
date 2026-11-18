import { headers } from "next/headers"

import { countryFromHeaders } from "@/lib/visitor-country/country-from-headers"
import { VisitorCountryContext } from "@/lib/visitor-country/visitor-country-context"

/**
 * Reads the visitor's country from the request on the server and provides it
 * to everything below (useVisitorCountry). It reads request headers, so wrap
 * it in <Suspense>: the rest of the page still prerenders.
 */
export async function VisitorCountryProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const country = countryFromHeaders(await headers())
  return (
    <VisitorCountryContext country={country}>{children}</VisitorCountryContext>
  )
}
