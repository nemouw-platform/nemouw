"use client"

import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
  ComboboxTrigger,
} from "@workspace/ui/components/combobox"
import { InputGroupButton } from "@workspace/ui/components/input-group"
import type { CountryCode } from "libphonenumber-js"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import {
  countryOptions,
  matchesCountry,
  type CountryOption,
} from "@/components/form/phone-input/countries"
import { CountryFlag } from "@/components/form/phone-input/country-flag"

interface CountrySelectProps {
  value: CountryCode
  onChange: (country: CountryCode) => void
  /** Where focus goes after a pick: the number field, to type next */
  focusAfterPick?: React.RefObject<HTMLElement | null>
  disabled?: boolean
}

/**
 * The dialing-code picker at the start of the phone field: the flag and code
 * on the button, a searchable list (names in the page's language) when open.
 */
export function CountrySelect({
  value,
  onChange,
  focusAfterPick,
  disabled,
}: CountrySelectProps) {
  const t = useTranslations("PhoneInput")
  const picked = React.useRef(false)
  const locale = useLocale()
  const options = React.useMemo(() => countryOptions(locale), [locale])
  const selected = options.find((option) => option.code === value)

  return (
    <Combobox<CountryOption>
      items={options}
      value={selected ?? null}
      onValueChange={(option) => {
        if (!option) return
        picked.current = true
        onChange(option.code)
      }}
      // Base UI hands focus back to the button once the list has closed (after
      // its animation), so move it on from there; Escape stays on the button
      onOpenChangeComplete={(open) => {
        if (!open && picked.current) focusAfterPick?.current?.focus()
        picked.current = false
      }}
      itemToStringLabel={(option) => option.name}
      itemToStringValue={(option) => option.code}
      isItemEqualToValue={(option, current) => option.code === current.code}
      filter={matchesCountry}
      disabled={disabled}
    >
      <ComboboxTrigger
        render={<InputGroupButton variant="ghost" size="sm" />}
        aria-label={t("country", {
          country: selected?.name ?? value,
          code: selected?.callingCode ?? "",
        })}
        className="h-8 gap-1.5 ps-2 pe-1.5 font-normal"
      >
        <CountryFlag country={value} />
        <span className="text-sm tabular-nums">+{selected?.callingCode}</span>
      </ComboboxTrigger>
      <ComboboxContent className="w-72" align="start">
        <ComboboxInput
          showTrigger={false}
          placeholder={t("search")}
          aria-label={t("search")}
        />
        <ComboboxEmpty>{t("noResults")}</ComboboxEmpty>
        <ComboboxList>
          {(option: CountryOption) => (
            <ComboboxItem key={option.code} value={option}>
              <CountryFlag country={option.code} />
              <span className="min-w-0 flex-1 truncate">{option.name}</span>
              <span
                dir="ltr"
                className="text-xs text-muted-foreground tabular-nums"
              >
                +{option.callingCode}
              </span>
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  )
}
