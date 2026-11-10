# Nemouw frontend

pnpm + Turborepo monorepo with two Next.js apps and shared packages.

| Workspace | What it is | Dev URL |
|---|---|---|
| `apps/web` | Public site for patients and specialists | http://localhost:3000 |
| `apps/admin` | Staff dashboard | http://localhost:3001 |
| `packages/ui` | shadcn/ui components and theme (`@workspace/ui`) | |
| `packages/api` | Spring Boot API client and types (`@workspace/api`) | |
| `packages/i18n` | Supported locales, names and text direction (`@workspace/i18n`) | |
| `packages/eslint-config`, `packages/typescript-config` | Shared tooling config | |

## Getting started

Create each app's env file from its example (once):

```bash
cp apps/web/.env.example apps/web/.env.local
cp apps/admin/.env.example apps/admin/.env.local
```

Then:

```bash
pnpm install
pnpm dev                    # both apps
pnpm dev --filter=web       # only the public site
pnpm dev --filter=admin     # only the admin dashboard
```

## Adding shadcn components

```bash
pnpm dlx shadcn@latest add button -c apps/web
```

Components are placed in `packages/ui/src/components` and are available to both apps:

```tsx
import { Button } from "@workspace/ui/components/button"
```

## Checks

```bash
pnpm lint
pnpm typecheck
pnpm build
```