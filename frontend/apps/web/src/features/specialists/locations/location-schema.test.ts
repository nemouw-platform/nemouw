import type { SpecialistLocation } from "@workspace/api"
import { describe, expect, it } from "vitest"

import {
  asPrimaryRequest,
  createLocationSchema,
  toLocationRequest,
  toLocationValues,
  type LocationValues,
} from "@/features/specialists/locations/location-schema"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

const t = ((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key) as FormErrorsTranslator
const schema = createLocationSchema({ country: "Country", city: "City" }, t, {
  phoneFormat: "phoneFormat",
})

const saved: SpecialistLocation = {
  id: "l1",
  country_id: "lb",
  label: "Hamra clinic",
  city: "Beirut",
  address_line1: "Bliss Street",
  address_line2: null,
  phone: "+9611123456",
  latitude: null,
  longitude: null,
  is_primary: true,
}

describe("createLocationSchema", () => {
  const values: LocationValues = toLocationValues(undefined, "lb")
  const issues = (change: Partial<LocationValues>) => {
    const result = schema.safeParse({ ...values, ...change })
    return result.success
      ? {}
      : Object.fromEntries(
          result.error.issues.map((i) => [i.path[0], i.message])
        )
  }

  it("needs a city; everything else may stay empty", () => {
    expect(issues({})).toEqual({ city: 'required {"field":"City"}' })
    expect(issues({ city: "Beirut" })).toEqual({})
  })

  it("needs a valid phone number when one is given", () => {
    expect(issues({ city: "Beirut", phone: "+9611" })).toEqual({
      phone: "phoneFormat",
    })
  })
})

describe("toLocationValues / toLocationRequest", () => {
  it("starts a new location in the default country", () => {
    expect(toLocationValues(undefined, "lb")).toMatchObject({
      country_id: "lb",
      city: "",
      is_primary: false,
    })
  })

  it("leaves empty fields out and never sends is_primary: false", () => {
    expect(toLocationRequest(toLocationValues(saved, "x"))).toEqual({
      country_id: "lb",
      city: "Beirut",
      label: "Hamra clinic",
      address_line1: "Bliss Street",
      phone: "+9611123456",
      is_primary: true,
    })
    const other = { ...saved, is_primary: false }
    expect(toLocationRequest(toLocationValues(other, "x"))).not.toHaveProperty(
      "is_primary"
    )
  })

  it("makes a location the primary one, keeping its fields", () => {
    expect(asPrimaryRequest({ ...saved, is_primary: false })).toMatchObject({
      city: "Beirut",
      is_primary: true,
    })
  })
})
