"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import type { Country, SpecialistLocation } from "@workspace/api"
import { Checkbox } from "@workspace/ui/components/checkbox"
import { isSupportedCountry } from "libphonenumber-js"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"
import { Controller, useForm, useWatch } from "react-hook-form"

import { FormControllerField } from "@/components/form/form-controller-field"
import { FormTextField } from "@/components/form/form-field"
import { CountryFlag, PhoneInput } from "@/components/form/phone-input"
import { SelectControl } from "@/components/form/select-control"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  createLocationSchema,
  LOCATION_FIELDS,
  toLocationRequest,
  toLocationValues,
  type LocationValues,
} from "@/features/specialists/locations/location-schema"
import { locationsApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"
import { regionName } from "@/lib/region-names"
import { useVisitorCountry } from "@/lib/visitor-country/visitor-country-context"

interface LocationFormProps {
  /** The location to edit; none to add one */
  location?: SpecialistLocation
  countries: Country[]
  /** Whether other locations exist (the first one is always the main one) */
  hasOthers: boolean
  onSaved: (location: SpecialistLocation) => void
  onCancel: () => void
}

/** Adds a place where the specialist works, or edits one */
export function LocationForm({
  location,
  countries,
  hasOthers,
  onSaved,
  onCancel,
}: LocationFormProps) {
  const t = useTranslations("Locations.form")
  const tFormErrors = useTranslations("Form.errors")
  const locale = useLocale()
  const visitorCountry = useVisitorCountry()
  const schema = React.useMemo(
    () =>
      createLocationSchema(
        { country: t("country"), city: t("city") },
        tFormErrors,
        { phoneFormat: t("phoneFormat") }
      ),
    [t, tFormErrors]
  )
  // A new location starts in the visitor's country when it is in service
  const defaultCountry =
    countries.find((country) => country.iso_code === visitorCountry) ??
    countries[0]
  const form = useForm<LocationValues>({
    resolver: zodResolver(schema),
    defaultValues: toLocationValues(location, defaultCountry?.id ?? ""),
    mode: "onTouched",
  })
  const save = useFormRequest(
    (signal, values: LocationValues) => {
      const body = toLocationRequest(values)
      return location?.id
        ? locationsApi.update(location.id, body, { signal })
        : locationsApi.add(body, { signal })
    },
    {
      onSuccess: onSaved,
      onError: (error) =>
        applyFieldErrors(form, error, LOCATION_FIELDS, tFormErrors),
    }
  )
  const { errors } = form.formState

  // The phone field starts on the location's country
  const countryId = useWatch({ control: form.control, name: "country_id" })
  const isoCode = countries.find(
    (country) => country.id === countryId
  )?.iso_code
  const phoneCountry =
    isoCode && isSupportedCountry(isoCode) ? isoCode : undefined

  const countryOptions = countries.map((country) => ({
    value: country.id ?? "",
    label: regionName(country.iso_code ?? "", locale) ?? country.name ?? "",
    icon:
      country.iso_code && isSupportedCountry(country.iso_code) ? (
        <CountryFlag country={country.iso_code} />
      ) : undefined,
  }))
  const id = (name: string) => `${name}-${location?.id ?? "new"}`
  // The main location stays main until another one takes its place
  const canChoosePrimary = hasOthers && !location?.is_primary

  return (
    <FormCard
      title={location ? t("editTitle") : t("newTitle")}
      description={hasOthers || location ? undefined : t("firstDescription")}
      onSubmit={form.handleSubmit((values) => save.run(values))}
      formError={save.formError}
      cancel={{ label: t("cancel"), onClick: onCancel }}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("saving")}
          size="default"
        >
          {location ? t("save") : t("add")}
        </SubmitButton>
      }
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <FormTextField
          id={id("label")}
          label={t("label")}
          placeholder={t("labelPlaceholder")}
          description={t("labelHint")}
          error={errors.label}
          {...form.register("label")}
        />
        <FormControllerField
          name="country_id"
          id={id("country_id")}
          control={form.control}
          required
          label={t("country")}
          error={errors.country_id}
          render={(props) => (
            <SelectControl
              {...props}
              options={countryOptions}
              placeholder={t("countryPlaceholder")}
            />
          )}
        />
        <FormTextField
          id={id("city")}
          required
          label={t("city")}
          placeholder={t("cityPlaceholder")}
          error={errors.city}
          {...form.register("city")}
        />
        <FormControllerField
          name="phone"
          id={id("phone")}
          control={form.control}
          label={t("phone")}
          description={t("phoneHint")}
          error={errors.phone}
          render={(props) => (
            <PhoneInput {...props} defaultCountry={phoneCountry} />
          )}
        />
        <FormTextField
          id={id("address_line1")}
          label={t("address1")}
          placeholder={t("address1Placeholder")}
          error={errors.address_line1}
          {...form.register("address_line1")}
        />
        <FormTextField
          id={id("address_line2")}
          label={t("address2")}
          placeholder={t("address2Placeholder")}
          error={errors.address_line2}
          {...form.register("address_line2")}
        />
      </div>
      {canChoosePrimary && (
        <Controller
          name="is_primary"
          control={form.control}
          render={({ field }) => (
            <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm">
              <Checkbox
                checked={field.value}
                onCheckedChange={(checked) => field.onChange(checked === true)}
              />
              {t("makePrimary")}
            </label>
          )}
        />
      )}
    </FormCard>
  )
}
