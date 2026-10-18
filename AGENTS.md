# PPMS — Agent Guide

Project: Poultry Production Management System (vertically integrated broiler integrator: parent flocks → hatchery → farms → slaughterhouse + sales).

## Repository layout
- `docs/plan/` — implementation plan (read `00-overview.md` first).
- `backend/` — Spring Boot 3.3 / Java 21 multi-module Maven project.
- `frontend/` — Vite + React + TypeScript + MUI (to be created).
- `infra/` — Docker Compose, k8s Helm, observability, scripts.

## Build & test commands
Run all commands from the repo root unless noted.

```bash
# Build everything (skip tests for speed during dev)
cd backend && ./mvnw -T1C clean package -DskipTests

# Run unit + integration tests (requires Docker for Testcontainers)
cd backend && ./mvnw -T1C verify -Pintegration

# Start local infrastructure (Postgres, Kafka, Mailpit, MinIO)
make infra-up

# Start infrastructure + all backend services + gateway
make up

# Stop everything
make down

# Generate JWT signing keys (one-time)
make keys

# Seed demo data (after services are up)
make seed
```

## Key conventions
- Package layout per service: `domain/`, `application/`, `infrastructure/`, `api/`, `config/`.
- Cross-context references are UUID columns only; no JPA `@ManyToOne` across contexts.
- Business rules live in the domain/application layer; controllers are thin.
- `@PreAuthorize` at service layer; controllers delegate.
- Every write is `@Audited` and every domain state change goes through the outbox → Kafka.
- Migrations: Flyway, prefix per service (`VID`, `VAD`, `VPF`, `VHA`, `VFA`, `VSL`, `VNO`, `VRP`).

## Local development database
Postgres 16 on `localhost:5432` with one database per service:
`ppms_identity`, `ppms_admin`, `ppms_parent_flock`, `ppms_hatchery`, `ppms_farm`, `ppms_slaughterhouse`, `ppms_notification`, `ppms_reporting`.

## Security
- JWT RS256 keys in `infra/compose/secrets/` (gitignored); generate with `make keys`.
- Never commit `.env`, `application-local.yml`, or `*.pem`.
- Gateway is the single origin for the SPA at `http://localhost:8080`.
