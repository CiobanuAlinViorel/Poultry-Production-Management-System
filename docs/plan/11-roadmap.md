# 11 — Roadmap, Definition of Done, Test Matrix, Go-live

## 1. Phases (each ends with a runnable, demoable increment)

| Phase | Scope (plan files / tasks) | Demo at end |
|---|---|---|
| **P0 Foundation** | 01 all · 02 K1–K9 · 03 I0 · 09 gateway §1 (routes for identity/admin only) | `make up` starts infra + identity + admin stub + gateway; login via gateway returns JWT with roles from admin stub |
| **P1 Identity & Admin** | 03 I1–I11 · 04 A0–A9 · 09 notification N0–N4 (needed for e-mails) · 10 FE0–FE3 | Admin creates a user, grants FARM_WORKER at Farm A, user activates by e-mail, logs in with MFA on phone & desktop |
| **P2 Parent flock** | 05 PF0–PF9 · 10 FE8 (can run in parallel with P3 backend if two implementers) | Breeder cycle demo (05 exit criteria) in UI |
| **P3 Hatchery** | 06 H0–H8 · 10 FE7 | Eggs from P2 received, incubated (TestClock/demo seed), chicks dispatched |
| **P4 Farm** | 07 F0–F11 · 09 notification N5–N8 · 10 FE4–FE6 | Full broiler cycle in UI incl. offline sheet, withdrawal hard block, FCI PDF |
| **P5 Slaughterhouse & Sales** | 08 S0–S10 · 10 FE9–FE10 | Reception → session → cold store → sales order → dispatch note with traceability → invoice export |
| **P6 Reporting & Traceability** | 09 reporting R0–R9 · 10 FE11 | Manager dashboard; trace from a dispatch code back to parent flock |
| **P7 HR, catalogues, costing polish** | 04 HR tasks if deferred · §D costing in all services · 10 FE12 | Rosters published; cost/kg live & cost/kg carcass; margin per customer |
| **P8 Hardening & go-live** | k8s Helm, backups/restore drill, observability dashboards, pen-test fixes, load test (NFR-P), UAT with real staff, EN translation QA, help sheets (FE13) | Production deployment at one site of each type |

Business-order note: the owner asked for the backend to be built parent flock →
hatchery → farm → slaughterhouse and that is the order above. If delivery pressure
requires the most valuable module first, **farm (P4) can be pulled before P2/P3**
without technical dependency: the chick reception accepts manual (non-announced)
receptions.

## 2. Definition of Done (every task)
- [ ] Code in the DDD package layout; no cross-context imports (ArchUnit green).
- [ ] Flyway migration(s) + entity validation (`ddl-auto=validate`) pass.
- [ ] Business rules as domain/service code with `@PreAuthorize` at service layer.
- [ ] Unit tests for rules/formulas; integration test for the happy path + each 4xx
      business code; coverage gate met.
- [ ] Domain events published through the outbox and documented in the service file §6;
      consumer side idempotent.
- [ ] `@Audited` on every write; alert codes registered in notification seed.
- [ ] OpenAPI annotations (summary, examples, error codes); generated FE client updated.
- [ ] i18n keys for new error/alert codes in RO and EN.
- [ ] Settings used via `SettingsClient` with defaults (no literals).
- [ ] Frontend screen (when in scope) responsive at 4 breakpoints, axe clean, story added.
- [ ] CHANGELOG entry (conventional commit), PR description links the plan task id.

## 3. Test matrix (minimum)

| Layer | Tool | What |
|---|---|---|
| Unit (backend) | JUnit 5, Mockito, AssertJ | formulas (laying rate, fertility, hatchability, FCR, ADG, EPEF, yield, mass balance, best-before, VAT), state machines, policies (`DeliveryPolicy`, FEFO, scope guard) |
| Architecture | ArchUnit | layers, no cross-context, policy coverage, `@PreAuthorize` presence |
| Integration (backend) | Testcontainers Postgres + Kafka, `@SpringBootTest`, MockMvc/WebTestClient | each feature's API, events out/in, role matrix (403), concurrency (`@Version` → 409), outbox relay |
| Contract | Spring Cloud Contract or JSON schema per event type in `docs/events/` + consumer tests | event payload compatibility across services |
| End-to-end backend chain | compose profile `services` + `infra/scripts/e2e-chain.sh` (REST calls with TestClock endpoints enabled in `demo` profile) | parent flock → hatchery → farm → slaughterhouse → reporting trace |
| Frontend unit | vitest + RTL + msw | components, forms, hooks, offline queue |
| Frontend e2e | Playwright (phone + desktop projects) + axe | journeys in 10 §9 |
| Performance | k6 scripts (`infra/perf`) | NFR-P: 50 concurrent users/service, p95 < 300 ms single reads, < 800 ms lists |
| Security | OWASP ZAP baseline against gateway, dependency scans, trivy | no high findings |
| Resilience | chaos: stop hatchery-service, verify farm continues (announcements lag only); Kafka down → outbox accumulates, relays after restart | NFR-A-02 |
| Backup/restore | script restores dump into fresh Postgres, services start, data intact | NFR-A-03/04 drill documented |

## 4. Demo & seed data (`infra/scripts/seed-demo.sh` → `demo` profile)
Company "Avicola Demo SRL": breeder site BS1 (2 production houses, 1 egg store),
hatchery HA1 (2 setters, 1 hatcher), farms F1 (4 houses) and F2 (2 houses),
slaughterhouse SL1 (1 line, 2 chill chambers, 1 freezer), 3 customers, SKUs (whole
fresh A, breast fillet, legs, wings), users per role (`<role>@demo.ppms` / documented
password), employees + roster for current week, catalogues (Ross 308 standards,
10 medications with withdrawal days, 6 vaccines, 5 feed types), and a time-compressed
history: one closed breeder week, one hatched incubation batch, one lot at day 25 with
a treatment ending in 3 days (so the delivery block is visible), one closed lot with
slaughter session and dispatched order (so trace works).

## 5. Go-live checklist
- [ ] Production secrets (JWT keys, DB roles, SMTP, VAPID, S3) provisioned via k8s
      secrets / external secret manager; no dev defaults.
- [ ] TLS on ingress; HSTS; CORS restricted to the SPA origin.
- [ ] Postgres backups verified by restore drill; PITR enabled where available.
- [ ] Kafka retention (7 days) and compaction for directory topics; DLT monitored.
- [ ] Grafana dashboards + alert routes (on-call e-mail/Telegram) configured.
- [ ] Settings reviewed per site with the veterinarian & production manager
      (thresholds, shelf lives, VAT, invoice series).
- [ ] Catalogue of medications with withdrawal days validated against SPC documents.
- [ ] Users & roles created; MFA enrolled for managers/admins; kiosk tablets registered.
- [ ] Printed QR labels for houses/lots/chambers; printers tested (A4 + label).
- [ ] Training: 20-minute per-role sessions using the help sheets; RO text proofread.
- [ ] Paper fallback procedure documented for outages; data back-entry endpoint allows
      backfill dates (supervisor).
- [ ] Legal retention: audit/trace tables partitioned by year; retention ≥ 5 years;
      export packs for DSVSA tested.

## 6. Risks & mitigations
| Risk | Mitigation |
|---|---|
| Event schema drift between services | versioned `eventType`, JSON schemas in `docs/events`, consumer contract tests, tolerant readers |
| Field connectivity poor | offline queue for all field forms; kiosk tablets with local Wi-Fi; sync conflicts surfaced not hidden |
| Users with low digital literacy | wizard UX, pictograms, help videos, kiosk mode, PIN login, read-back confirmation; UAT with real workers before go-live |
| Regulatory nuance (FCI content, NCP) | review FCI template & NCP flow with the company veterinarian and DSVSA guidance; keep templates editable (Thymeleaf) |
| Scope size | strict phase exits; farm module first if needed; MUST before SHOULD inside each feature |
| Kafka ops complexity | single-broker KRaft in dev; Strimzi in prod; outbox makes producers resilient; DLT alerts |
