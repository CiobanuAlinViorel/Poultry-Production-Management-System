# 09 — Cross-cutting services: gateway, notification, reporting

Three new modules. `gateway-service` should be created **right after identity-service
(03)** so the frontend has one origin from day one; `notification-service` right after
shared-kernel alerts exist (first consumer: identity e-mails); `reporting-service` after
the domain services publish events (it can be built incrementally per domain).

---

## 1. gateway-service (Spring Cloud Gateway, port 8080)

Package `com.ppms.gateway`, reactive (WebFlux) — **does not** depend on shared-kernel
(servlet); JWT validation via `spring-boot-starter-oauth2-resource-server` with
`jwk-set-uri = http://identity-service:8084/.well-known/jwks.json`.

Routes (`application.yml`, path prefix stripped = none; services already serve
`/api/v1/...` so we route by **module prefix** the frontend uses):

| Frontend path | Target |
|---|---|
| `/api/identity/**` → `/api/v1/**` | identity-service |
| `/api/admin/**` | admin-service |
| `/api/parent-flock/**` | parent-flock-service |
| `/api/hatchery/**` | hatchery-service |
| `/api/farm/**` | farm-service |
| `/api/slaughterhouse/**` | slaughterhouse-service |
| `/api/notification/**` | notification-service |
| `/api/reporting/**` | reporting-service |
| `/docs/<module>/**` | swagger-ui/api-docs of each (SYSTEM_ADMIN only in prod) |
| `/ws/**` | notification-service WebSocket/SSE (token in first message / query) |

Filters: `RewritePath` (`/api/<module>/(?<p>.*)` → `/api/v1/${p}`), `TokenRelay`
(pass Bearer through unchanged), add `X-Request-Id`/`traceparent`, `X-Forwarded-*`,
strip hop headers, **deny** `/internal/**` and `/actuator/**` (except gateway's own
health), rate limiting (in-memory `Bucket4j`: auth endpoints 10/min/IP, others
600/min/user), request size limit 20 MB (uploads), CORS allow-list from env
(`PPMS_CORS_ORIGINS`), security headers (HSTS, CSP for docs), gzip.
Public paths: `/api/identity/auth/login|refresh|forgot-password|reset-password|activate|
mfa/verify|pin-login`, `/api/identity/.well-known/**`, `/actuator/health`.
Resilience: per-route `CircuitBreaker` (Resilience4j) → 503 envelope `SERVICE_UNAVAILABLE`
with `service` field so the SPA shows "Modulul X este indisponibil" without breaking
other modules; timeouts 10 s (30 s for `/export` and `*.pdf`).
Observability: metrics per route, access log JSON. Tests: WebTestClient routing +
public path matrix + rate-limit.

## 2. notification-service (port 8087, schema `notification`, package `com.ppms.notification`)

### 2.1 Responsibilities
Consume `ppms.alerts` and other events needing human notification; maintain **Alert**
lifecycle (open/acknowledged/resolved); fan out **Notification**s per user via channels
(in-app, e-mail, web-push; SMS adapter interface only); real-time delivery to the SPA
(SSE/WebSocket); user preferences; digests.

### 2.2 Domain
| Entity | Fields |
|---|---|
| `Alert` | alertCode, severity, scopeType, scopeId, siteName (from directory), entityType, entityId, params jsonb, titleKey/messageKey (i18n keys = alertCode), dedupKey, status `OPEN/ACKNOWLEDGED/RESOLVED/EXPIRED`, raisedAt, lastSeenAt, occurrences, acknowledgedBy/At, ackReason, resolvedAt, resolvedBy/`SYSTEM`, targetRoles[] |
| `AlertRule` | alertCode, defaultSeverity, targetRoles, channels per severity (INFO→in-app; WARNING→in-app+push; CRITICAL→in-app+push+email), escalationMinutes (CRITICAL unacknowledged → escalate to MANAGER after 60 min), autoResolveAfterHours?, enabled, per-site overrides |
| `Notification` | userId, alertId?, type `ALERT/TASK/INFO/SYSTEM`, titleKey, bodyKey, params, link (SPA route), channel results jsonb, read (bool), readAt, createdAt, expiresAt |
| `UserPreference` | userId, locale, emailEnabled, pushEnabled, quietHours, digest `NONE/DAILY`, mutedAlertCodes[] |
| `PushSubscription` | userId, endpoint, keys (p256dh, auth), userAgent, createdAt |
| `EmailOutbox` | to, subject, bodyHtml, status, attempts, sentAt, error |
| `Recipient directory` read model | userId → displayName, email, locale, role assignments (from identity + admin events) |

### 2.3 Rules
- Dedup: same `dedupKey` while `OPEN` → increment `occurrences`, update `lastSeenAt`,
  no new notifications unless severity increased or 6 h passed (configurable).
- Recipients = users having a **role in `targetRoles` scoped to `scopeId` or GLOBAL**
  (resolved from the recipient directory; MANAGER gets CRITICAL only).
- CRITICAL requires acknowledgement **with reason** (X2-FR-04); not acknowledged after
  `escalationMinutes` → notify MANAGER + site manager role via e-mail.
- `alert.resolved` events (same `dedupKey`) or `autoResolveAfterHours` close alerts.
- E-mail via Spring Mail (Mailpit dev; SMTP/SES prod), Thymeleaf templates RO/EN,
  retry 3× with backoff; web-push via `webpush-java` with VAPID keys (env).
- Quiet hours: WARNING/INFO deferred to digest; CRITICAL always immediate.
- Identity e-mails (`password-reset-requested`, `activation-requested`) handled here
  as `SYSTEM` notifications with templates.

### 2.4 API
`GET /alerts?scopeType&scopeId&severity&status&from&to` (scoped), `GET /alerts/{id}`,
`POST /alerts/{id}/acknowledge {reason}`, `POST /alerts/{id}/resolve`, `GET /alerts/summary?siteIds`
(counts by severity for dashboards), `GET /notifications?unreadOnly` (own), `POST
/notifications/{id}/read`, `/read-all`, `GET /stream` (SSE, own notifications + alert
counters), `GET/PUT /preferences`, `POST/DELETE /push-subscriptions`, admin:
`GET/PUT /alert-rules`, `POST /test-email` (SYSTEM_ADMIN), `GET /email-outbox`.
Events published: `ppms.notification` (`alert.acknowledged`, `alert.escalated`,
`notification.sent`). Consumed: `ppms.alerts`, `ppms.identity.user`,
`ppms.admin.role-assignment`, `ppms.admin.employee`, `ppms.site.directory`,
`ppms.admin.roster` (roster published → notify employees), `ppms.farm.delivery.fci.issued`
(email FCI to slaughterhouse contact from settings).

### 2.5 Tasks
N0 bootstrap; N1 recipient directory consumers; N2 Alert ingestion + dedup + rules +
seed `AlertRule`s for every alert code listed in files 03–08 (compile the list into
`resources/seed/alert-rules.csv`); N3 Notification fan-out in-app + SSE; N4 e-mail
(templates, outbox, retry) incl. identity mails; N5 web-push; N6 ack/escalation/
auto-resolve jobs; N7 preferences, digests; N8 API, exports, metrics
(`ppms_alerts_open{severity,site}`).
Tests: IT with Kafka container: alert.raised → Alert row → notifications for scoped
users only; CRITICAL → mail in GreenMail/Mailpit; dedup; escalation with `TestClock`.

## 3. reporting-service (port 8088, schema `reporting`, package `com.ppms.reporting`)

### 3.1 Responsibilities
Read-only, event-sourced read models for cross-context questions: **manager
dashboard**, **traceability chain**, **KPI history & benchmarks**, **cost & margin
roll-ups**, **audit explorer**, **regulatory exports** (DSVSA CSV packs), scheduled
reports (PDF/XLSX by e-mail). Never the source of truth; rebuildable by replaying topics
(`POST /admin/rebuild` resets offsets for a consumer group — SYSTEM_ADMIN).

### 3.2 Read models (tables)
- `site_overview` (siteId, type, name, activeUnits (lots/flocks/batches), birds, open
  alerts by severity, last activity).
- `lot_fact` (lotId, lotCode, farm, house, breed, placementDate, closeDate, initial,
  current, mortalityPct, avgWeight, fcr, epef, deliveries, slaughterYield,
  condemnationPct, costTotal, costPerKg, revenueAllocated, margin) — upserted from farm,
  slaughterhouse & costing events.
- `flock_fact`, `incubation_fact`, `session_fact`, `dispatch_fact`, `invoice_fact`
  similar.
- `kpi_daily` (siteId, unitId, date, metric, value) time-series for charts.
- `trace_link` (fromType, fromCode, toType, toCode, relation, occurredAt) — a graph of
  codes: `EggBatch(PF)→EggTransferOrder→EggReception→EggBatch(HA)→IncubationBatch→
  HatchRecord→ChickBatch→ChickTransferOrder→ChickReception→Lot→FarmDeliveryOrder→
  LiveBirdReception→SlaughterSession→ProductionLot→StockUnit→DispatchOrder→Invoice`.
- `inventory_position` (siteId, item, onHand, belowReorder, expiringQty).
- `cost_rollup` (siteId, costCenterType/Id, period, category, amount) and
  `margin_rollup` (customer, sku, period, revenue, cost, margin).
- `audit_index` (actor, action, entity, siteId, occurredAt, service, payload) searchable.
- `alert_history`.

### 3.3 API
`GET /dashboard/manager` (company: sites overview, active lots/flocks/batches, planned
deliveries next 7 days, cold store occupancy, open CRITICAL alerts, today's kill plan,
KPIs vs last month), `GET /dashboard/site/{siteId}` (per type), `GET /kpi?metric&siteId&unitId&from&to&granularity`,
`GET /benchmarks/lots?farmId&from&to` (lots ranked by EPEF/FCR/cost), `GET /trace/{code}`
(detects code type by prefix; returns forward & backward chain as tree + timeline;
X3-FR-03), `GET /trace/search?q`, `GET /costing/rollup?siteId&period&groupBy`,
`GET /margin?customerId&skuId&period`, `GET /audit?actor&entity&from&to&service`,
`GET /alerts/history`, exports for all (`?format=csv|xlsx`), `GET /reports` (catalogue),
`POST /reports/{code}/run` (async → document; e.g. `DSVSA_TREATMENT_REGISTER`,
`WEEKLY_PRODUCTION_SUMMARY`, `MONTHLY_MANAGEMENT_PACK` PDF), `GET/POST /report-schedules`
(cron, recipients; notification-service e-mails link).
Roles: `MANAGER`/`ACCOUNTANT` all; site roles see own sites; `SYSTEM_ADMIN` audit + rebuild.

### 3.4 Tasks
R0 bootstrap + generic idempotent projector framework (one projector class per topic,
version-tolerant JSON); R1 site_overview + alert_history (first dashboard); R2
farm projections (lot_fact, kpi_daily) → farm dashboards; R3 parent flock & hatchery
projections; R4 slaughterhouse + sales + margin; R5 trace_link graph + `/trace`; R6
inventory & costing roll-ups; R7 audit index; R8 report engine (Thymeleaf → PDF, XLSX)
+ schedules; R9 rebuild tooling, metrics (projection lag).
Tests: projector unit tests per event type (given event → row state), IT replay of a
recorded event fixture set (`resources/fixtures/*.jsonl` generated by the end-to-end
chain tests of the domain services) → assert dashboard & trace results.

## Exit criteria
SPA uses only `http://gateway:8080`; alerts appear in real time with acknowledgement
flow; manager dashboard and full traceability lookup work on demo data.
