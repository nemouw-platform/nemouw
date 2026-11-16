# Backend setup and run guide

How to run the Nemouw backend with Docker only. You need Docker with Compose v2 (`docker compose`). Nothing else.

Run every command from the repository root (the folder with `docker-compose.yml`).

## 1. Create the env files (once)

```bash
cp .env.example .env
cp backend/.env.example backend/.env
```

Both files are git-ignored. Do not commit them.

### 1.1 Root `.env` (database)

Works as it is for local development. Change `POSTGRES_PASSWORD` if you want, and remove the inline `# comments`.

```env
APP_ENV=dev
LOG_LEVEL=debug
DB_URL=jdbc:postgresql://db:5432/nemouw
POSTGRES_DB=nemouw
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```

### 1.2 `backend/.env` (application)

This is a working local setup. Replace the two secrets.

```env
# ---- No default in application.properties: the app refuses to start without them ----
URL_BASE=http://localhost:3000
JWT_SECRET=<paste a generated value>          # openssl rand -base64 48, 32+ characters
OTP_HASH_SECRET=<paste a different value>     # openssl rand -base64 48, 32+ characters
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
# Media (S3): placeholders are fine until you upload files
MEDIA_BUCKET=nemouw-local
AWS_REGION=eu-central-1
MEDIA_PUBLIC_BASE_URL=http://localhost:9000/nemouw-local

# ---- Have a default, but the default does not suit local development ----
COOKIE_SECURE=false          # default true: browsers drop the cookies on plain http
ALLOW_LOCALHOST_ORIGINS=true # default false
MAIL_DELIVERY=file           # default live: needs RESEND_API_KEY. "file" writes .eml files, sends nothing (section 5)

# ---- Optional ----
API_DOCS_ENABLED=true        # Swagger UI at http://localhost:8080/swagger-ui.html
```

Remove the `# comments` from the real file.

Values without a default are empty in `application.properties` (`${JWT_SECRET:}`), and an empty value fails validation: the secrets must be 32+ characters, and `URL_BASE`, `CORS_ALLOWED_ORIGINS`, `MEDIA_BUCKET`, `AWS_REGION` and `MEDIA_PUBLIC_BASE_URL` must not be blank. If any is missing, the startup error lists all of them at once.

Everything else (`TRUST_PROXY`, token lifetimes, rate limit, log settings, `MAIL_FROM`) has a working default. `backend/.env.example` lists them.

## 2. Start the backend and the database

```bash
docker compose up -d
```

This builds the image the first time (a few minutes), starts PostgreSQL, waits until it is healthy, starts the backend, and Flyway creates the database tables automatically.

- API: `http://localhost:8080/v1`
- Debugger port: `5005`
- PostgreSQL: `127.0.0.1:5432` (user, password and database from `.env`)

Useful commands:

```bash
docker compose logs -f backend   # follow the backend logs
docker compose ps                # see what is running
docker compose down              # stop (data is kept)
docker compose down -v           # stop and DELETE the database
```

Quick check that it is up: `curl http://localhost:8080/v1/countries` should answer with JSON (an empty list is fine).

### Developing with automatic reload

```bash
docker compose up --watch
```

Changing a file in `backend/src/main/java` or `backend/pom.xml` rebuilds and restarts the backend by itself. Use this instead of `docker compose up -d` while you code.

## 3. Run the tests

```bash
docker compose run --rm backend mvn test
```

This starts the database if needed and runs all tests. `NemouwBackendApplicationTests` loads the full application, so it needs the `.env` files from step 1 and uses the same dev database as the running app.

Run one test class:

```bash
docker compose run --rm backend mvn test -Dtest=PasswordPolicyTest
```

## 4. Create a user

The backend has a command that creates an account and sends the person an invitation. Roles: `admin`, `specialist`, `patient`.

```bash
docker compose run --rm backend mvn spring-boot:run '-Dspring-boot.run.fork=false' '-Dspring-boot.run.arguments=--create-user'
```

It asks:

```
Role (admin, specialist, patient): admin
Email: you@example.com
First name: Riwa
Last name: Hammoud
Invite email language (en, ar, fr) [en]: en
Send a admin invitation to you@example.com? [y/N]: y
Invitation sent to you@example.com.
```

Notes:

- Some of the app boots for this command, so expect about 20 seconds.
- The account is created as `pending`. It becomes `active` when the person sets a password through the invitation link (next section).

## 5. Activate the user (first login)

With `MAIL_DELIVERY=file` no real email is sent. The invitation is written to a file on your machine:

```
backend/target/dev-mails/<timestamp>-<id>-<email>.eml
```

1. Open the newest `.eml` file in any mail client or text editor. It contains a link like `http://localhost:3000/reset-password#token=XXXX`.
2. Copy the value after `#token=` (it is URL-encoded, decode it if it contains `%`).
3. Set the password (valid for 24 hours):

   ```bash
   curl -X POST http://localhost:8080/v1/auth/password/reset \
     -H "Content-Type: application/json" \
     -d '{"token":"XXXX","new_password":"a-strong-password-here"}'
   ```

   The password needs 8 to 64 characters and must not be a common password.
4. Log in (the tokens come back as HttpOnly cookies, not in the body):

   ```bash
   curl -i -X POST http://localhost:8080/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"identifier":"you@example.com","password":"a-strong-password-here"}'
   ```

To send real emails instead, set `MAIL_DELIVERY=live`, `RESEND_API_KEY` and `MAIL_FROM` in `backend/.env`, then restart.

## 6. Complete flow, from zero

```bash
cp .env.example .env
cp backend/.env.example backend/.env
# edit backend/.env as in section 1.2 (secrets, CORS, media placeholders)

docker compose up -d                                   # start backend + database
docker compose logs -f backend                         # wait for "Started NemouwBackendApplication"
docker compose run --rm backend mvn test               # optional: run the tests
docker compose run --rm backend mvn spring-boot:run '-Dspring-boot.run.fork=false' '-Dspring-boot.run.arguments=--create-user'  # create a user
# open backend/target/dev-mails/*.eml, copy the token, set the password, log in (section 5)
```

## 7. Troubleshooting

| Problem                                                                | Fix                                                                                                             |
|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Backend exits right away with validation errors about blank properties | A value without a default is empty in `backend/.env` (section 1.2). Fix it and run `docker compose up -d` again |
| `POSTGRES_DB` variable is not set warning                              | `.env` is missing in the repository root. Copy `.env.example`                                                   |
| `JWT_SECRET` or `OTP_HASH_SECRET` rejected                             | Each must be at least 32 characters. Generate with `openssl rand -base64 48`                                    |
| Port 8080 or 5432 already in use                                       | Stop the other program using it, or change the port mapping in `docker-compose.yml`                             |
| `No interactive terminal available` on create-user                     | Run it from a real terminal with `docker compose run --rm ...`, not through a script or CI                      |
| Browser requests fail with a CORS error                                | Add the frontend origin to `CORS_ALLOWED_ORIGINS` and restart                                                   |
| Login works with curl but not in the browser                           | Send requests with `credentials: 'include'` and keep `COOKIE_SECURE=false` on plain `http`                      |
| Start over with an empty database                                      | `docker compose down -v`, then `docker compose up -d`                                                           |