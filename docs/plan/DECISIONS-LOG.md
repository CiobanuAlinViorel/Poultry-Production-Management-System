# Decisions Log

Decisions agreed with the product owner before the plan was written. Append new entries
(date, decision, reason) when an implementer must deviate from the plan.

| # | Date | Decision | Reason / notes |
|---|---|---|---|
| D1 | 2026-10-17 | Existing specs (`docs/specs/*`) are the baseline but features were **re-derived from industrial practice**; the plan adds FCI, NCP Salmonella, HACCP CCPs, sales/invoicing, HR/shifts, costing, kiosk/PIN, etc. | Owner request: model a real integrator (Avicola-type) |
| D2 | 2026-10-17 | `identity-service` existing code is **kept and extended** in its current package (`com.example.identity_service`); all other services use `com.ppms.<ctx>` | Avoid churn on working code |
| D3 | 2026-10-17 | **API Gateway** (Spring Cloud Gateway) is the single origin for the SPA | SPA simplicity, edge JWT validation, resilience per module |
| D4 | 2026-10-17 | Scope = **all** requirements (Must + Should + Could) | Owner choice |
| D5 | 2026-10-17 | **Kafka from day one** with transactional outbox; no REST polling between contexts | Owner choice; outbox keeps producers resilient |
| D6 | 2026-10-17 | Frontend: **Vite + React + TS + MUI**, TanStack Query, RHF+zod, react-i18next, Recharts, PWA (dexie offline queue) | Owner chose MUI; rest selected for maturity/accessibility |
| D7 | 2026-10-17 | Languages **RO (default) + EN**; must work on **all devices** and be usable by staff with no screen experience | Owner request |
| D8 | 2026-10-17 | Infra target: docker-compose (dev/prod profiles) + GitHub Actions CI **+ Kubernetes Helm chart** | Owner choice |
| D9 | 2026-10-17 | **Each domain service owns its own site registry**; admin-service keeps a Kafka-fed site directory and stores `scopeId` as opaque UUID | Owner choice; keeps contexts autonomous |
| D10 | 2026-10-17 | Adjacent areas in scope: **costing/economics, sales & invoicing data, HR/shifts**; IoT sensor ingestion only as `source=SENSOR` reading endpoints (no device management) | Owner multi-select |
| D11 | 2026-10-17 | Documents: **server-side PDF** (openhtmltopdf) + **CSV/XLSX** exports, stored in MinIO/S3 | Owner choice |
| D12 | 2026-10-17 | Plan delivered as `docs/plan/` multi-file set | Owner choice |
| D13 | 2026-10-17 | Dev Postgres: **one container, one database per service** (replaces the 5-container compose); prod identical database names | Simpler local setup; still schema-per-service isolation |
| D14 | 2026-10-17 | New modules: `gateway-service`, `notification-service`, `reporting-service` | Alerts/notifications and cross-context reporting need their own stores and Kafka consumers |
| D15 | 2026-10-17 | Veterinary withdrawal, best-before and ante-mortem checks are **non-overridable domain invariants**; only the lairage-minimum rule has an audited welfare override | Food-safety law; welfare exception is real practice (heat stress) |
