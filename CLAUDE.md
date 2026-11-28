# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Maven aggregator (`packaging=pom`) over two independently deployable Spring Boot 4.2 / Java 21 services:

| Module | Port (`local`) | Responsibility |
|---|---|---|
| `catalog-service` | 8071 | Book catalog (offset + cursor pagination) |
| `order-service` | 8072 (`APP_PORT`) | Order creation + RabbitMQ scaffolding |

The root `pom.xml` is only an aggregator — it does **not** parent the modules. Each module inherits `spring-boot-starter-parent` directly and repeats its build/plugin config, so plugin and dependency changes usually must be made in each module POM.

## Commands

```bash
./mvnw verify                                   # build + test all modules
./mvnw -pl catalog-service verify               # one module
./mvnw -pl order-service test -Dtest=OrderServiceApplicationTests   # single test class
./mvnw spotless:apply                           # auto-format (Palantir)
```

- Spotless `check` is bound to the `compile` phase: any compile/test run fails on formatting violations. Run `spotless:apply` first when a build fails on style.
- Tests use Testcontainers (`@ServiceConnection`), so Docker must be running. `order-service` starts PostgreSQL *and* RabbitMQ containers.
- CI (`.github/workflows/catalog-service.yml`) currently covers only `catalog-service`, triggered on path changes and publishing an image on `main`.

### Running locally

```bash
cp .env.example .env
docker compose --env-file .env -f deployment/docker-compose/infra.yml up -d

./mvnw -pl catalog-service spring-boot:run -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.workingDirectory=$(pwd)
./mvnw -pl order-service spring-boot:run -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.workingDirectory=$(pwd)
```

**Working-directory gotcha:** `application-local.properties` imports `optional:file:.env[.properties]`, resolved relative to the process working directory. `spring-boot:run` defaults to the module directory, so the `workingDirectory=$(pwd)` flag is required when running from the repo root. `catalog-service` has no defaults for `CATALOG_DB_USER` / `CATALOG_DB_PASSWORD` and will fail startup without `.env`; `order-service` falls back to defaults and reads `ORDER_DB_LOCAL_URL` / `ORDER_DB_USER` / `ORDER_DB_PWD`.

- Health: `http://localhost:8071/actuator/health` (catalog), `http://localhost:8072/actuator/health` (order)
- Swagger UI: `/{port}/swagger-ui.html`
- Container image: `./mvnw -pl <module> spring-boot:build-image` → `jeppu/bookstore-<artifactId>`

## Architecture

### Service boundaries and data ownership
- No shared code or schema between services. Each owns its PostgreSQL database: `catalog-db` on host port 15432 and `order-db` on 25432 (both containers in `deployment/docker-compose/infra.yml`, plus RabbitMQ on 5672 / management 15672).
- `deployment/docker-compose/init/<service>-db/01-schema.sql` runs only on first container start and creates the `sujay` role and database. Tables and seed data are **Flyway's** job (`src/main/resources/db/migration`).
- `spring.jpa.hibernate.ddl-auto=none` — schema changes require a new Flyway migration, never entity annotations alone.

### Code layout and layering
- `dev.jeppu.web.controller` — REST controllers; `dev.jeppu.domain` — business logic and persistence; `dev.jeppu.config` — configuration.
- `catalog-service` groups by aggregate: `domain/product` (record `Product`, `ProductEntity`, mapper, repository, service, pagination records) and `domain/exception`.
- `order-service` is mid-refactor: `OrderEntity`/`OrderItemEntity`/`OrderService`/`OrderRepository` live directly in `dev.jeppu.domain`, while request/response DTO records are in `domain/models`.
- Domain records serve as API contracts; JPA entities are separate and package-private where possible (`ProductEntity`). Static mappers (`ProductMapper`, `OrderMapper`) convert between them.

### Framework specifics (Spring Boot 4)
- Use `spring-boot-starter-webmvc` (not `-web`).
- Jackson 3 is under `tools.jackson.*`; e.g. `tools.jackson.databind.json.JsonMapper` (not `com.fasterxml`). The catalog cursor codec and RabbitMQ converter both use it.
- Entities use `GenerationType.SEQUENCE` with `allocationSize = 50`, matching the `increment by 50` SQL sequences; orders also use `@Version` optimistic locking.
- API errors use `ProblemDetail` (`GlobalExceptionHandler` in catalog): invalid cursor → 400, not found → 404, duplicate → 409.

### Catalog pagination
- Offset: `GET /api/v1/products?page=N` (1-based at the API, converted to Spring Data's 0-based in one place).
- Cursor: `GET /api/v1/products/cursor?cursor=<token>`. The cursor is Base64 URL-encoded JSON `{"v":1,"code":"..."}`, kept opaque/versioned on purpose. The keyset query uses the unique `code` column, fetches `size + 1` rows, and encodes the **last returned** row (not the sentinel). `docs/pagination-cursor-vs-offset.md` is the design rationale — preserve these invariants when editing.

### Order service / messaging
- `POST /api/v1/orders` validates `CreateOrderRequest` (`@Valid`), maps it to `OrderEntity` with a UUID `orderNumber` and `NEW` status, and sets `username` from `SecurityService`. `SecurityService` is a stub returning `"user"`, a placeholder for real Spring Security.
- `ApplicationPropertiesConfig` (`@ConfigurationProperties("orders")`, enabled by `@ConfigurationPropertiesScan`) maps `application.properties` to exchange/queue names. `RabbitMQConfig` declares a direct exchange (`orders-exchange`), four durable queues (`new-orders`, `delivered-orders`, `cancelled-orders`, `error-orders`), bindings whose routing keys are the queue names, and a Jackson message converter.
- Order events are **not published yet**. `RabbitMqDemoController` (`POST /api/send`) and `RabbitMqDemoListener` are scratch scaffolding that use the exchange/queues; treat them as demo code.
- Known mismatch: `OrderStatus` includes `PAID`, but the V1 migration's `status` check constraint allows only `NEW/SHIPPED/DELIVERED/CANCELLED`; persisting `PAID` will violate the constraint until a migration adds it.
