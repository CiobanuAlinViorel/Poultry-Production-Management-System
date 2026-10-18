# 01 — Infrastructure, Repository Hygiene & DevOps

Independent of business code; must be completed before `02-shared-kernel.md`.
Everything here is reproducible with `docker compose up` on a developer laptop.

---

## 1. Repository layout (target)

```
/
├── .github/workflows/            ci-backend.yml, ci-frontend.yml, release.yml
├── .devin/                       (agent config, optional)
├── backend/
│   ├── pom.xml                   reactor + BOM (exists; add new modules)
│   ├── shared-kernel/
│   ├── identity-service/
│   ├── admin-service/
│   ├── parent-flock-service/
│   ├── hatchery-service/
│   ├── farm-service/
│   ├── slaughterhouse-service/
│   ├── notification-service/     NEW
│   ├── reporting-service/        NEW
│   ├── gateway-service/          NEW
│   └── docker/
│       ├── Dockerfile.service    one multi-stage Dockerfile parameterised by MODULE
│       └── postgres/init/01-create-databases.sql
├── frontend/                      React SPA (see 10-frontend.md)
├── infra/
│   ├── compose/
│   │   ├── docker-compose.yml           infra only (db, kafka, mailpit, minio, observability)
│   │   ├── docker-compose.services.yml  all backend services + gateway (profile "services")
│   │   ├── docker-compose.frontend.yml  nginx serving SPA (profile "frontend")
│   │   └── .env.example
│   ├── k8s/                             Helm chart `ppms` (see §8)
│   ├── nginx/                            nginx.conf for SPA + TLS
│   ├── observability/                    prometheus.yml, grafana provisioning, loki, promtail
│   └── scripts/                          backup.sh, restore.sh, gen-jwt-keys.sh, seed-demo.sh
├── docs/
│   ├── plan/                             this plan
│   ├── specs/                            copy Specs.MD, parent-flock-architecture.md, identity-admin design here
│   └── adr/                              architecture decision records (ADR-0001-kafka-from-start.md ...)
├── Makefile                              convenience targets (up, down, build, test, seed, logs)
├── .gitignore  .gitattributes  .editorconfig  README.md  AGENTS.md
```

Tasks:
1. Move `backend/docker-compose.yml` → `infra/compose/docker-compose.yml` and rewrite per §3.
2. Copy the three spec documents from the gitignored worktree into `docs/specs/`.
3. Add `.editorconfig` (UTF-8, LF, 4 spaces Java, 2 spaces TS/JSON/YAML, final newline).
4. Add `AGENTS.md` with build/test/run commands (content in §10).
5. Remove `*.iml`, `.idea/workspace.xml` from git index if tracked (`git rm --cached`).

---

## 2. `.gitignore` (replace the current file with this content)

```gitignore
# ===== Build output =====
target/
build/
out/
dist/
*.jar
*.war
*.ear
*.class
!.mvn/wrapper/maven-wrapper.jar
.mvn/timing.properties

# ===== Node / frontend =====
node_modules/
frontend/dist/
frontend/coverage/
frontend/.vite/
frontend/storybook-static/
frontend/playwright-report/
frontend/test-results/
*.tsbuildinfo
.eslintcache

# ===== Spring / config with secrets =====
application-local.yml
application-local.yaml
application-local.properties
**/src/main/resources/application-*-secret.yml
.env
.env.*
!.env.example
!infra/compose/.env.example

# ===== Secrets / keys =====
*.pem
*.key
*.p12
*.jks
*.pfx
!infra/k8s/**/ca-bundle.pem

# ===== IDEs =====
.idea/
*.iml
*.iws
*.ipr
.vscode/
!.vscode/extensions.json
!.vscode/settings.shared.json
.project
.classpath
.settings/
.factorypath

# ===== OS =====
.DS_Store
Thumbs.db
desktop.ini

# ===== Docker / infra =====
docker-compose.override.yml
infra/compose/volumes/
infra/compose/backups/
*.dump
*.sql.gz

# ===== Logs / tmp =====
*.log
logs/
tmp/
.tmp/
hs_err_pid*
replay_pid*

# ===== Test / coverage =====
jacoco.exec
**/jacoco/
**/surefire-reports/
**/failsafe-reports/
.testcontainers*

# ===== Agent working dirs =====
.claude/
.cursor/
.devin/cache/
```

`.gitattributes` keep existing + add `*.sh text eol=lf`, `mvnw text eol=lf`,
`*.cmd text eol=crlf`.

---

## 3. docker-compose (local development)

> Decision: one PostgreSQL 16 container with **one database per service** (not one
> database per schema) for dev; production uses a managed Postgres or the k8s operator
> with the same database names. Service `application.yml` keeps `default_schema`.
> Kafka in **KRaft** mode (single broker) from day one.

### 3.1 `infra/compose/.env.example`
```env
COMPOSE_PROJECT_NAME=ppms
POSTGRES_USER=ppms
POSTGRES_PASSWORD=ppms_dev
POSTGRES_PORT=5432
KAFKA_PORT=9092
KAFKA_UI_PORT=8090
MAILPIT_UI_PORT=8025
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=ppms
MINIO_ROOT_PASSWORD=ppms_dev_minio
GRAFANA_PORT=3001
PROMETHEUS_PORT=9090
JWT_PRIVATE_KEY_FILE=./secrets/jwt-private.pem
JWT_PUBLIC_KEY_FILE=./secrets/jwt-public.pem
```

### 3.2 `infra/compose/docker-compose.yml` (infrastructure)
```yaml
name: ppms
services:
  postgres:
    image: postgres:16-alpine
    container_name: ppms-postgres
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: postgres
    ports: ["${POSTGRES_PORT}:5432"]
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ../../backend/docker/postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
    command: ["postgres", "-c", "max_connections=300", "-c", "shared_buffers=512MB"]

  kafka:
    image: apache/kafka:3.8.0
    container_name: ppms-kafka
    ports: ["${KAFKA_PORT}:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:19092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:19092,EXTERNAL://localhost:${KAFKA_PORT}
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
      KAFKA_LOG_RETENTION_HOURS: 168
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    volumes: [kafka_data:/var/lib/kafka/data]
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:19092 >/dev/null 2>&1"]
      interval: 10s
      timeout: 10s
      retries: 10

  kafka-init:                       # creates all topics (idempotent) — see 02 §5.2 for list
    image: apache/kafka:3.8.0
    depends_on: { kafka: { condition: service_healthy } }
    entrypoint: ["/bin/sh", "-c"]
    command: ["/scripts/create-topics.sh kafka:19092"]
    volumes: ["./kafka/create-topics.sh:/scripts/create-topics.sh:ro"]

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports: ["${KAFKA_UI_PORT}:8080"]
    environment:
      KAFKA_CLUSTERS_0_NAME: ppms
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:19092
    depends_on: [kafka]

  mailpit:                          # SMTP sink for dev e-mails
    image: axllent/mailpit:latest
    ports: ["${MAILPIT_UI_PORT}:8025", "1025:1025"]

  minio:                            # S3-compatible store for generated PDFs / uploads
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    ports: ["${MINIO_PORT}:9000", "${MINIO_CONSOLE_PORT}:9001"]
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes: [minio_data:/data]
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 10s
      retries: 5

  minio-init:
    image: minio/mc:latest
    depends_on: { minio: { condition: service_healthy } }
    entrypoint: ["/bin/sh", "-c"]
    command: >
      "mc alias set local http://minio:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD} &&
       mc mb -p local/ppms-documents && mc mb -p local/ppms-uploads && exit 0"

  prometheus:
    image: prom/prometheus:latest
    ports: ["${PROMETHEUS_PORT}:9090"]
    volumes: ["../observability/prometheus.yml:/etc/prometheus/prometheus.yml:ro"]
    profiles: [observability]

  grafana:
    image: grafana/grafana:latest
    ports: ["${GRAFANA_PORT}:3000"]
    environment: { GF_SECURITY_ADMIN_PASSWORD: admin, GF_AUTH_ANONYMOUS_ENABLED: "true" }
    volumes: ["../observability/grafana/provisioning:/etc/grafana/provisioning:ro", grafana_data:/var/lib/grafana]
    profiles: [observability]

  loki:
    image: grafana/loki:latest
    ports: ["3100:3100"]
    profiles: [observability]

  promtail:
    image: grafana/promtail:latest
    volumes: ["/var/run/docker.sock:/var/run/docker.sock:ro", "../observability/promtail.yml:/etc/promtail/config.yml:ro"]
    profiles: [observability]

  pgbackup:                         # NFR-A-03: dump every 4 h, keep 14 days
    image: prodrigestivill/postgres-backup-local:16
    depends_on: [postgres]
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ppms_identity,ppms_admin,ppms_parent_flock,ppms_hatchery,ppms_farm,ppms_slaughterhouse,ppms_notification,ppms_reporting
      SCHEDULE: "0 */4 * * *"
      BACKUP_KEEP_DAYS: 14
      BACKUP_KEEP_WEEKS: 8
      BACKUP_KEEP_MONTHS: 6
    volumes: ["./backups:/backups"]
    profiles: [backup]

volumes:
  pg_data: {}
  kafka_data: {}
  minio_data: {}
  grafana_data: {}
```

### 3.3 `backend/docker/postgres/init/01-create-databases.sql`
```sql
CREATE DATABASE ppms_identity;
CREATE DATABASE ppms_admin;
CREATE DATABASE ppms_parent_flock;
CREATE DATABASE ppms_hatchery;
CREATE DATABASE ppms_farm;
CREATE DATABASE ppms_slaughterhouse;
CREATE DATABASE ppms_notification;
CREATE DATABASE ppms_reporting;
-- Optional least-privilege roles (one per service); dev uses the superuser for simplicity:
-- CREATE ROLE svc_farm LOGIN PASSWORD 'x'; GRANT ALL ON DATABASE ppms_farm TO svc_farm; ...
```
Update `identity-service` and `admin-service` `application.yml` datasource URLs to
`jdbc:postgresql://localhost:5432/ppms_identity` / `ppms_admin` (schema settings stay).

### 3.4 `infra/compose/docker-compose.services.yml`
One block per service, identical shape (profile `services`):
```yaml
services:
  identity-service:
    build: { context: ../../backend, dockerfile: docker/Dockerfile.service, args: { MODULE: identity-service } }
    image: ppms/identity-service:${TAG:-dev}
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ppms_identity
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:19092
      JWT_PRIVATE_KEY_FILE: /run/secrets/jwt_private
      JWT_PUBLIC_KEY_FILE: /run/secrets/jwt_public
      ADMIN_SERVICE_URL: http://admin-service:8085
    secrets: [jwt_private, jwt_public]
    depends_on: { postgres: { condition: service_healthy }, kafka: { condition: service_healthy } }
    healthcheck: { test: ["CMD", "wget", "-qO-", "http://localhost:8084/actuator/health"], interval: 15s, retries: 10 }
    profiles: [services]
  # admin-service, parent-flock-service, hatchery-service, farm-service, slaughterhouse-service,
  # notification-service (adds SPRING_MAIL_HOST=mailpit, SPRING_MAIL_PORT=1025),
  # reporting-service (adds S3_ENDPOINT=http://minio:9000, S3_BUCKET=ppms-documents),
  # gateway-service (ports: ["8080:8080"], env ROUTES_* = internal service URLs)
secrets:
  jwt_private: { file: ${JWT_PRIVATE_KEY_FILE} }
  jwt_public:  { file: ${JWT_PUBLIC_KEY_FILE} }
```
Only the **gateway** publishes a port; services talk on the compose network.

### 3.5 `backend/docker/Dockerfile.service`
```dockerfile
# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build
ARG MODULE
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY shared-kernel/pom.xml shared-kernel/pom.xml
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl ${MODULE} -am dependency:go-offline
COPY shared-kernel shared-kernel
COPY ${MODULE} ${MODULE}
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl ${MODULE} -am -DskipTests package \
 && java -Djarmode=layertools -jar ${MODULE}/target/*.jar extract --destination /layers

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ppms && adduser -S ppms -G ppms && apk add --no-cache wget tzdata
ENV TZ=Europe/Bucharest JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
USER ppms
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### 3.6 Spring profiles per service
- `default` — localhost infra (compose ports), `ddl-auto=validate`, Flyway on.
- `docker` — hostnames `postgres`, `kafka`, `mailpit`, `minio`.
- `test` — Testcontainers (set by test base class, not a file).
- `prod` — all values from env vars; `springdoc` disabled unless `PPMS_DOCS_ENABLED=true`.
- Key files: JWT keys loaded from `*_FILE` env var if present, else `JWT_PRIVATE_KEY`
  env var (PEM text), else `application-local.yml`. Implement in shared-kernel
  `JwtKeyProvider` (extend the existing one).

### 3.7 Makefile targets
`make infra-up` (db+kafka+mailpit+minio), `make up` (infra + services), `make
obs-up`, `make down`, `make build` (`./mvnw -T1C verify`), `make test-it`
(`-Pintegration`), `make fe-dev`, `make seed` (runs `infra/scripts/seed-demo.sh` that
posts demo data through the API: 1 breeder site, 1 hatchery, 2 farms, 1 slaughterhouse,
users per role), `make keys` (runs `gen-jwt-keys.sh` → `infra/compose/secrets/`).

---

## 4. Maven reactor changes (`backend/pom.xml`)

1. Fix module order: `shared-kernel` first, then `identity-service`, `admin-service`,
   domain services, `notification-service`, `reporting-service`, `gateway-service`.
2. Add to `<properties>`: `spring-cloud.version=2023.0.3`, `openhtmltopdf.version=1.0.10`
   (PDF), `apache-poi.version=5.3.0` (XLSX), `opencsv.version=5.9`, `jacoco.version=0.8.12`,
   `archunit.version=1.3.0`, `springdoc.version=2.6.0`, `testcontainers.version=1.20.1`.
3. `<dependencyManagement>`: import `spring-cloud-dependencies` BOM; manage
   `testcontainers-bom`, `kafka` (Spring Kafka comes from Boot BOM), `openhtmltopdf-pdfbox`,
   `poi-ooxml`, `opencsv`, `archunit-junit5`, `mapstruct`, `mapstruct-processor`.
4. `<pluginManagement>`: `maven-surefire` (unit `*Test`), `maven-failsafe` (`*IT`, profile
   `integration`), `jacoco-maven-plugin` with `check` rule `LINE ≥ 0.70` on package
   `**/application/service/**`, `spring-boot-maven-plugin` (`layers` enabled),
   `maven-compiler-plugin` with Lombok + MapStruct annotation processors
   (`lombok-mapstruct-binding`).
5. Each service POM depends on: `shared-kernel`, `spring-boot-starter-web`, `-validation`,
   `-data-jpa`, `-security`, `-actuator`, `spring-kafka`, `flyway-core` +
   `flyway-database-postgresql`, `postgresql`, `springdoc-openapi-starter-webmvc-ui`,
   `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
   (optional), tests: `spring-boot-starter-test`, `spring-security-test`,
   `testcontainers: postgresql, kafka, junit-jupiter`, `spring-kafka-test`, `archunit-junit5`.

Standard `application.yml` fragment every service copies:
```yaml
spring:
  application.name: ${MODULE_NAME}
  jpa:
    open-in-view: false
    hibernate.ddl-auto: validate
    properties.hibernate: { default_schema: <schema>, jdbc.time_zone: UTC, jdbc.batch_size: 50 }
  flyway: { schemas: <schema>, default-schema: <schema>, locations: classpath:db/migration }
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer: { group-id: <module>, auto-offset-reset: earliest, enable-auto-commit: false }
    producer: { acks: all, properties: { enable.idempotence: true } }
    listener.ack-mode: record
management:
  endpoints.web.exposure.include: health,info,prometheus,metrics
  endpoint.health.probes.enabled: true
  tracing.sampling.probability: 0.1
ppms:
  site-timezone: Europe/Bucharest
  jwt: { public-key-file: ${JWT_PUBLIC_KEY_FILE:}, public-key: ${JWT_PUBLIC_KEY:} }
  admin-service.url: ${ADMIN_SERVICE_URL:http://localhost:8085}
  outbox: { poll-interval-ms: 500, batch-size: 100 }
```

---

## 5. Kafka topic bootstrap (`infra/compose/kafka/create-topics.sh`)

Creates every topic listed in `02-shared-kernel.md` §5.2 with
`--partitions 3 --replication-factor 1 --config retention.ms=604800000` (dev) and
`cleanup.policy=compact` for the `*.directory` topics. Idempotent (`--if-not-exists`).

---

## 6. CI — GitHub Actions

### 6.1 `ci-backend.yml` (on PR + push to main, paths `backend/**`)
1. `actions/setup-java@v4` (temurin 21, cache maven).
2. `./mvnw -B -T1C verify -Pintegration` — Testcontainers uses the runner's Docker.
3. Upload JaCoCo report; fail if `check` fails.
4. ArchUnit tests run as part of unit tests (layer rules, no cross-context imports).
5. On `main`: build & push images per module to GHCR (`ghcr.io/<org>/ppms-<module>:sha`,
   `:latest`) using `docker/build-push-action` with the Dockerfile in §3.5.
6. `trivy` image scan (fail on CRITICAL), `dependency-check` or `mvn org.owasp:...`
   weekly schedule.

### 6.2 `ci-frontend.yml` (paths `frontend/**`)
`pnpm install --frozen-lockfile` → `pnpm lint` → `pnpm typecheck` → `pnpm test --coverage`
→ `pnpm build` → Playwright e2e against `docker compose --profile services` (nightly or
label-gated) → upload `dist` artifact; on `main` build nginx image `ppms-frontend`.

### 6.3 `release.yml`
Tag `v*` → build all images with the tag, generate changelog (conventional commits),
package Helm chart to GHCR OCI.

Branch protection: PR required, CI green, 1 review; conventional commit messages
(`feat(farm): ...`, `fix(identity): ...`).

---

## 7. Security & secrets

- RS256 key pair for JWT. `infra/scripts/gen-jwt-keys.sh` → `infra/compose/secrets/`
  (gitignored). Rotate by adding new `kid`; JWKS exposes current + previous.
- No secret in any committed file. `.env.example` only has dev defaults.
- TLS terminated at Nginx (compose prod profile, Let's Encrypt via certbot sidecar) or at
  the k8s Ingress (cert-manager). Internal traffic plain HTTP in the private network.
- Postgres: one login role per service in prod (`svc_<module>`), only its own database.
- Actuator: `health` public (liveness/readiness), everything else requires
  `SYSTEM_ADMIN` or internal network.
- Gateway rate limit on `/api/v1/auth/**` (Redis-less in-memory bucket, 10 req/min/IP).

---

## 8. Kubernetes (Helm chart `infra/k8s/ppms`)

- `Chart.yaml`, `values.yaml` (image tags, replicas, resources, env), templates per
  service: `Deployment` (readiness `/actuator/health/readiness`, liveness `/liveness`,
  `resources` requests 512Mi/250m, limits 1Gi/1000m), `Service`, `HorizontalPodAutoscaler`
  (CPU 70 %), `ConfigMap` (non-secret env), `ExternalSecret`/`Secret` placeholders,
  `PodDisruptionBudget`.
- `Ingress` (nginx class) routing `/` → frontend, `/api` → gateway, TLS via cert-manager.
- Dependencies: Bitnami `postgresql` (or CloudNativePG operator), Strimzi Kafka
  (`KafkaNodePool` KRaft, 3 brokers prod / 1 dev), `minio`, `kube-prometheus-stack`,
  `loki-stack`. All optional via `values.<dep>.enabled`.
- `CronJob` for logical backups → S3 bucket `ppms-backups` (retention 30 days) if the
  Postgres operator does not handle it.
- Flyway runs on service start (`initContainer` not needed; services tolerate concurrent
  start thanks to Flyway locking).
- Deploy with `helm upgrade --install ppms infra/k8s/ppms -f values-<env>.yaml`.

---

## 9. Observability

- Micrometer → Prometheus (`/actuator/prometheus`), Grafana dashboards provisioned:
  JVM, HTTP, Kafka consumer lag, business gauges (`ppms_active_lots`,
  `ppms_open_critical_alerts`, `ppms_outbox_pending`).
- Logs JSON (logstash encoder) with `traceId`, `userId`, `siteId` MDC; shipped by
  promtail → Loki.
- Tracing OpenTelemetry (OTLP to Grafana Tempo optional).
- Alerts (Prometheus rules): service down 2 min, outbox pending > 1000 for 5 min,
  consumer lag > 10 000, 5xx ratio > 2 %.

---

## 10. `AGENTS.md` content (create at repo root)

```md
# PPMS — agent/developer quick reference
- Backend: `cd backend && ./mvnw -T1C verify` (unit) · `./mvnw -T1C verify -Pintegration` (needs Docker)
- Run one service: `./mvnw -pl farm-service spring-boot:run` (infra must be up)
- Infra: `make infra-up` / `make down` · Kafka UI :8090 · Mailpit :8025 · MinIO :9001
- Frontend: `cd frontend && pnpm i && pnpm dev` (proxy to gateway :8080)
- Plan: docs/plan/ (follow file order). Specs: docs/specs/. ADRs: docs/adr/
- Conventions: docs/plan/00-overview.md §3.3. Never hard-code business thresholds.
- Secrets: never commit; use infra/compose/.env (gitignored) and secrets/ dir.
```

---

## Exit criteria
- [ ] `make infra-up` starts Postgres (8 DBs), Kafka (topics created), Mailpit, MinIO.
- [ ] `./mvnw verify` green on a clean clone (existing identity/admin stubs compile).
- [ ] Dockerfile builds `identity-service` image; container passes healthcheck.
- [ ] CI workflows run on a PR and publish JaCoCo + images on main.
- [ ] `.gitignore`, `.editorconfig`, `AGENTS.md`, `docs/specs/*` committed; no `.iml`
      or key material tracked.
