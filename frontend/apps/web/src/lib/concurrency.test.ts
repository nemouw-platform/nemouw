import { describe, expect, it } from "vitest"

import { mapWithLimit } from "@/lib/concurrency"

describe("mapWithLimit", () => {
  it("keeps the order and never runs more than the limit at once", async () => {
    let running = 0
    let most = 0
    const result = await mapWithLimit([5, 1, 3, 2, 4], 2, async (n) => {
      running++
      most = Math.max(most, running)
      await new Promise((resolve) => setTimeout(resolve, n))
      running--
      return n * 10
    })
    expect(result).toEqual([50, 10, 30, 20, 40])
    expect(most).toBe(2)
  })

  it("handles no items", async () => {
    expect(await mapWithLimit([], 3, async () => 1)).toEqual([])
  })
})
