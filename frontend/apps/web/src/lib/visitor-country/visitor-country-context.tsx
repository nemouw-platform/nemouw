"use client"

import type { CountryCode } from "libphonenumber-js"
import * as React from "react"

import { DEFAULT_COUNTRY } from "@/lib/visitor-country/country-from-headers"

const VisitorCountry = React.createContext<CountryCode>(DEFAULT_COUNTRY)

/** Hands the country the server found down to Client Components */
export function VisitorCountryContext({
  country,
  children,
}: {
  country: CountryCode
  children: React.ReactNode
}) {
  return <VisitorCountry value={country}>{children}</VisitorCountry>
}

/** The visitor's country, as found on the server (Lebanon outside a provider) */
export function useVisitorCountry(): CountryCode {
  return React.useContext(VisitorCountry)
}
