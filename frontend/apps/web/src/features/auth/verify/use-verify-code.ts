"use client"

import { useTranslations } from "next-intl"
import type { UseFormReturn } from "react-hook-form"

import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { classifyVerifyError } from "@/features/auth/verify/verify-errors"
import type { VerifyValues } from "@/features/auth/verify/verify-schema"
import { useRouter } from "@/i18n/navigation"
import { authApi } from "@/lib/api"

/**
 * Submits step 2. Success sets the short-lived setup cookie and moves to the
 * password step; a rejected code clears the boxes so the user can retype.
 */
export function useVerifyCode(
  email: string,
  form: UseFormReturn<VerifyValues>
) {
  const router = useRouter()
  const t = useTranslations("Verify.errors")

  const verify = useAuthRequest(
    (signal, code: string) => authApi.verifyOtp({ email, code }, { signal }),
    {
      // The email lets step 3 flag passwords that contain it
      onSuccess: () =>
        router.push({ pathname: "/signup/password", query: { email } }),
      onError: (error) => {
        if (classifyVerifyError(error) !== "invalidCode") return false
        form.setValue("code", "")
        form.setError(
          "code",
          { message: t("invalidCode") },
          { shouldFocus: true }
        )
        return true
      },
    }
  )

  return {
    submit: form.handleSubmit(({ code }) => verify.run(code)),
    isPending: verify.isPending,
    formError: verify.formError,
  }
}
