"use client"

import type {
  Country,
  SpecialistAvailability,
  SpecialistLocation,
} from "@workspace/api"
import { useLoad } from "@workspace/ui/hooks/use-load"
import { useLocale } from "next-intl"
import * as React from "react"

import { countriesApi, locationsApi } from "@/lib/api"
import { regionName } from "@/lib/region-names"

/**
 * Where and when the specialist works: their locations, every opening
 * window, and the countries a location can be in. Saves report back through
 * `savedLocation` and `removedLocation`; hours are loaded again after a save.
 */
export function useLocations() {
  const locale = useLocale()
  const [locations, setLocations] = React.useState<SpecialistLocation[]>([])
  const loads = {
    locations: useLoad((signal) => locationsApi.list({ signal }), {
      onSuccess: setLocations,
    }),
    hours: useLoad((signal) => locationsApi.listHours({ signal })),
    countries: useLoad((signal) => countriesApi.list(true, { signal })),
  }
  const failure = Object.values(loads).find((load) => load.failed)

  /** A saved location in place (or added); a new primary demotes the old one */
  const savedLocation = React.useCallback((saved: SpecialistLocation) => {
    setLocations((list) => {
      const others = list
        .filter((item) => item.id !== saved.id)
        .map((item) =>
          saved.is_primary ? { ...item, is_primary: false } : item
        )
      const index = list.findIndex((item) => item.id === saved.id)
      if (index === -1) return [...others, saved]
      return [...others.slice(0, index), saved, ...others.slice(index)]
    })
  }, [])

  const reloadHours = loads.hours.retry
  const removedLocation = React.useCallback(
    (id: string) => {
      setLocations((list) => list.filter((item) => item.id !== id))
      // Its hours went with it
      reloadHours()
    },
    [reloadHours]
  )

  return {
    locations,
    hours: loads.hours.data ?? ([] as SpecialistAvailability[]),
    countries: loads.countries.data ?? ([] as Country[]),
    /** A country's name in the page's language, from its id */
    countryName: (id: string | undefined) => {
      const country = loads.countries.data?.find((item) => item.id === id)
      if (!country) return ""
      return regionName(country.iso_code ?? "", locale) ?? country.name ?? ""
    },
    isLoading: Object.values(loads).some((load) => load.isLoading),
    failure,
    savedLocation,
    removedLocation,
    reloadHours,
  }
}
