"use client"

import type {
  SpecialistResponse,
  SpecialistUpdateRequest,
} from "@workspace/api"
import type { FieldValues, UseFormReturn } from "react-hook-form"

import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { specialistsApi } from "@/lib/api"
import { useFormRequest } from "@/lib/forms/use-form-request"

interface UpdateOptions<TValues extends FieldValues> {
  /** The PATCH body for the form's values */
  toRequest: (values: TValues) => SpecialistUpdateRequest
  /** The saved profile as the form's values: its new baseline */
  toValues: (specialist: SpecialistResponse) => TValues
  /**
   * Handles the failures the form understands (e.g. puts them on its fields)
   * and returns true; `setFormError` says something above the button
   */
  onError: (error: unknown, setFormError: (message: string) => void) => boolean
}

/**
 * Saves a form that changes part of the specialist's profile (PATCH
 * /specialists/me): every page then shows the saved profile, and the form
 * starts again from it. `saved` stays true until the next edit.
 */
export function useUpdateSpecialist<TValues extends FieldValues>(
  form: UseFormReturn<TValues>,
  { toRequest, toValues, onError }: UpdateOptions<TValues>
) {
  const { setSpecialist } = useSpecialist()

  const save = useFormRequest(
    (signal, values: TValues) =>
      specialistsApi.update(toRequest(values), { signal }),
    {
      onSuccess: (specialist) => {
        setSpecialist(specialist)
        form.reset(toValues(specialist))
      },
      onError: (error) =>
        onError(error, (message) => save.setFormError(message)),
    }
  )

  return {
    submit: form.handleSubmit((values) => save.run(values)),
    isPending: save.isPending,
    formError: save.formError,
    saved: save.succeeded && !form.formState.isDirty,
  }
}
