# End-to-end tests

Playwright drives a production build of the web app against the **real local
backend**: sign-up, verify, password, onboarding, login, redirects, auto-login
and password reset.

## Prerequisites

1. Backend running on `http://localhost:8080` (`docker compose up -d` from the
   repo root) with these in `backend/.env`:
   - `MAIL_DELIVERY=file`: emails become `.eml` files in `backend/target/dev-mails`,
     which the reset test reads
   - `DEV_FIXED_OTP=000000`: every sign-up code is `000000`
   - `ALLOW_LOCALHOST_ORIGINS=true`
2. Reference data for onboarding (one country, two specialties, two syndicates):
   ```bash
   docker exec -i nemouw-db-1 psql -U postgres -d nemouw < apps/web/e2e/fixtures/dev-seed.sql
   ```
   Safe to run again. Without it the onboarding tests are skipped.
3. A browser: `pnpm --filter web exec playwright install chromium`, or reuse an
   installed one with `PLAYWRIGHT_CHANNEL=msedge` (or `chrome`).

## Run

From `frontend/`:

```bash
pnpm --filter web test:e2e                              # builds, starts :3100, runs
PLAYWRIGHT_CHANNEL=msedge pnpm --filter web test:e2e    # with installed Edge
pnpm --filter web test:e2e --ui                         # interactive
```

An app already running on `E2E_PORT` (default 3100) is reused instead of
rebuilt. Other settings: `E2E_OTP` (default `000000`), `E2E_MAIL_DIR`,
`E2E_BASE_URL`.

Each test signs up fresh `e2e-…@example.com` accounts, so runs never collide.
The tests run one at a time: the backend rate-limits per IP and per address.
