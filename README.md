# Nemouw

A specialist discovery and referral platform for Lebanon, connecting patients with verified allied health professionals (speech, occupational, psychomotor therapy, special education, ...) and letting specialists refer to trusted colleagues.

> **Status:** early stage, under active development.

## Run it locally

With Docker installed, one command starts the database, the backend, the public
site (http://localhost:3000) and the staff dashboard (http://localhost:3001):

```bash
docker compose up -d --build
```

It needs two settings files first: see [Running Nemouw with Docker](docs/docker/README.md).

## Documentation

- [Running Nemouw with Docker](docs/docker/README.md)
- [Authentication](docs/backend/user/README.md)
- [Error handling](docs/backend/errors/README.md)
- [Package-by-feature architecture](docs/backend/architecture/README.md)