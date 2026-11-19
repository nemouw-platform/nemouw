# Running Nemouw with Docker

One command starts the whole platform on your machine: the database, the
backend API, the public site and the staff dashboard. You only need Docker:
no Node.js, Java or Maven.

| Service   | What it is                                   | Open                                   |
| --------- | -------------------------------------------- | -------------------------------------- |
| `web`     | Public site: patients and specialists         | http://localhost:3000                  |
| `admin`   | Staff dashboard                               | http://localhost:3001                  |
| `backend` | Spring Boot API (debugger on port 5005)       | http://localhost:8080                  |
| `db`      | PostgreSQL 18, reachable from your machine only | `localhost:5432`                     |
| `seed`    | Loads the reference data (Lebanon, syndicates, conditions) once, then exits | (no page) |

The frontend apps run as **production builds**, so they behave like the real
site. To see code changes in them, rebuild (see [Everyday commands](#everyday-commands)).
The backend runs in development mode with live reload.

## First run

### 1. Install

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (it includes Docker Compose), started.
- Git.

### 2. Get the code

```bash
git clone <repository url> nemouw
cd nemouw
git switch frontend/specialist-dashboard   # the branch with the work to look at
```

### 3. Create the two settings files

Both files hold secrets and are never committed.

**`.env`** (repository root): the database.

```bash
cp .env.example .env
```

Change `POSTGRES_PASSWORD` to any password, and remove the inline comments.

**`backend/.env`**: the backend.

```bash
cp backend/.env.example backend/.env
```

Then set these values (remove the inline comments too):

| Setting                   | Local value                    | Why |
| ------------------------- | ------------------------------ | --- |
| `URL_BASE`                | `http://localhost:3000`        | Links in emails (password reset) open the local site |
| `JWT_SECRET`              | a new secret                   | Signs sessions. Generate: `openssl rand -base64 48` |
| `OTP_HASH_SECRET`         | another new secret             | Same command, a different value |
| `COOKIE_SECURE`           | `false`                        | Local sites run on http, so cookies cannot require https |
| `ALLOW_LOCALHOST_ORIGINS` | `true`                         | Lets the sites on ports 3000 and 3001 call the API |
| `MAIL_DELIVERY`           | `file`                         | Emails are written to files instead of being sent |
| `DEV_FIXED_OTP`           | `000000`                       | Every sign-up code is `000000` |
| `API_DOCS_ENABLED`        | `true` (optional)              | Swagger UI at http://localhost:8080/swagger-ui.html |

**Photo and document uploads** use the team's staging S3 bucket. Ask a
teammate for these values privately (never by email, chat history or a
commit) and set them in `backend/.env`:

```
MEDIA_BUCKET=...
AWS_REGION=...
MEDIA_PUBLIC_BASE_URL=...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

Without them everything works except uploading photos and credentials.

Without OpenSSL (e.g. plain Windows), generate a secret in PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 4. Start everything

```bash
docker compose up -d --build
```

The first start takes a while (about 5 to 10 minutes): Docker builds the two
frontend images and the backend downloads its Maven dependencies. Later starts
take seconds. Follow the backend until it is ready:

```bash
docker compose logs -f backend    # wait for "Started NemouwBackendApplication", then Ctrl+C
```

Then open http://localhost:3000.

## Trying it out

1. **Sign up** at http://localhost:3000/en/signup with any email address.
   The code is `000000`, and nothing is really sent: emails are written to
   `backend/target/dev-mails/` as `.eml` files (open them to follow a
   password-reset link).
2. **Onboard**: pick a username and a syndicate (Lebanon and two syndicates
   are loaded by the `seed` service) with any member number.
3. **Dashboard**: profile, photos, credentials, locations and opening hours.

Pages exist in English, French and Arabic: change the language from the globe
in the top bar, or go to `/fr/...` or `/ar/...`.

## Everyday commands

Run them from the repository root.

| To...                                         | Run |
| --------------------------------------------- | --- |
| See the latest work after `git pull`          | `docker compose up -d --build` |
| Rebuild only one frontend app                 | `docker compose up -d --build web` (or `admin`) |
| See what is running                           | `docker compose ps` |
| Read a service's logs                         | `docker compose logs -f backend` (or `web`, `admin`, `db`) |
| Stop everything (data is kept)                | `docker compose down` |
| Start over with an empty database             | `docker compose down -v`, then `docker compose up -d --build` |
| Open a database shell                         | `docker compose exec db psql -U postgres -d nemouw` |

Use the user and database names from your `.env` if you changed them.

### Other ports

If a port is taken (for example a local `pnpm dev` already uses 3000), set
other ones in the root `.env`:

```
WEB_PORT=3100
ADMIN_PORT=3101
```

### Backend development

- `docker compose watch` rebuilds the backend container when a Java file or
  `pom.xml` changes.
- Attach a Java debugger (IntelliJ: Remote JVM Debug) to `localhost:5005`.

### Frontend development

Docker builds the frontend like production, which is right for looking at the
work but slow for changing it. To work on the frontend, keep the database and
backend in Docker and run the apps with pnpm instead (hot reload): see
[`frontend/README.md`](../../frontend/README.md).

```bash
docker compose up -d db backend seed
cd frontend && pnpm install && pnpm dev
```

## How it is put together

- [`docker-compose.yml`](../../docker-compose.yml) defines the five services.
- [`backend/Dockerfile`](../../backend/Dockerfile): Compose uses its `dev`
  stage (Maven, the source mounted from your machine). Its later stages build
  the small production image.
- [`frontend/Dockerfile`](../../frontend/Dockerfile) builds one image per app
  (`--build-arg APP=web` or `admin`): it installs the pnpm workspace, builds the
  app as a Next.js
  [standalone server](https://nextjs.org/docs/app/api-reference/config/next-config-js/output),
  and keeps only that server and its static files.
- `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`) is the backend
  address **as your browser sees it**, not the `backend` service name: the
  browser calls the API directly. It is baked in at build time, so changing it
  needs a rebuild:
  `NEXT_PUBLIC_API_URL=http://192.168.1.20:8080 docker compose up -d --build web`.
- The `seed` service waits for the backend (which creates the tables on
  start), then runs
  [`dev-seed.sql`](../../frontend/apps/web/e2e/fixtures/dev-seed.sql). The script
  is safe to run again, so it runs on every `up`.

## Troubleshooting

| Problem | Fix |
| ------- | --- |
| `port is already allocated` | Something else uses that port: stop it, or [pick other ports](#other-ports). |
| The site loads but every action says it cannot reach Nemouw | The backend is still starting (first start: several minutes): `docker compose logs -f backend`. |
| Signed in, but the next page sends you back to login | Use `localhost` in the address bar, not `127.0.0.1`: the session cookies belong to `localhost`. Check `COOKIE_SECURE=false`. |
| The backend exits at once | A required setting is empty. Its log names it: `docker compose logs backend`. |
| No syndicate to pick during onboarding | The seed did not run: `docker compose logs seed`, then `docker compose up seed`. |
| Uploading a photo or credential fails | The media settings in `backend/.env` are missing or wrong ([step 3](#3-create-the-two-settings-files)). |
| Your code changes do not show on the site | The frontend images are production builds: `docker compose up -d --build web`. |
