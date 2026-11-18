"use client"

import { useTranslations } from "next-intl"
import type { UseFormReturn } from "react-hook-form"

import {
  DETAILS_FIELDS,
  toDetailsValues,
  toUpdateRequest,
  type DetailsValues,
} from "@/features/specialists/details/details-schema"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { specialistsApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

/** Saves the details form; the saved profile becomes the form's new baseline */
export function useSaveDetails(form: UseFormReturn<DetailsValues>) {
  const { setSpecialist } = useSpecialist()
  const t = useTranslations("Form.errors")

  const save = useFormRequest(
    (signal, values: DetailsValues) =>
      specialistsApi.update(toUpdateRequest(values), { signal }),
    {
      onSuccess: (specialist) => {
        setSpecialist(specialist)
        form.reset(toDetailsValues(specialist))
      },
      onError: (error) => applyFieldErrors(form, error, DETAILS_FIELDS, t),
    }
  )

  return {
    submit: form.handleSubmit((values) => save.run(values)),
    isPending: save.isPending,
    formError: save.formError,
    // Until the next edit
    saved: save.succeeded && !form.formState.isDirty,
  }
}
