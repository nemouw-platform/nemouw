# Nemouw

A specialist discovery and referral platform for Lebanon, connecting patients with verified allied health professionals (speech, occupational, psychomotor therapy, special education, ...) and letting specialists refer to trusted colleagues.

> **Status:** early stage, under active development.

## Repository layout

| Folder               | What it is                                                                                     |
|----------------------|------------------------------------------------------------------------------------------------|
| `backend/`           | Spring Boot 4 API (Java 25, Maven, PostgreSQL, Flyway). Runs with Docker                       |
| `frontend/`          | pnpm + Turborepo monorepo with two Next.js apps (see [frontend/README.md](frontend/README.md)) |
| `docs/`              | Detailed documentation                                                                         |
| `docker-compose.yml` | Runs the backend and PostgreSQL for local development                                          |

## Run the backend (Docker only)

You only need Docker. No local Java, Maven or PostgreSQL.

```bash
# 1. Create the env files (once), then fill in the values, see the setup guide
cp .env.example .env
cp backend/.env.example backend/.env

# 2. Start the backend and the database
docker compose up -d

# 3. Run the tests
docker compose run --rm backend mvn test

# 4. Create the first user (interactive)
docker compose run --rm backend mvn spring-boot:run '-Dspring-boot.run.fork=false' '-Dspring-boot.run.arguments=--create-user'
```

The API is then on `http://localhost:8080/v1`. Step 1 needs a few required values or the app refuses to start.
The full walk-through (env values, API docs, activating the created user, troubleshooting) is in **[Backend setup and run guide](docs/backend/Setup.md)**.

## Documentation

| Document                                                               | Covers                                                               |
|------------------------------------------------------------------------|----------------------------------------------------------------------|
| [Backend setup and run guide](docs/backend/Setup.md)                   | Docker setup, env values, run, tests, create a user, troubleshooting |
| [Authentication](docs/backend/user/README.md)                          | Sign-up, login, sessions, password reset, cookies, security model    |
| [Error handling](docs/backend/errors/README.md)                        | Error response format, status codes, rate limiting                   |
| [Specialist discovery](docs/backend/specialist/Discovery.md)           | Public search, filters, sorting and specialist profiles              |
| [Package-by-feature architecture](docs/backend/architecture/README.md) | Package layout, module boundaries, conventions                       |
| [Frontend](frontend/README.md)                                         | Next.js apps and shared packages                                     |

The OpenAPI contract is the source of truth for endpoints: `backend/src/main/resources/static/openapi.yaml`. When `API_DOCS_ENABLED=true`, Swagger UI is served at `http://localhost:8080/swagger-ui.html`.