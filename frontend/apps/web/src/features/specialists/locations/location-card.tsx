"use client"

import type { SpecialistLocation } from "@workspace/api"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
} from "@workspace/ui/components/card"
import { isApiError } from "@workspace/api"
import {
  ClockIcon,
  LoaderCircleIcon,
  MapPinIcon,
  PencilIcon,
  PhoneIcon,
  StarIcon,
  Trash2Icon,
} from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import { ConfirmButton } from "@/components/confirm-button"
import { FormAlert } from "@/components/form/form-alert"
import { formatPhone } from "@/components/form/phone-input/phone"
import { CardHeading } from "@/features/dashboard/card-heading"
import {
  byDay,
  WEEK_DAYS,
  weekdayName,
  type HoursWindow,
} from "@/features/specialists/hours/hours"
import { asPrimaryRequest } from "@/features/specialists/locations/location-schema"
import { locationsApi } from "@/lib/api"
import { generalErrorMessage } from "@/lib/forms/api-errors"

interface LocationCardProps {
  location: SpecialistLocation
  countryName: string
  hours: HoursWindow[]
  /** Other locations exist (the main one cannot be removed then) */
  hasOthers: boolean
  onEdit: () => void
  onEditHours: () => void
  onSaved: (location: SpecialistLocation) => void
  onRemoved: (id: string) => void
}

/** One place of work: its address, phone and weekly hours, and its actions */
export function LocationCard({
  location,
  countryName,
  hours,
  hasOthers,
  onEdit,
  onEditHours,
  onSaved,
  onRemoved,
}: LocationCardProps) {
  const t = useTranslations("Locations")
  const tFormErrors = useTranslations("Form.errors")
  const [promoting, setPromoting] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const place = [location.city, countryName].filter(Boolean).join(", ")
  const lockedPrimary = Boolean(location.is_primary && hasOthers)

  async function makePrimary() {
    if (!location.id) return
    setPromoting(true)
    setError(null)
    try {
      onSaved(
        await locationsApi.update(location.id, asPrimaryRequest(location))
      )
    } catch (failure) {
      setError(generalErrorMessage(failure, tFormErrors))
    } finally {
      setPromoting(false)
    }
  }

  return (
    <Card className="[--card-spacing:--spacing(5)]">
      <CardHeader>
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-md bg-muted">
            <MapPinIcon aria-hidden className="size-5 text-primary" />
          </span>
          <div className="flex min-w-0 flex-col gap-1">
            <CardHeading>{location.label || location.city}</CardHeading>
            <CardDescription>
              {[location.address_line1, location.address_line2, place]
                .filter(Boolean)
                .join(" · ")}
            </CardDescription>
            {location.phone && (
              <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <PhoneIcon aria-hidden className="size-3.5" />
                <span dir="ltr">{formatPhone(location.phone)}</span>
              </p>
            )}
          </div>
        </div>
        {location.is_primary && (
          <CardAction>
            <Badge variant="secondary">
              <StarIcon data-icon="inline-start" />
              {t("card.primary")}
            </Badge>
          </CardAction>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        <HoursSummary hours={hours} />
        <FormAlert>{error}</FormAlert>
      </CardContent>

      <CardFooter className="flex-wrap justify-end gap-2 py-3">
        {lockedPrimary && (
          <p className="me-auto text-xs text-muted-foreground">
            {t("remove.primaryHint")}
          </p>
        )}
        {!location.is_primary && (
          <Button
            variant="ghost"
            size="sm"
            disabled={promoting}
            onClick={() => void makePrimary()}
          >
            {promoting ? (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            ) : (
              <StarIcon data-icon="inline-start" />
            )}
            {t("card.makePrimary")}
          </Button>
        )}
        <ConfirmButton
          title={t("remove.title")}
          description={t("remove.description", {
            place: location.label || location.city || "",
          })}
          confirmLabel={t("remove.confirm")}
          cancelLabel={t("remove.cancel")}
          // The main location goes last: make another one main first
          disabled={lockedPrimary}
          onConfirm={async () => {
            if (!location.id) return
            await locationsApi.remove(location.id)
            onRemoved(location.id)
          }}
          errorMessage={(failure) =>
            isApiError(failure) && failure.code === "PRIMARY_LOCATION_REQUIRED"
              ? t("remove.primary")
              : null
          }
        >
          <Trash2Icon data-icon="inline-start" />
          {t("remove.action")}
        </ConfirmButton>
        <Button variant="outline" size="sm" onClick={onEditHours}>
          <ClockIcon data-icon="inline-start" />
          {t("card.editHours")}
        </Button>
        <Button variant="outline" size="sm" onClick={onEdit}>
          <PencilIcon data-icon="inline-start" />
          {t("card.edit")}
        </Button>
      </CardFooter>
    </Card>
  )
}

/** The week at a glance: each open day and its times */
export function HoursSummary({ hours }: { hours: HoursWindow[] }) {
  const t = useTranslations("Locations.hours")
  const locale = useLocale()
  const days = byDay(hours)
  const open = WEEK_DAYS.filter((day) => days[day].length > 0)

  if (open.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("none")}</p>
  }
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-sm">
      {open.map((day) => (
        <React.Fragment key={day}>
          <dt className="text-muted-foreground capitalize">
            {weekdayName(day, locale)}
          </dt>
          <dd>
            {/* Times read left to right on Arabic pages too */}
            <span dir="ltr">
              {days[day]
                .map((window) => `${window.start}–${window.end}`)
                .join(", ")}
            </span>
          </dd>
        </React.Fragment>
      ))}
    </dl>
  )
}
