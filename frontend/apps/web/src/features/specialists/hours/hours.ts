import type {
  SpecialistAvailability,
  SpecialistAvailabilityWriteRequest,
  WeekDay,
} from "@workspace/api"

// Opening hours as the editor sees them: windows on a weekday, kept or new,
// and what to send to turn the saved ones into the edited ones.

/** Monday first, as the backend lists them */
export const WEEK_DAYS = [
  "monday",
  "tuesday",
  "wednesday",
  "thursday",
  "friday",
  "saturday",
  "sunday",
] as const satisfies readonly WeekDay[]

export interface HoursWindow {
  /** Stable React key: the saved id, or a made-up one for a new window */
  key: string
  /** Set once saved */
  id?: string
  day: WeekDay
  /** "HH:mm", or "" while not filled in */
  start: string
  end: string
}

/** The saved windows at one location (null: the ones tied to none) */
export function toWindows(
  rows: SpecialistAvailability[],
  locationId: string | null
): HoursWindow[] {
  return rows
    .filter((row) => (row.location_id ?? null) === locationId)
    .filter(
      (
        row
      ): row is SpecialistAvailability & { id: string; day_of_week: WeekDay } =>
        Boolean(row.id && row.day_of_week)
    )
    .map((row) => ({
      key: row.id,
      id: row.id,
      day: row.day_of_week,
      start: row.start_time ?? "",
      end: row.end_time ?? "",
    }))
}

/** Each weekday's windows, earliest first (every day present, maybe empty) */
export function byDay(windows: HoursWindow[]): Record<WeekDay, HoursWindow[]> {
  const days = Object.fromEntries(
    WEEK_DAYS.map((day) => [day, [] as HoursWindow[]])
  ) as Record<WeekDay, HoursWindow[]>
  for (const window of windows) days[window.day].push(window)
  for (const day of WEEK_DAYS) {
    days[day].sort((a, b) => a.start.localeCompare(b.start))
  }
  return days
}

/** What is wrong with a window */
export type HoursProblem =
  /** A time is not filled in */
  | "missing"
  /** It closes before (or when) it opens */
  | "order"
  /** It overlaps another window that day */
  | "overlap"

/** The windows that cannot be saved, by key */
export function checkHours(
  windows: HoursWindow[]
): Partial<Record<string, HoursProblem>> {
  const problems: Partial<Record<string, HoursProblem>> = {}
  for (const day of Object.values(byDay(windows))) {
    let latestEnd = ""
    for (const window of day) {
      if (!window.start || !window.end) problems[window.key] = "missing"
      else if (window.end <= window.start) problems[window.key] = "order"
      else if (latestEnd && window.start < latestEnd) {
        problems[window.key] = "overlap"
      }
      if (window.end > latestEnd) latestEnd = window.end
    }
  }
  return problems
}

export interface HoursChanges {
  remove: string[]
  update: Array<{ id: string; body: SpecialistAvailabilityWriteRequest }>
  /** With the window's key, to give it its id once saved */
  add: Array<{ key: string; body: SpecialistAvailabilityWriteRequest }>
}

/** The calls that turn the saved windows into the edited ones */
export function hoursChanges(
  saved: HoursWindow[],
  edited: HoursWindow[],
  locationId: string | null
): HoursChanges {
  const body = (window: HoursWindow): SpecialistAvailabilityWriteRequest => ({
    ...(locationId && { location_id: locationId }),
    day_of_week: window.day,
    start_time: window.start,
    end_time: window.end,
  })
  const kept = new Map(
    edited.filter((window) => window.id).map((window) => [window.id, window])
  )
  const savedById = new Map(saved.map((window) => [window.id, window]))

  return {
    remove: saved
      .filter((window) => window.id && !kept.has(window.id))
      .map((window) => window.id!),
    update: [...kept.values()]
      .filter((window) => {
        const before = savedById.get(window.id)
        return (
          before &&
          (before.day !== window.day ||
            before.start !== window.start ||
            before.end !== window.end)
        )
      })
      .map((window) => ({ id: window.id!, body: body(window) })),
    add: edited
      .filter((window) => !window.id)
      .map((window) => ({ key: window.key, body: body(window) })),
  }
}

/** A weekday's name in the page's language ("lundi"), Monday first */
export function weekdayName(day: WeekDay, locale: string): string {
  // 1 January 2024 was a Monday
  const date = Date.UTC(2024, 0, 1 + WEEK_DAYS.indexOf(day))
  return new Intl.DateTimeFormat(locale, {
    weekday: "long",
    timeZone: "UTC",
  }).format(date)
}

/** True when nothing would be sent */
export function isUnchanged(changes: HoursChanges): boolean {
  return (
    changes.remove.length === 0 &&
    changes.update.length === 0 &&
    changes.add.length === 0
  )
}
