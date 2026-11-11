@AGENTS.md

# Nemouw frontend (monorepo)

Web clients for Nemouw, a specialist discovery and referral platform for Lebanon (patients find verified allied health specialists; specialists refer to colleagues). The backend is a Spring Boot API in `../backend`.

This folder is a **pnpm + Turborepo monorepo**: two independent Next.js apps sharing internal packages.

## Layout

```
apps/
  web/     public site (patients & specialists)  -> domain.com,        dev port 3000
  admin/   staff dashboard (credential review)   -> admin.domain.com,  dev port 3001
packages/
  ui/                 shadcn components, Nova theme (globals.css), cn()   -> @workspace/ui
  api/                Spring Boot client: error types, fetch helpers      -> @workspace/api
  i18n/               supported locales, names, text direction            -> @workspace/i18n
  eslint-config/      shared ESLint configs
  typescript-config/  shared tsconfig bases
```

Inside each app, code lives in `src/`; `@/*` maps to that app's `src/*`:

- `src/app/` routes and layouts (keep them thin)
- `src/features/<feature>/` business logic per domain (auth, specialists, credentials, ...), mirroring the backend's package-by-feature layout
- `src/components/` app-specific components
- `src/hooks/`, `src/lib/` app-specific hooks and helpers

## Ownership and boundaries

- `apps/web` and `apps/admin` are owned by different people and deploy independently. **An app must never import from another app.** Code both apps need goes into `packages/`.
- A change in `packages/*` affects both apps; keep shared packages stable and check both apps when changing them.
- `packages/api` is **plain TypeScript**: no React, no `next/*` imports, no browser-only APIs beyond `fetch`. Next-specific glue (reading cookies on the server, redirects) belongs in the app.
- `packages/ui` holds generic UI primitives only, never business logic or API calls.
- `packages/i18n` is plain TypeScript too (no next-intl, no React). UI primitives take translated strings as props (e.g. `ThemeToggle label`), never translate themselves.

## Commands

Use **pnpm** only. Run from `frontend/` (the monorepo root):

- `pnpm dev` runs both apps (web :3000, admin :3001)
- `pnpm dev --filter=web` / `pnpm dev --filter=admin` runs one app
- `pnpm build`, `pnpm lint`, `pnpm typecheck`, `pnpm test` run across all workspaces (Turborepo caches results). Unit tests use Vitest (`*.test.ts` next to the code)
- `pnpm --filter web test:e2e` runs the Playwright end-to-end suite (`apps/web/e2e/`) against a production build and the real local backend; setup in `apps/web/e2e/README.md`. Run it after changing any auth flow
- Add a dependency to one workspace: `pnpm add <pkg> --filter=web`
- Add a shadcn component (lands in `packages/ui`): `pnpm dlx shadcn@latest add <component> -c apps/web`
  Import it as `import { Button } from "@workspace/ui/components/button"`.

Before calling a change done, run `pnpm lint`, `pnpm typecheck`, `pnpm test` and `pnpm build`.

## Stack

- Next.js 16 (App Router, `cacheComponents` on), React 19, TypeScript strict
- Tailwind CSS v4: no `tailwind.config`; theme tokens live in `packages/ui/src/styles/globals.css`
- shadcn/ui, style `base-nova`, built on **Base UI** (`@base-ui/react`), not Radix. Icons: `lucide-react`
- `cn()` from `@workspace/ui/lib/utils`; dark mode via `next-themes`

Next.js 16 differs from older versions. Check `node_modules/next/dist/docs/` (inside an app folder) before using an API you are not sure about (see AGENTS.md).

## Conventions

- Add UI primitives with the shadcn CLI instead of writing them by hand.
- Style with Tailwind classes and the theme's CSS variables (`bg-background`, `text-muted-foreground`, ...). No hard-coded colors.
- Prefer Server Components; add `"use client"` only where state, effects or browser APIs are needed.
- Follow SOLID with a few focused files: when a hook, component or module has several parts, give it a folder of about 4 to 6 files, split by responsibility (public `index.ts`, `types.ts`, framework-free core logic, pure state/helpers, the thin hook or component that wires them). See `packages/ui/src/hooks/use-request/` and `packages/api/src/client/`. Tiny helpers stay inside the file that uses them rather than getting their own file. Test the core logic and the public hook or component.
- Don't repeat yourself, especially in UI: when a second screen needs the same markup or logic (a step header, a submit button with spinner, an error alert, submit and error state), extract a shared component or hook and move the first caller onto it. Feature-wide pieces live in `src/features/<feature>/components` or `lib`; app-agnostic ones (generic hooks, primitives) in `packages/ui`.

## Internationalization

Locales are `en`, `fr` and `ar` (Arabic is right-to-left), matching the backend's `LocaleCode`. Both apps use [next-intl](https://next-intl.dev) with the locale in every URL (`/en/...`, `/fr/...`, `/ar/...`).

- Every route lives under `src/app/[locale]/`; that layout is the root layout and sets `<html lang dir>`. `src/proxy.ts` redirects paths without a locale (cookie `NEXT_LOCALE`, then `Accept-Language`, then `en`).
- No hard-coded user-facing text. Strings live in each app's `messages/{en,fr,ar}.json`; `en.json` is the source of the key types, so add a key there first, then to `fr` and `ar`.
- Read them with `useTranslations("Namespace")` (Server and Client Components) or `await getTranslations("Namespace")` in async server code and `generateMetadata`.
- Import `Link`, `redirect`, `useRouter` and `usePathname` from `@/i18n/navigation`, never from `next/link` / `next/navigation`, so links keep the current locale.
- Change the locale only with a full page load (as `src/components/locale-switcher.tsx` does), never `<Link locale>` or `router.replace(..., { locale })`: the locale is the root layout's param, and a client-side switch re-renders the root layout in the browser (React then warns about next-themes' inline `<script>`).
- Write direction-neutral styles: logical utilities (`ms-*`, `me-*`, `ps-*`, `pe-*`, `start-*`, `end-*`, `text-start`) instead of `ml-*`/`mr-*`/`left-*`/`text-left`, and `rtl:` for the rare exception (e.g. flipping an arrow icon).
- Arabic uses IBM Plex Sans Arabic instead of Geist (`src/lib/fonts.ts`).
- Send the current locale (`useLocale()` / `getLocale()`) as `locale` on sign-up and password-reset requests so emails arrive in the same language.

## Talking to the backend

API docs live in `../docs/backend/` (auth, errors, architecture). The OpenAPI contract (`../backend/src/main/resources/static/openapi.yaml`, Swagger UI at `http://localhost:8080/swagger-ui.html` when `API_DOCS_ENABLED=true`) is the source of truth for request and response shapes. Local API: `http://localhost:8080`.

- Call the backend through `@workspace/api`, never with raw `fetch`: `createApiClient({ baseUrl })` plus typed endpoint groups such as `createAuthApi(client)`. Paths are written without `/v1` (`client.get("/specialists/me")`).
- In Client Components, run calls through `useRequest` from `@workspace/ui/hooks/use-request`: it debounces (300 ms by default, `debounceMs: 0` to send at once), aborts the superseded request, and tracks `data` / `error` / `isPending`. Always pass its `signal` on (`(signal, body) => auth.signup(body, { signal })`). Use `run()` for fire-and-forget calls (search), `runAsync()` when you need the result (form submit; ignore rejections where `isAbortError(e)`), and `restart()` to repeat the last call at once (a "Try again" button).
- Types come from the contract: `pnpm --filter @workspace/api generate` rewrites `packages/api/src/generated/openapi.ts` (never edit it by hand); use named aliases from `types.ts` or `Schema<"Name">`. Regenerate whenever the backend changes `openapi.yaml`.
- Base path `/v1`. JSON keys are `snake_case`, in requests, responses and our types (no case conversion).
- Auth uses **HttpOnly cookies** (`access_token`, `refresh_token`). Tokens never appear in response bodies, so never try to read or store them in JS. The client sends `credentials: "include"`.
- On a 401 outside `/auth/*` the client calls `POST /v1/auth/refresh` once (shared by concurrent callers) and retries; if that fails it calls `onSessionExpired`. This covers `UNAUTHENTICATED` too: the browser drops `access_token` after its 15-minute Max-Age while the 90-day `refresh_token` is still valid. Server-side clients pass `autoRefresh: false` (rotated cookies could not reach the browser).
- Errors share one shape: `{ status, code, message, trace_id }`, plus `errors: [{ field, code, message }]` on 422. Every failure is thrown as `ApiError` (`status`, `code`, `fieldError(field)`, `retryAfter` from 429s), including `NETWORK_ERROR` (status 0) and `UNEXPECTED_RESPONSE` (no standard body). Branch on `code`, never on `message`.
- Local testing: with `MAIL_DELIVERY=file` and `DEV_FIXED_OTP=000000` in `backend/.env`, sign-up codes are always `000000` and emails are written to files instead of being sent.
- The backend allows CORS only from origins in `CORS_ALLOWED_ORIGINS` (or any localhost port when `ALLOW_LOCALHOST_ORIGINS=true`).

## Sessions and redirects (web)

- `src/proxy.ts` reads the access cookie's JWT payload (role, setup scope, expiry; the signature is the backend's job) and redirects by the table in `features/auth/session/route-access.ts`: protected pages (optionally per role) send signed-out visitors to `/login?next=…`, guest pages (login, sign-up) send signed-in users home, sign-up step 3 needs the password-setup token. A new protected page is one line in that table.
- The proxy only sees the access cookie (15 min). Returning users with a live refresh cookie land on the login page, which refreshes quietly and continues to `next` (`use-resume-session.ts`). `next` must pass `safeNextPath` (internal paths only).
- When an API call's 401 survives the refresh, the client signs out (clearing the cookies, retrying after a 429 since the rate limit counts the logout too) and loads `/login?next=…` (`session/end-session.ts`).
- Production needs the access cookie to reach the Next server: the backend must set it on the parent domain, or the API must be served from the same origin. Until then the proxy cannot see sessions there.

## Secrets

Never commit `.env*` files. Only `NEXT_PUBLIC_*` variables reach the browser, and they are fixed at build time; never put secrets in them.