import type { ApiClient } from "../client"
import type { CallOptions } from "./types"
import type {
  ForgotPasswordRequest,
  LoginRequest,
  PasswordSetupResponse,
  ResendOtpRequest,
  ResetPasswordRequest,
  SessionResponse,
  SetPasswordRequest,
  SignupRequest,
  SignupResponse,
  VerifyOtpRequest,
} from "../types"

/**
 * Auth endpoints (docs/backend/user/README.md). Sign-up is three calls:
 * signup -> verifyOtp (sets a setup cookie) -> setupPassword (starts the session).
 */
export function createAuthApi(client: ApiClient) {
  return {
    /** 201, emails a 6-digit code. 409 EMAIL_TAKEN; 429 while the resend cooldown runs. */
    signup: (body: SignupRequest, options?: CallOptions) =>
      client.post<SignupResponse>("/auth/signup", body, options),

    /** 200 + setup cookie. Every failure is 400 INVALID_OR_EXPIRED_TOKEN. */
    verifyOtp: (body: VerifyOtpRequest, options?: CallOptions) =>
      client.post<PasswordSetupResponse>("/auth/verify-otp", body, options),

    /** 202 (also for unknown emails); 429 with Retry-After during the 60 s cooldown. */
    resendOtp: (body: ResendOtpRequest, options?: CallOptions) =>
      client.post<void>("/auth/resend-otp", body, options),

    /** Needs the setup cookie from verifyOtp. Activates the account and starts the session. */
    setupPassword: (body: SetPasswordRequest, options?: CallOptions) =>
      client.post<SessionResponse>("/auth/password/setup", body, options),

    /** 401 INVALID_CREDENTIALS; 403 ACCOUNT_SUSPENDED or ACCOUNT_NOT_VERIFIED. */
    login: (body: LoginRequest, options?: CallOptions) =>
      client.post<SessionResponse>("/auth/login", body, options),

    /**
     * Rotates both cookies using the refresh cookie and returns the session
     * (role included). 401 INVALID_TOKEN when there is no live session.
     */
    refresh: (options?: CallOptions) =>
      client.post<SessionResponse>("/auth/refresh", undefined, options),

    /** Always 204; clears both cookies. */
    logout: (options?: CallOptions) =>
      client.post<void>("/auth/logout", undefined, options),

    /** 202 (also for unknown emails); 429 with Retry-After when throttled. */
    forgotPassword: (body: ForgotPasswordRequest, options?: CallOptions) =>
      client.post<void>("/auth/password/forgot", body, options),

    /** 200; revokes every session of the account. 400 INVALID_OR_EXPIRED_TOKEN. */
    resetPassword: (body: ResetPasswordRequest, options?: CallOptions) =>
      client.post<void>("/auth/password/reset", body, options),
  }
}

export type AuthApi = ReturnType<typeof createAuthApi>
