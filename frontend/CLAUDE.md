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
- `pnpm build`, `pnpm lint`, `pnpm typecheck` run across all workspaces (Turborepo caches results)
- Add a dependency to one workspace: `pnpm add <pkg> --filter=web`
- Add a shadcn component (lands in `packages/ui`): `pnpm dlx shadcn@latest add <component> -c apps/web`
  Import it as `import { Button } from "@workspace/ui/components/button"`.

Before calling a change done, run `pnpm lint`, `pnpm typecheck` and `pnpm build`.

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

API docs live in `../docs/backend/` (auth, errors, architecture). The OpenAPI contract is the source of truth for request and response shapes. Local API: `http://localhost:8080`.

- Base path `/v1`. JSON keys are `snake_case`.
- Auth uses **HttpOnly cookies** (`access_token`, `refresh_token`). Tokens never appear in response bodies, so never try to read or store them in JS. Browser requests must send cookies (`credentials: "include"`).
- On 401, call `POST /v1/auth/refresh` (no body) once, then retry.
- Errors share one shape: `{ status, code, message, trace_id }`, plus `errors: [{ field, code, message }]` on 422. Types and `ApiError` live in `@workspace/api`. Branch on `code`, never on `message`.
- The backend allows CORS only from origins in `CORS_ALLOWED_ORIGINS` (or any localhost port when `ALLOW_LOCALHOST_ORIGINS=true`).

## Secrets

Never commit `.env*` files. Only `NEXT_PUBLIC_*` variables reach the browser, and they are fixed at build time; never put secrets in them.