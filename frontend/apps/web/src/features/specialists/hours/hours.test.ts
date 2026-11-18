import { describe, expect, it } from "vitest"

import {
  byDay,
  checkHours,
  hoursChanges,
  isUnchanged,
  toWindows,
  weekdayName,
  type HoursWindow,
} from "@/features/specialists/hours/hours"

const window = (
  key: string,
  day: HoursWindow["day"],
  start: string,
  end: string,
  id?: string
): HoursWindow => ({ key, id, day, start, end })

describe("toWindows / byDay", () => {
  const rows = [
    {
      id: "a",
      location_id: "clinic",
      day_of_week: "monday" as const,
      start_time: "15:00",
      end_time: "19:00",
    },
    {
      id: "b",
      location_id: "clinic",
      day_of_week: "monday" as const,
      start_time: "09:00",
      end_time: "13:00",
    },
    {
      id: "c",
      location_id: "other",
      day_of_week: "friday" as const,
      start_time: "09:00",
      end_time: "12:00",
    },
    {
      id: "d",
      location_id: null,
      day_of_week: "sunday" as const,
      start_time: "10:00",
      end_time: "11:00",
    },
  ]

  it("keeps one location's windows, or the ones tied to none", () => {
    expect(toWindows(rows, "clinic").map((w) => w.id)).toEqual(["a", "b"])
    expect(toWindows(rows, null).map((w) => w.id)).toEqual(["d"])
  })

  it("groups by weekday, earliest first, with every day present", () => {
    const days = byDay(toWindows(rows, "clinic"))
    expect(days.monday.map((w) => w.start)).toEqual(["09:00", "15:00"])
    expect(days.tuesday).toEqual([])
    expect(Object.keys(days)).toHaveLength(7)
  })
})

describe("checkHours", () => {
  it("accepts windows that follow each other", () => {
    expect(
      checkHours([
        window("1", "monday", "09:00", "13:00"),
        window("2", "monday", "13:00", "17:00"),
        window("3", "tuesday", "09:00", "13:00"),
      ])
    ).toEqual({})
  })

  it("flags missing times, reversed times and overlaps", () => {
    expect(
      checkHours([
        window("empty", "monday", "", "13:00"),
        window("reversed", "tuesday", "17:00", "09:00"),
        window("first", "wednesday", "09:00", "13:00"),
        window("overlaps", "wednesday", "12:00", "15:00"),
      ])
    ).toEqual({ empty: "missing", reversed: "order", overlaps: "overlap" })
  })

  it("catches a window inside an earlier, longer one", () => {
    expect(
      checkHours([
        window("long", "friday", "08:00", "18:00"),
        window("short", "friday", "09:00", "10:00"),
        window("after", "friday", "12:00", "13:00"),
      ])
    ).toEqual({ short: "overlap", after: "overlap" })
  })
})

describe("weekdayName", () => {
  it("names weekdays in the page's language", () => {
    expect(weekdayName("monday", "en")).toBe("Monday")
    expect(weekdayName("sunday", "fr")).toBe("dimanche")
  })
})

describe("hoursChanges", () => {
  const saved = [
    window("a", "monday", "09:00", "13:00", "a"),
    window("b", "tuesday", "09:00", "13:00", "b"),
    window("c", "wednesday", "09:00", "13:00", "c"),
  ]

  it("removes, updates and adds only what changed", () => {
    const edited = [
      window("a", "monday", "09:00", "13:00", "a"),
      window("b", "tuesday", "10:00", "14:00", "b"),
      window("new", "thursday", "09:00", "12:00"),
    ]
    expect(hoursChanges(saved, edited, "clinic")).toEqual({
      remove: ["c"],
      update: [
        {
          id: "b",
          body: {
            location_id: "clinic",
            day_of_week: "tuesday",
            start_time: "10:00",
            end_time: "14:00",
          },
        },
      ],
      add: [
        {
          key: "new",
          body: {
            location_id: "clinic",
            day_of_week: "thursday",
            start_time: "09:00",
            end_time: "12:00",
          },
        },
      ],
    })
  })

  it("leaves location_id out for hours tied to no location", () => {
    const changes = hoursChanges(
      [],
      [window("n", "monday", "09:00", "10:00")],
      null
    )
    expect(changes.add).toEqual([
      {
        key: "n",
        body: { day_of_week: "monday", start_time: "09:00", end_time: "10:00" },
      },
    ])
  })

  it("is unchanged when nothing differs", () => {
    expect(isUnchanged(hoursChanges(saved, saved, "clinic"))).toBe(true)
  })
})
