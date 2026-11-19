// Request and response shapes from the backend contract (openapi.yaml).
// Regenerate src/generated/openapi.ts with `pnpm --filter @workspace/api generate`
// whenever the backend changes the spec. JSON keys stay snake_case, as on the wire.
import type { components } from "./generated/openapi"

type Schemas = components["schemas"]

/** Any schema from the contract by name, e.g. Schema<"SpecialistProfile"> */
export type Schema<Name extends keyof Schemas> = Schemas[Name]

export type Role = Schemas["Role"]
export type SignupRole = Schemas["SignupRole"]
/** Locales the backend accepts (email language). Same values as @workspace/i18n. */
export type ApiLocale = Schemas["Locale"]

export type SignupRequest = Schemas["SignupRequest"]
export type SignupResponse = Schemas["SignupResponse"]
export type VerifyOtpRequest = Schemas["VerifyOtpRequest"]
export type PasswordSetupResponse = Schemas["PasswordSetupResponse"]
export type ResendOtpRequest = Schemas["ResendOtpRequest"]
export type SetPasswordRequest = Schemas["SetPasswordRequest"]
export type LoginRequest = Schemas["LoginRequest"]
export type SessionResponse = Schemas["SessionResponse"]
export type ForgotPasswordRequest = Schemas["ForgotPasswordRequest"]
export type ResetPasswordRequest = Schemas["ResetPasswordRequest"]

export type SpecialistResponse = Schemas["SpecialistResponse"]
export type Gender = Schemas["Gender"]
export type SpecialistVerificationStatus =
  Schemas["SpecialistVerificationStatus"]
export type SpecialistOnboardingRequest = Schemas["SpecialistOnboardingRequest"]
export type SpecialistUpdateRequest = Schemas["SpecialistUpdateRequest"]
/** A specialist's public text (title, headline, bio) in one locale */
export type SpecialistProfileResponse = Schemas["SpecialistProfileResponse"]
export type SpecialistProfileWriteRequest =
  Schemas["SpecialistProfileWriteRequest"]
export type SyndicateSummary = Schemas["SyndicateSummary"]

/** A stored public image: its URL changes on every upload */
export type ImageResponse = Schemas["ImageResponse"]

export type CredentialType = Schemas["CredentialType"]
export type CredentialStatus = Schemas["CredentialsVerificationStatus"]
export type CredentialResponse = Schemas["CredentialResponse"]
/** The fields of a credential (create and edit); the document is sent apart */
export type CredentialFields = Schemas["CredentialUpdateRequest"]
/** A short-lived link to a private document: open it, never store it */
export type DocumentAccessResponse = Schemas["DocumentAccessResponse"]

export type Country = Schemas["Country"]
/** A condition specialists can treat, named in each locale (label, label_fr, label_ar) */
export type Condition = Schemas["Condition"]
export type SpecialistLocation = Schemas["SpecialistLocation"]
export type SpecialistLocationWriteRequest =
  Schemas["SpecialistLocationWriteRequest"]
export type WeekDay = Schemas["WeekDay"]
/** One opening window on one weekday, "HH:mm" to "HH:mm" */
export type SpecialistAvailability = Schemas["SpecialistAvailability"]
export type SpecialistAvailabilityWriteRequest =
  Schemas["SpecialistAvailabilityWriteRequest"]
