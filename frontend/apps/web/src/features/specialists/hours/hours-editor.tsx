"use client"

import { Button } from "@workspace/ui/components/button"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@workspace/ui/lib/utils"
import { PlusIcon, XIcon } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  byDay,
  checkHours,
  hoursChanges,
  isUnchanged,
  WEEK_DAYS,
  weekdayName,
  type HoursProblem,
  type HoursWindow,
} from "@/features/specialists/hours/hours"
import { locationsApi } from "@/lib/api"
import { useFormRequest } from "@/lib/forms/use-form-request"

interface HoursEditorProps {
  /** e.g. "Opening hours at Hamra clinic" */
  title: string
  /** The location these hours are at; null for hours tied to none */
  locationId: string | null
  /** The saved windows at that location */
  saved: HoursWindow[]
  /** Called after a save, also a failed one, to load the hours again */
  onSaved: () => void
  onDone: () => void
  /** The button that closes without saving. Default: "Cancel" */
  cancelLabel?: string
}

let newKeys = 0

/**
 * A week of opening hours: each day closed, or open in one or more windows.
 * Saving sends only what changed (each window is its own call).
 */
export function HoursEditor({
  title,
  locationId,
  saved,
  onSaved,
  onDone,
  cancelLabel,
}: HoursEditorProps) {
  const t = useTranslations("Locations.hours")
  const locale = useLocale()
  const [windows, setWindows] = React.useState(saved)
  const [problems, setProblems] = React.useState<
    Partial<Record<string, HoursProblem>>
  >({})
  const days = byDay(windows)

  const save = useFormRequest(
    async (signal, edited: HoursWindow[]) => {
      const changes = hoursChanges(saved, edited, locationId)
      for (const id of changes.remove) {
        await locationsApi.removeHours(id, { signal })
      }
      for (const { id, body } of changes.update) {
        await locationsApi.updateHours(id, body, { signal })
      }
      for (const { key, body } of changes.add) {
        const created = await locationsApi.addHours(body, { signal })
        // Saved: a retry after a later failure must not add it again
        setWindows((list) =>
          list.map((item) =>
            item.key === key ? { ...item, id: created.id } : item
          )
        )
      }
    },
    {
      onSuccess: () => {
        onSaved()
        onDone()
      },
      onError: () => {
        // Some calls may have gone through: show what is saved now
        onSaved()
        return false
      },
    }
  )

  function change(key: string, patch: Partial<HoursWindow>) {
    setWindows((list) =>
      list.map((item) => (item.key === key ? { ...item, ...patch } : item))
    )
    setProblems((current) => ({ ...current, [key]: undefined }))
  }

  function add(day: HoursWindow["day"]) {
    const last = days[day].at(-1)
    setWindows((list) => [
      ...list,
      {
        key: `new-${++newKeys}`,
        day,
        // The first window of a day starts as a usual working day
        start: last ? last.end : "09:00",
        end: last ? "" : "17:00",
      },
    ])
  }

  function submit(event: React.FormEvent) {
    event.preventDefault()
    const found = checkHours(windows)
    setProblems(found)
    if (Object.keys(found).length > 0) return
    if (isUnchanged(hoursChanges(saved, windows, locationId))) return onDone()
    save.run(windows)
  }

  return (
    <FormCard
      title={title}
      description={t("description")}
      onSubmit={submit}
      formError={save.formError}
      submit={
        <>
          <Button type="button" variant="ghost" onClick={onDone}>
            {cancelLabel ?? t("cancel")}
          </Button>
          <SubmitButton
            pending={save.isPending}
            pendingLabel={t("saving")}
            size="default"
          >
            {t("save")}
          </SubmitButton>
        </>
      }
    >
      <div className="flex flex-col divide-y">
        {WEEK_DAYS.map((day) => {
          const name = weekdayName(day, locale)
          return (
            <div
              key={day}
              role="group"
              aria-label={name}
              className="grid gap-2 py-3 first:pt-0 last:pb-0 sm:grid-cols-[8rem_1fr] sm:gap-4"
            >
              <span className="pt-2 text-sm font-medium capitalize">
                {name}
              </span>
              <div className="flex flex-col gap-2">
                {days[day].length === 0 && (
                  <span className="pt-2 text-sm text-muted-foreground">
                    {t("closed")}
                  </span>
                )}
                {days[day].map((window) => (
                  <WindowRow
                    key={window.key}
                    window={window}
                    dayName={name}
                    problem={problems[window.key]}
                    onChange={(patch) => change(window.key, patch)}
                    onRemove={() =>
                      setWindows((list) =>
                        list.filter((item) => item.key !== window.key)
                      )
                    }
                  />
                ))}
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="w-fit"
                  aria-label={t("addFor", { day: name })}
                  onClick={() => add(day)}
                >
                  <PlusIcon data-icon="inline-start" />
                  {days[day].length === 0 ? t("open") : t("addMore")}
                </Button>
              </div>
            </div>
          )
        })}
      </div>
    </FormCard>
  )
}

/** One window: opens and closes, a remove button, and what is wrong with it */
function WindowRow({
  window,
  dayName,
  problem,
  onChange,
  onRemove,
}: {
  window: HoursWindow
  dayName: string
  problem: HoursProblem | undefined
  onChange: (patch: Partial<HoursWindow>) => void
  onRemove: () => void
}) {
  const t = useTranslations("Locations.hours")
  const problemId = React.useId()
  const control = {
    type: "time",
    "aria-invalid": problem ? true : undefined,
    "aria-describedby": problem ? problemId : undefined,
    className: "h-10 w-32 bg-card",
  }

  return (
    // The message sits beside the times (wrapping only on narrow screens), so
    // a problem does not push the next days down
    <div className="flex flex-wrap items-center gap-2">
      {/* A range reads left to right in every language, as on the card */}
      <span dir="ltr" className="flex items-center gap-2">
        <Input
          {...control}
          aria-label={t("opens", { day: dayName })}
          placeholder="09:00"
          value={window.start}
          onChange={(event) =>
            onChange({ start: event.target.value.slice(0, 5) })
          }
        />
        <span aria-hidden className="text-muted-foreground">
          –
        </span>
        <Input
          {...control}
          aria-label={t("closes", { day: dayName })}
          placeholder="17:00"
          value={window.end}
          onChange={(event) =>
            onChange({ end: event.target.value.slice(0, 5) })
          }
        />
      </span>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={t("remove", { day: dayName })}
        onClick={onRemove}
      >
        <XIcon />
      </Button>
      {problem && (
        <p id={problemId} className={cn("text-sm text-destructive")}>
          {t(`problems.${problem}`)}
        </p>
      )}
    </div>
  )
}
