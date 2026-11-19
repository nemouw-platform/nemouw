"use client"

import { Button } from "@workspace/ui/components/button"
import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { MapPinIcon, PlusIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { FormDialog } from "@/features/dashboard/form-dialog"
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

/**
 * The popup's content: a location's details (a new one without id), or the
 * opening hours at a location (null: hours tied to none). `afterAdd` marks
 * the hours step that follows adding a location.
 */
type Editing =
  | { kind: "location"; id?: string }
  | { kind: "hours"; id: string | null; afterAdd?: boolean }
  | null

/**
 * The locations page: each place of work with its address and weekly hours,
 * the main one first. Adding or editing opens a popup, one at a time.
 */
export function LocationsSection() {
  const t = useTranslations("Locations")
  const tFormErrors = useTranslations("Form.errors")
  const data = useLocations()
  const [editing, setEditing] = React.useState<Editing>(null)
  const close = () => setEditing(null)

  const sorted = [...data.locations].sort(
    (a, b) => Number(b.is_primary) - Number(a.is_primary)
  )
  // Hours tied to no location (made elsewhere): shown so they can be edited
  const unplaced = toWindows(data.hours, null)

  const addButton = (
    <Button onClick={() => setEditing({ kind: "location" })}>
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
  } else if (sorted.length === 0) {
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
      return (
        <LocationCard
          key={id}
          location={location}
          countryName={data.countryName(location.country_id)}
          hours={toWindows(data.hours, id)}
          hasOthers={sorted.length > 1}
          onEdit={() => setEditing({ kind: "location", id })}
          onEditHours={() => setEditing({ kind: "hours", id })}
          onSaved={data.savedLocation}
          onRemoved={data.removedLocation}
        />
      )
    })
  }

  /** The popup's form for what is being edited */
  let form: React.ReactNode = null
  if (editing?.kind === "location") {
    const location = data.locations.find((item) => item.id === editing.id)
    form = (
      <LocationForm
        key={editing.id ?? "new"}
        location={location}
        countries={data.countries}
        hasOthers={data.locations.some((item) => item.id !== editing.id)}
        onSaved={(saved) => {
          data.savedLocation(saved)
          // A new place goes straight on to its hours: without them it is
          // half done
          if (!location && saved.id) {
            setEditing({ kind: "hours", id: saved.id, afterAdd: true })
          } else close()
        }}
        onCancel={close}
      />
    )
  } else if (editing?.kind === "hours") {
    const location = data.locations.find((item) => item.id === editing.id)
    form = (
      <HoursEditor
        key={`hours-${editing.id}`}
        title={
          location
            ? t("hours.title", { place: location.label || location.city || "" })
            : t("hours.unplacedTitle")
        }
        locationId={editing.id}
        saved={toWindows(data.hours, editing.id)}
        onSaved={data.reloadHours}
        onDone={close}
        cancelLabel={editing.afterAdd ? t("hours.later") : undefined}
      />
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={data.locations.length > 0 && addButton}
      />
      {body}
      <FormDialog open={form !== null} onClose={close}>
        {form}
      </FormDialog>
      {unplaced.length > 0 && (
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
      )}
    </div>
  )
}
