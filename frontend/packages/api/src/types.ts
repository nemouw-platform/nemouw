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
export type SpecialistOnboardingRequest = Schemas["SpecialistOnboardingRequest"]
export type SyndicateSummary = Schemas["SyndicateSummary"]
