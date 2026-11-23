# Bookstore Microservices

A bookstore application built with Spring Boot microservices, following a course-based learning approach.

## Services

| Service | Port | Description |
|---|---|---|
| catalog-service | 8071 | Product catalog (books) |

## Prerequisites

- Java 21 (see `.sdkmanrc` — run `sdk env` if you use SDKMAN)
- Docker (for PostgreSQL and Testcontainers-based tests)

## Setup

**1. Create your local environment file**

```bash
cp .env.example .env
```

Fill in real values (any values work — it's your local Docker Postgres).

**2. Start the infrastructure (PostgreSQL)**

```bash
docker compose --env-file .env -f deployment/docker-compose/infra.yml up -d
```

**3. Run the service with the `local` profile**

From your IDE — run `CatalogServiceApplication` with:
- Active profiles: `local`
- Working directory: the repository root (IntelliJ: `$PROJECT_DIR$`)

Or from the command line:

```bash
./mvnw -pl catalog-service spring-boot:run -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.workingDirectory=$(pwd)
```

> **Why the working directory matters:** the `local` profile imports `.env` via a
> relative path (`spring.config.import=optional:file:.env[.properties]` in
> `application-local.properties`). The app must run with the repo root as its
> working directory to find it — note that `spring-boot:run` defaults to the
> *module* directory, which is why the extra flag is needed on the CLI. If the
> file isn't found, startup fails with a database auth error showing the
> unresolved placeholder — that's your cue.

**4. Verify**

```bash
curl http://localhost:8071/actuator/health
```

API docs (Swagger UI): http://localhost:8071/swagger-ui.html

## How configuration works

- `application.properties` — shared, environment-agnostic settings (committed, no secrets)
- `application-local.properties` — local dev datasource, reads values from `.env` (committed, no secrets)
- `.env` — real local values for both the app and docker-compose (**gitignored, you create this**)
- `.env.example` — template documenting the required variables (committed)

In deployed environments, the same `${...}` placeholders are satisfied by real
environment variables injected by the platform — no `.env` file involved.

## Testing

```bash
./mvnw verify
```

Tests use Testcontainers — Docker must be running. Spotless (Palantir format)
is enforced at compile time; run `./mvnw spotless:apply` to auto-format.

## Project layout

```
├── catalog-service/          # Spring Boot service (Maven module)
├── deployment/
│   └── docker-compose/
│       ├── infra.yml         # PostgreSQL container
│       └── init/catalog-db/  # DB bootstrap SQL (role + database)
├── .env.example              # config template — copy to .env
└── pom.xml                   # Maven aggregator
```
