# Running PPMS locally on Windows

Everything below was verified on Windows with PowerShell. All commands are run from the
repository root unless stated otherwise.

## 1. Prerequisites

| Tool | Why | Check |
|---|---|---|
| Docker Desktop (with Compose v2) | Postgres, Kafka, Mailpit, MinIO and all backend services run in containers | `docker version`, `docker compose version` |
| Node.js 20+ and `pnpm` | Frontend dev server | `node -v`, `pnpm -v` |
| OpenSSL | One-time JWT key generation (ships with Git for Windows: `C:\Program Files\Git\usr\bin\openssl.exe`) | `openssl version` |
| Git for Windows | Provides Git Bash for the `*.sh` helper scripts and `make` targets | `git --version` |
| Java 21 + Maven wrapper (optional) | Only if you build or run services outside Docker (`backend\mvnw.cmd`) | `java -version` |

`make` is **not** required; every Makefile target has a plain command listed below.

## 2. One-time setup

```powershell
# 1. Environment file for Docker Compose (edit ports/passwords if needed)
Copy-Item infra\compose\.env.example infra\compose\.env

# 2. JWT RS256 key pair (written to infra\compose\secrets\, gitignored)
bash infra/scripts/gen-jwt-keys.sh
#   or without Git Bash:
#   New-Item -ItemType Directory -Force infra\compose\secrets | Out-Null
#   openssl genrsa -out infra\compose\secrets\jwt-private.pem 2048
#   openssl rsa -in infra\compose\secrets\jwt-private.pem -pubout -out infra\compose\secrets\jwt-public.pem
```

Values worth knowing from `.env`:

- `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_PORT` — the Postgres container credentials and the
  host port. The `psql` commands below assume these values.
- `PPMS_INTERNAL_SECRET` — shared HMAC secret for identity → admin internal calls (any string in dev).
- `KAFKA_UI_PORT`, `MAILPIT_UI_PORT`, `MINIO_CONSOLE_PORT`, `GRAFANA_PORT` — UI ports.

## 3. Start the backend (Docker Compose)

```powershell
cd infra\compose

# Infrastructure only (Postgres, Kafka + topic init, Kafka UI, Mailpit, MinIO)
docker compose -f docker-compose.yml up -d

# Infrastructure + all 8 services + gateway. First run builds the images (several minutes).
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile services up -d --build

# Watch health; wait until every service shows "(healthy)"
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile services ps

# Logs
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile services logs -f

cd ..\..
```

The API gateway is the single entry point at **http://localhost:8080**
(`http://localhost:8080/actuator/health` should return `{"status":"UP"}`).

Optional observability stack (Prometheus, Grafana, Loki):

```powershell
cd infra\compose; docker compose -f docker-compose.yml --profile observability up -d; cd ..\..
```

## 4. Create a login user

On a fresh database the identity service creates a bootstrap administrator automatically:

- **`admin@ppms.local` / `Admin123!`** (override with `PPMS_BOOTSTRAP_ADMIN_EMAIL` / `PPMS_BOOTSTRAP_ADMIN_PASSWORD`)

It receives the `SYSTEM_ADMIN` role through the admin-service seed, which gives full access everywhere.

To add another `SYSTEM_ADMIN` (`alin.ciobanu@ppms.ro`), run the provided SQL against the Postgres
container (adjust `-U` to your `POSTGRES_USER`):

```powershell
Get-Content infra\scripts\create-admin-user.sql -Raw | docker exec -i ppms-postgres psql -U postgres -d postgres
```

To reset any user's password to `Admin123!`:

```powershell
$hash = '$2a$10$woFSKKY6qky4QBJYPeCB4.MW2ZCn0T7zwwSxSc7q1KCn9DC3aXply'
"UPDATE identity.credentials SET password_hash='$hash', must_change_password=false WHERE user_id=(SELECT id FROM identity.users WHERE email='admin@ppms.local'); UPDATE identity.users SET status='ACTIVE', failed_login_attempts=0, locked_at=NULL WHERE email='admin@ppms.local';" |
  docker exec -i ppms-postgres psql -U postgres -d ppms_identity
```

## 5. Start the frontend

```powershell
cd frontend
pnpm install
pnpm dev          # http://localhost:5173
```

The SPA calls the gateway at `VITE_API_URL` (default `http://localhost:8080`). Log in with one of
the users above. Useful scripts: `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build`.

## 6. Stop / reset

```powershell
cd infra\compose
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile services --profile observability down

# Also wipe the databases/volumes (fresh start; bootstrap admin is recreated on next start)
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile services --profile observability down -v
cd ..\..
```

## 7. Local URLs

| What | URL |
|---|---|
| Frontend (Vite) | http://localhost:5173 |
| API gateway | http://localhost:8080 |
| Kafka UI | http://localhost:8090 (`KAFKA_UI_PORT`) |
| Mailpit (outgoing e-mail) | http://localhost:8025 |
| MinIO console | http://localhost:9001 (user/password from `.env`) |
| Grafana (observability profile) | http://localhost:3001 |
| Postgres | `localhost:<POSTGRES_PORT>`, one DB per service: `ppms_identity`, `ppms_admin`, `ppms_parent_flock`, `ppms_hatchery`, `ppms_farm`, `ppms_slaughterhouse`, `ppms_notification`, `ppms_reporting` |

Gateway → service routing: `/api/identity/**`, `/api/admin/**`, `/api/parent-flock/**`,
`/api/hatchery/**`, `/api/farm/**`, `/api/slaughterhouse/**`, `/api/notification/**`, `/api/reporting/**`.

## 8. Building / running services outside Docker (optional)

```powershell
cd backend
# Sequential build is required whenever shared-kernel changed (see AGENTS.md)
.\mvnw.cmd clean install -DskipTests
# Unit tests (admin-service context test needs Docker for Testcontainers)
.\mvnw.cmd clean test -pl shared-kernel,identity-service,admin-service
```

To run a service from the IDE against the Docker infrastructure, set:

- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:<POSTGRES_PORT>/ppms_<service>` plus the `.env` user/password
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- `JWT_PUBLIC_KEY_FILE` (and `JWT_PRIVATE_KEY_FILE` for identity) pointing at `infra\compose\secrets\*.pem`
- `PPMS_INTERNAL_SECRET` (same value for identity and admin), `ADMIN_SERVICE_URL=http://localhost:8085` for identity

Service ports: identity 8084, admin 8085, parent-flock 8081, hatchery 8082, farm 8083,
slaughterhouse 8086, notification 8087, reporting 8088, gateway 8080.

## 9. Smoke tests

With the stack up:

- `infra\scripts\authz-smoke.ps1` logs in as the admin and a `FARM_MANAGER` user and prints the
  HTTP status matrix across modules (expects a `farm.manager@ppms.local` user with password
  `Admin123!` and a FARM-scoped role assignment; see the script header).
- `infra\scripts\access-smoke.ps1` exercises user/role/permission/assignment management
  (`/api/identity/users`, `/api/admin/access/**`). Idempotent.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File infra\scripts\authz-smoke.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File infra\scripts\access-smoke.ps1
```

## 10. Using the UI as administrator

- **View as role** (top bar, admins only): previews the menu, dashboard tiles and buttons exactly as
  the selected role would see them. It only changes the display — your administrator rights stay
  active on the server, and a banner reminds you while the preview is on.
- **Administration → Users**: search/filter accounts, create users (optionally with an initial
  password so they can log in immediately), open a user to enable/disable/unlock, reset password or
  MFA, and assign/revoke roles. Site-scoped roles (e.g. `FARM_MANAGER`) need a site: sites appear in
  the picker once they are created in their module (farm, hatchery, …) and propagated via Kafka.
- **Administration → Roles & permissions**: the role catalogue with scopes and permissions; create
  custom roles and permission keys (`module:resource:action`). `SYSTEM_ADMIN` always has every
  permission; `MANAGER` is read-only everywhere.
- **Operations → Farms**: farm cards/table → farm detail (houses, silos, lots) → lot detail (daily
  sheets, deliveries). Create buttons only appear for roles with write access to the module.

## 11. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `psql: FATAL: role "ppms" does not exist` | Your `.env` uses different Postgres credentials than `.env.example`; pass your `POSTGRES_USER` to `-U`. |
| Login returns 401 and the identity log says `UNKNOWN_EMAIL` / `BAD_PASSWORD` | Wrong credentials (browser autofill is a common culprit). Reset the password with the SQL in section 4. |
| Login returns 500 with `ADMIN_UNAVAILABLE` in identity logs | identity cannot reach admin-service's `/internal/**` endpoint: check both containers are healthy and share the same `PPMS_INTERNAL_SECRET`. |
| First request right after a gateway restart fails / connection reset | Transient; retry. If it persists, `docker restart ppms-gateway-service-1` (the gateway caches service IPs). |
| `kafka-init` logs `/scripts/create-topics.sh: not found` | The script was checked out with CRLF line endings. Compose strips `\r` before running it; if you edited it, make sure it is saved with LF. |
| Service fails to start with a Flyway/Hibernate validation error | Entity/migration drift. Never edit an applied migration; add a new `V<prefix>NNN__*.sql`. For a scratch DB use `down -v`. |
| `Unresolved compilation problem` in Maven test runs | Stale IDE-compiled classes in `target\`; run with `clean`. |
| A logged-in user sees an empty menu / "You do not have permission" | The user has no role assignment in `admin.user_role_assignment`. Assign one or use the bootstrap admin. |
