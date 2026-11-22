import { hasLocale } from "next-intl"
import createMiddleware from "next-intl/middleware"
import { NextResponse, type NextRequest } from "next/server"

import { redirectFor } from "@/features/auth/session/route-access"
import {
  ACCESS_TOKEN_COOKIE,
  readAccessToken,
} from "@/features/auth/session/session-cookie"

import { routing } from "./i18n/routing"

const intlProxy = createMiddleware(routing)

/**
 * 1. Auth redirects for localized paths (/en/login, /ar/onboarding, ...):
 *    a quick check of the access cookie, as Next.js recommends for proxies.
 *    The backend still authorizes every API call; this only routes people.
 * 2. Everything else goes to next-intl, which adds the locale prefix, so the
 *    redirected request comes back here and gets step 1.
 *
 * Needs the access cookie to reach this server: true locally (cookies ignore
 * ports), and in production only once the backend sets the cookies on the
 * parent domain (or the API is served from the same origin).
 */
export default function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl
  const [, locale, ...rest] = pathname.split("/")

  if (hasLocale(routing.locales, locale)) {
    const session = readAccessToken(
      request.cookies.get(ACCESS_TOKEN_COOKIE)?.value
    )
    const target = redirectFor(`/${rest.join("/")}`, session, search)
    if (target) {
      return NextResponse.redirect(new URL(`/${locale}${target}`, request.url))
    }
  }

  return intlProxy(request)
}

export const config = {
  // Everything except Next internals, API routes and files with an extension
  matcher: "/((?!api|_next|_vercel|.*\\..*).*)",
}
