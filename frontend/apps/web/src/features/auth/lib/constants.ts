// Facts from the backend (docs/backend/user/README.md) that several screens use

/** Sign-up screens: details, email code, password */
export const SIGNUP_STEP_COUNT = 3

/** "Step N of 3." for each sign-up screen (keep in a plain module: server pages read it) */
export const SIGNUP_STEPS = {
  details: { current: 1, total: SIGNUP_STEP_COUNT },
  verify: { current: 2, total: SIGNUP_STEP_COUNT },
  password: { current: 3, total: SIGNUP_STEP_COUNT },
} as const

/** Digits in the emailed sign-up code */
export const OTP_LENGTH = 6

/** Minimum gap between two code emails; the backend answers 429 before that */
export const RESEND_COOLDOWN_SECONDS = 60

/** Where a new specialist goes once signed up: onboarding (username, syndicate) */
export const AFTER_SIGNUP_PATH = "/onboarding"

/** Where a signed-in, onboarded user lands after logging in (no dashboard yet) */
export const AFTER_LOGIN_PATH = "/"
