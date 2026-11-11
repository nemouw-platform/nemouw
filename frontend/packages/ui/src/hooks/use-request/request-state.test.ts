import { describe, expect, it } from "vitest"

import {
  createInitialState,
  requestReducer,
} from "@workspace/ui/hooks/use-request/request-state"
import type { RequestState } from "@workspace/ui/hooks/use-request/types"

type State = RequestState<string, Error>
const idle = createInitialState<string, Error>()
const failure = new Error("boom")

describe("requestReducer", () => {
  it("started keeps data and error while pending", () => {
    const state: State = { status: "error", data: "old", error: failure }

    expect(requestReducer(state, { type: "started" })).toEqual({
      status: "pending",
      data: "old",
      error: failure,
    })
  })

  it("started returns the same object when already pending", () => {
    const pending: State = { ...idle, status: "pending" }

    expect(requestReducer(pending, { type: "started" })).toBe(pending)
  })

  it("succeeded stores data and clears the error", () => {
    const state: State = { status: "pending", data: "old", error: failure }

    expect(requestReducer(state, { type: "succeeded", data: "new" })).toEqual({
      status: "success",
      data: "new",
      error: undefined,
    })
  })

  it("failed stores the error and keeps data", () => {
    const state: State = { status: "pending", data: "old", error: undefined }

    expect(requestReducer(state, { type: "failed", error: failure })).toEqual({
      status: "error",
      data: "old",
      error: failure,
    })
  })

  it.each([
    [{ data: undefined, error: undefined }, "idle"],
    [{ data: "old", error: undefined }, "success"],
    [{ data: "old", error: failure }, "error"],
  ] as const)(
    "cancelled while pending with %o falls back to %s",
    (rest, status) => {
      const pending: State = { status: "pending", ...rest }

      expect(requestReducer(pending, { type: "cancelled" }).status).toBe(status)
    }
  )

  it("cancelled leaves a settled state untouched", () => {
    const settled: State = { status: "success", data: "x", error: undefined }

    expect(requestReducer(settled, { type: "cancelled" })).toBe(settled)
  })

  it("reset returns to idle with no data or error", () => {
    const state: State = { status: "error", data: "x", error: failure }

    expect(requestReducer(state, { type: "reset" })).toEqual(idle)
  })
})
