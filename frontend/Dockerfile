# One image per Next.js app of the monorepo, picked with --build-arg APP=web
# (or admin). docker compose builds both: see docs/docker/README.md.

# ---- Base: Node with pnpm (the version in package.json, through corepack) ----
FROM node:24-slim AS base
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
ENV NEXT_TELEMETRY_DISABLED=1
# Fetch pnpm without asking (no one is there to answer during a build)
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable
WORKDIR /repo


# ---- Dependencies: only the manifests, so installs stay cached until they change ----
FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/
COPY apps/admin/package.json apps/admin/
COPY packages/api/package.json packages/api/
COPY packages/ui/package.json packages/ui/
COPY packages/i18n/package.json packages/i18n/
COPY packages/eslint-config/package.json packages/eslint-config/
COPY packages/typescript-config/package.json packages/typescript-config/
RUN --mount=type=cache,id=pnpm,target=/pnpm/store \
    corepack install && pnpm install --frozen-lockfile


# ---- Build: one app, as a standalone server ----
FROM deps AS build
ARG APP
# Called by the BROWSER, so it is the backend's address as seen from your
# machine, not the compose service name. Inlined into the bundle at build time.
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_OUTPUT=standalone
COPY . .
RUN test -n "$APP" || (echo "Pass --build-arg APP=web or APP=admin" && exit 1)
RUN pnpm --filter "$APP" build


# ---- Runtime: the minimal server and its static files, nothing else ----
FROM node:24-slim AS runtime
ARG APP
ENV APP=$APP
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
WORKDIR /app

# The standalone folder mirrors the monorepo: the server is at apps/<app>/server.js
COPY --from=build --chown=node:node /repo/apps/$APP/.next/standalone ./
COPY --from=build --chown=node:node /repo/apps/$APP/.next/static ./apps/$APP/.next/static
COPY --from=build --chown=node:node /repo/apps/$APP/public ./apps/$APP/public

USER node
EXPOSE 3000
CMD ["sh", "-c", "exec node apps/$APP/server.js"]
