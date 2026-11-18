"use client"

import { Button } from "@workspace/ui/components/button"
import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { MapPinIcon, PlusIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { HoursEditor } from "@/features/specialists/hours/hours-editor"
import { toWindows } from "@/features/specialists/hours/hours"
import {
  HoursSummary,
  LocationCard,
} from "@/features/specialists/locations/location-card"
import { LocationForm } from "@/features/specialists/locations/location-form"
import { useLocations } from "@/features/specialists/locations/use-locations"
import { generalErrorMessage } from "@/lib/forms/api-errors"
import { CardHeading } from "@/features/dashboard/card-heading"

/** What is open for editing: a location's details or hours, or a new one */
type Editing =
  | { kind: "new" }
  | { kind: "location"; id: string }
  | { kind: "hours"; id: string | null }
  | null

/**
 * The locations page: each place of work with its address and weekly hours,
 * the main one first. One form is open at a time.
 */
export function LocationsSection() {
  const t = useTranslations("Locations")
  const tFormErrors = useTranslations("Form.errors")
  const data = useLocations()
  const [editing, setEditing] = React.useState<Editing>(null)
  const close = () => setEditing(null)
  const isEditing = (kind: string, id?: string | null) =>
    editing?.kind === kind &&
    (id === undefined || ("id" in editing && editing.id === id))

  const sorted = [...data.locations].sort(
    (a, b) => Number(b.is_primary) - Number(a.is_primary)
  )
  // Hours tied to no location (made elsewhere): shown so they can be edited
  const unplaced = toWindows(data.hours, null)

  const addButton = (
    <Button
      onClick={() => setEditing({ kind: "new" })}
      disabled={isEditing("new")}
    >
      <PlusIcon data-icon="inline-start" />
      {t("add")}
    </Button>
  )

  let body: React.ReactNode
  if (data.failure) {
    body = (
      <LoadError
        message={generalErrorMessage(data.failure.error, tFormErrors)}
        onRetry={data.failure.retry}
      />
    )
  } else if (data.isLoading) {
    body = <Skeleton className="h-48 w-full rounded-xl" />
  } else if (sorted.length === 0 && !isEditing("new")) {
    body = (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
          <MapPinIcon aria-hidden className="size-8 text-muted-foreground" />
          <div className="flex flex-col gap-1">
            <p className="font-medium">{t("empty.title")}</p>
            <p className="max-w-sm text-sm text-pretty text-muted-foreground">
              {t("empty.description")}
            </p>
          </div>
          {addButton}
        </CardContent>
      </Card>
    )
  } else {
    body = sorted.map((location) => {
      const id = location.id ?? ""
      const hours = toWindows(data.hours, id)
      const place = location.label || location.city || ""
      if (isEditing("location", id)) {
        return (
          <LocationForm
            key={id}
            location={location}
            countries={data.countries}
            hasOthers={sorted.length > 1}
            onSaved={(saved) => {
              data.savedLocation(saved)
              close()
            }}
            onCancel={close}
          />
        )
      }
      if (isEditing("hours", id)) {
        return (
          <HoursEditor
            key={id}
            title={t("hours.title", { place })}
            locationId={id}
            saved={hours}
            onSaved={data.reloadHours}
            onDone={close}
          />
        )
      }
      return (
        <LocationCard
          key={id}
          location={location}
          countryName={data.countryName(location.country_id)}
          hours={hours}
          hasOthers={sorted.length > 1}
          onEdit={() => setEditing({ kind: "location", id })}
          onEditHours={() => setEditing({ kind: "hours", id })}
          onSaved={data.savedLocation}
          onRemoved={data.removedLocation}
        />
      )
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={data.locations.length > 0 && addButton}
      />
      {isEditing("new") && (
        <LocationForm
          countries={data.countries}
          hasOthers={data.locations.length > 0}
          onSaved={(saved) => {
            data.savedLocation(saved)
            // Straight on to its hours: a place without them is half done
            setEditing({ kind: "hours", id: saved.id ?? null })
          }}
          onCancel={close}
        />
      )}
      {body}
      {unplaced.length > 0 &&
        (isEditing("hours", null) ? (
          <HoursEditor
            title={t("hours.unplacedTitle")}
            locationId={null}
            saved={unplaced}
            onSaved={data.reloadHours}
            onDone={close}
          />
        ) : (
          <Card className="[--card-spacing:--spacing(5)]">
            <CardContent className="flex flex-col gap-3">
              <CardHeading>{t("hours.unplacedTitle")}</CardHeading>
              <HoursSummary hours={unplaced} />
              <Button
                variant="outline"
                size="sm"
                className="w-fit"
                onClick={() => setEditing({ kind: "hours", id: null })}
              >
                {t("card.editHours")}
              </Button>
            </CardContent>
          </Card>
        ))}
    </div>
  )
}
