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

# Verify every service boots (Flyway + Hibernate ddl-auto=validate) against scratch DBs
# on the Docker Postgres (needs `make infra-up`, `make keys`, and packaged jars)
bash infra/scripts/validate-service-startup.sh            # all services
bash infra/scripts/validate-service-startup.sh admin farm # subset
```

Build caveats:
- Use a sequential `./mvnw clean install -DskipTests` whenever `shared-kernel` changed. A parallel
  `-T1C` build has been observed to compile services against a stale `~/.m2` shared-kernel and emit
  broken bytecode (`NoClassDefFoundError: OutboxPublisher` at runtime).
- All services run `spring.jpa.hibernate.ddl-auto=validate`; any entity/migration drift fails startup.
  Never edit an already-applied Flyway migration — add a new `V<prefix>NNN__*.sql` instead.

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

## Implementation status snapshot
- Backend reactor compiles and packages (`./mvnw -T1C clean package -DskipTests`).
- Identity, admin, parent-flock, hatchery, farm, slaughterhouse, notification, reporting, gateway all package.
- Identity has JWT, sessions, MFA/PIN, lockout, password policy + tests.
- Admin has roles/permissions, settings, catalogues, site-directory consumer, workforce core.
- Domain services publish `ppms.site.directory` events on site creation.
- Notification has email outbox (Thymeleaf + Mailpit), SSE stream, and identity event consumer.
- Reporting has SiteOverview/LotFact/KpiDaily read models and site-directory consumer.
- Gateway has OAuth2 JWT resource-server, CORS, and env-var based routes.
- Frontend foundation (React 18, Vite, MUI, i18n, auth, offline queue, PWA) builds and tests pass.
- CI workflow and basic Helm chart added.
- Prometheus + Grafana provisioning added.

## Kafka topics
- `ppms.site.directory` — site creation/updates (consumed by admin, reporting).
- `ppms.identity.user` — identity events (consumed by notification).
- `ppms.alerts` — operational alerts (consumed by notification).

## Remaining major work
- Full domain workflows: flocks, daily sheets, hatchery cycle, farm treatments/FCI/dispatch, slaughterhouse sessions/sales/invoicing.
- Additional reporting projections and dashboards.
- More frontend domain modules for hatchery/parent-flock/slaughterhouse/reporting.
- End-to-end integration tests with Testcontainers.
- Full Kubernetes production manifests and runbooks.
- Runtime integration verification against Docker Compose.
