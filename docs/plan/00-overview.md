# PPMS — Master Implementation Plan — 00 Overview

**Audience:** any LLM or developer implementing the system. Every file in `docs/plan/` is
self-contained enough to be handed to an implementer alone, but this overview defines the
vocabulary, conventions and order that all other files assume. Read this first.

**Target customer:** a vertically integrated industrial poultry company in Romania (model:
Avicola-type integrator) running breeder farms, a hatchery, broiler farms and a
slaughterhouse under one legal entity, with several physical sites per unit type.

---

## 0. How to use this plan

1. Files are numbered in **implementation order**. Do not start a later file before the
   "Exit criteria" of the previous one are met, unless the file says it is independent.
2. Each service file has the same skeleton:
   `Business process → Roles → Domain model → Business rules → API → Events → Jobs →
   Migrations → Tests → Feature-by-feature task list (with acceptance criteria)`.
3. Every task list item is written as a unit of work that fits in one PR: entity +
   migration + repository + service + controller + tests. Implement them top-down.
4. "Decision" boxes record choices already made with the product owner. Do not reopen
   them; if something blocks, add a note to `docs/plan/DECISIONS-LOG.md` (create it) and
   continue with the recorded choice.
5. Business constants (thresholds, limits) are always **configurable per site** via
   `admin-service` system settings (see `04-admin-service.md` §7), with the defaults
   stated in this plan. Never hard-code them in domain code.

---

## 1. What the application delivers

PPMS digitalises the whole production chain **from the breeder hen to the packed carcass
leaving the slaughterhouse**, replacing paper registers (fișe de mortalitate, fișe de
consum, registre de tratamente, fișe de colectare ouă, registre de incubație, documente
de însoțire) with structured, auditable records, while:

- enforcing food-safety law as **hard domain invariants** (veterinary withdrawal periods,
  ante-mortem clearance, lairage time, best-before dates, FCI document before slaughter);
- giving **full farm-to-fork traceability** (dispatch batch → slaughter session → farm lot
  → chick transfer → hatch → incubation batch → egg batch → egg transfer → parent flock);
- producing the **operational KPIs** that the industry actually manages by (laying rate,
  hatching-egg %, fertility, hatchability, chick quality, mortality, ADG, FCR, EPEF,
  stocking density, slaughter yield, condemnation %, cold-store occupancy);
- adding **cost & margin visibility** (cost/hatching egg, cost/chick, cost/kg live weight,
  cost/kg carcass, margin per dispatch);
- supporting **sales data** (customers, price lists, sales orders, invoice/aviz data) and
  **workforce data** (employees, shifts, who was on duty when a record was made);
- being usable on **any device** by staff with low digital literacy (large touch targets,
  guided step-by-step forms, pictograms + words, RO/EN).

Out of scope (explicitly): accounting ledger integration, payroll, feed-mill production,
vehicle fleet management, external retailer EDI. Integration points for these are left as
documented extension hooks only.

---

## 2. The real production process the system models

Numbers below are industry practice for Ross 308 / Cobb 500 type genetics in EU
conditions; they are the **defaults** for configurable standards.

### 2.1 Parent flock (fermă de reproducție)
- Parent stock arrives as day-old chicks (females + ~10–12% males), reared 0–20/22 weeks
  (rearing houses), then transferred to production houses; lay starts ~23–24 weeks,
  peak ~85–87% at weeks 30–32, flock depopulated at 60–64 weeks.
- Daily per house: mortality & culls (hens / males separately), feed (kg, by feed phase),
  water (L), egg collection several times per day: total, **hatching eggs** (settable),
  floor eggs, cracked, dirty, double-yolk, small (<50 g) → non-settable.
- KPIs: laying rate = eggs / live hens ×100; hatching-egg % = settable / total; eggs per
  hen housed (cumulative); feed per hen per day (g); body weight uniformity samples
  (weekly, % within ±10% of mean); male:female ratio.
- Eggs are **fumigated/sanitised**, stored at 15–18 °C / 75–80 % RH, pointed end down,
  **ideally set within 7 days** (fertility drops ~0.5–1 %/day after day 7); storage >10
  days is rejected or down-graded.
- Vaccination programme (Marek at hatchery, ND, IB, IBD, AE, Reo, Salmonella, etc.),
  blood sampling (serology) and the **Salmonella National Control Programme** sampling.
- Egg dispatch to hatchery with a delivery note (aviz) + flock traceability reference and
  quality summary.

### 2.2 Hatchery (stație de incubație)
- Egg reception (own flocks or external suppliers), incoming check (cracks, dirt, weight,
  age), storage, **pre-warming** 6–12 h, **setting** in setters (37.6–37.8 °C, 53–57 %
  RH, turning hourly) for 18 days, **candling** (day 7–10: clears/infertile + early dead;
  at **transfer day 18**: late dead, remove), transfer to **hatchers** (36.8–37.2 °C,
  rising RH), **hatch/pull at day 21**, chick processing (grading A/B/C/cull, sexing if
  needed, in-ovo or spray/subcutaneous vaccination), boxing (100/box), dispatch at ≤ 24 h
  of age in climate-controlled trucks.
- KPIs: fertility % (fertile/set), hatchability of eggs set (saleable chicks/set), hatch
  of fertile (%), embryonic mortality by phase, chick quality score, egg weight loss
  (target 11–13 % to day 18), chick yield (chick wt / egg wt ≈ 67–68 %).
- Incubator readings per shift (temp, RH, CO₂, turning), machine maintenance.

### 2.3 Broiler farm (fermă de creștere)
- House prepared (cleaned, disinfected, litter, pre-heated to 32–34 °C floor), chicks
  placed; one **lot** per house per cycle; cycle 35–42 days to 2.2–2.8 kg; **downtime**
  (vid sanitar) 10–14 days.
- Daily per house: mortality & culls with cause, feed (kg by phase: starter/grower/
  finisher/withdrawal), water (L), min/max temperature, RH, ammonia, litter condition,
  flock behaviour; weekly: weight samples (≥ 50–100 birds or automatic scales),
  uniformity (CV %).
- KPIs: cumulative mortality %, ADG (g/day), FCR (kg feed / kg live weight; 1.5–1.7),
  water:feed ratio (1.7–2.0), **EPEF** (European Production Efficiency Factor =
  liveability × avg weight(kg) / (age × FCR) × 100; 350–450), stocking density
  (kg/m², legal max 33, 39 with derogation, 42 with extra criteria — Directive
  2007/43/EC), thinning (partial depopulation) at ~day 32–35 then final at 40–42.
- Veterinary: treatments (antibiotics, coccidiostats in feed, vaccines via water) with
  **withdrawal period** per product; **no delivery while withdrawal active** (hard block,
  zero override).
- Pre-slaughter: feed withdrawal 8–12 h before catching (water stays on), catching crew,
  crates/modules, truck weigh-in; **Food Chain Information (FCI)** document (Reg. (EC)
  853/2004 Annex II Sect. III) sent ≥ 24 h before arrival; movement document / veterinary
  transport certificate.
- Sanitation: cleaning & disinfection record, Salmonella swab results before next
  placement.

### 2.4 Slaughterhouse (abator)
- Truck arrival, weigh-in (gross), unloading, **lairage** (rest, ventilation; min 1–2 h,
  max ≈ 12 h total transport+lairage), **ante-mortem inspection** by the official/
  authorised veterinarian (DSVSA), **stunning** (water-bath or CAS) → bleeding → scalding
  → plucking → evisceration → **post-mortem inspection** (condemnations by cause) →
  chilling (air/spin; carcass ≤ 4 °C) → grading (A/B whole) → cutting (breast fillet,
  thighs, drumsticks, wings, back) → packaging & labelling (lot code, best-before) →
  cold storage (0–4 °C, or freezing ≤ −18 °C) → dispatch to customers with traceability
  on the dispatch note / invoice.
- KPIs: live weight received vs declared, DOA %, slaughter yield (hot carcass / live,
  72–76 %), chilled yield, condemnation % by cause, line speed (birds/h), cut-up yields
  (breast ~22–25 % of live), cold-store occupancy, FIFO/FEFO compliance, customer
  on-time %.
- Compliance: Reg. (EC) 853/2004, 1099/2009 (welfare at killing), 2073/2005
  (microbiological criteria: Salmonella/Campylobacter sampling on neck skins), 178/2002
  traceability (records ≥ 5 years), HACCP CCPs (scalding temp, chill temp, metal
  detection).

### 2.5 Hand-offs = transfer orders (integration contract)
```
ParentFlock  --EggTransferOrder-->  Hatchery  --ChickTransferOrder-->  Farm
Farm  --FarmDeliveryOrder (+FCI)-->  Slaughterhouse  --DispatchOrder-->  Customer
```
Status lifecycle of every transfer: `PLANNED → APPROVED → DISPATCHED → RECEIVED`
(+ `CANCELLED` from any non-terminal state, + `REJECTED` from RECEIVED when the receiver
refuses the goods). The **producer owns the order**; the receiver records a reception
that references it by ID and publishes an event; the producer updates its copy of status
from that event.

---

## 3. Architecture

### 3.1 Services (Spring Boot 3.3, Java 21, Maven multi-module, DDD per bounded context)

| Module | Port | DB schema | Owns |
|---|---|---|---|
| `shared-kernel` | lib | — | BaseEntity, errors, security (JWT RS256 validation), event envelope, outbox, audit, pagination, test support |
| `identity-service` | 8084 | `identity` | users, credentials, sessions/refresh tokens, login audit, password policy, JWKS |
| `admin-service` | 8085 | `admin` | roles, permissions, scoped role assignments, employees, shifts/rosters, system settings, feature flags |
| `parent-flock-service` | 8081 | `parent_flock` | breeder sites/houses, flocks, daily sheets, health, eggs, storage, egg transfer orders, inventory, costing |
| `hatchery-service` | 8082 | `hatchery` | hatchery sites, egg reception/batches, setters/hatchers, incubation, candling, hatch, chick grading, chick transfer orders, inventory, costing |
| `farm-service` | 8083 | `farm` | farms, houses, chick reception, lots, daily sheets, treatments, weights/KPIs, delivery orders + FCI, inventory, sanitation, costing |
| `slaughterhouse-service` | 8086 | `slaughterhouse` | reception, lairage, inspections, sessions, grading, cutting, packaging, cold chain, customers, price lists, sales orders, dispatch, invoice data, costing |
| `notification-service` | 8087 | `notification` | alerts (severity, ack), in-app notifications, email (SMTP), web-push, subscription rules |
| `reporting-service` | 8088 | `reporting` | Kafka-fed read models: manager dashboard, cross-chain traceability, KPI history, cost roll-ups, CSV/XLSX/PDF exports |
| `gateway-service` | 8080 | — | Spring Cloud Gateway: single origin for the SPA, JWT validation at the edge, routing, rate-limit, CORS |

> Decision: `notification-service`, `reporting-service` and `gateway-service` are new
> modules added by this plan. `identity-service` existing code is **kept and extended**
> (package `com.example.identity_service` stays; new services use `com.ppms.<ctx>`).
> Each domain service **owns its own site registry** (farms, hatcheries, ...);
> `admin-service` stores `scopeId` as an opaque UUID and resolves names via a
> Kafka-fed `site_directory` read model (see 04 §6).

### 3.2 Communication
- **Synchronous:** SPA → gateway → service (REST/JSON, OpenAPI 3). Service-to-service
  sync calls are limited to: identity→admin (`effective-roles` at token mint) and
  any-service→admin (`settings` cached, with local fallback defaults).
- **Asynchronous (Kafka, from day one):** every state change that another context cares
  about is a domain event written to an **outbox table in the same transaction** and
  relayed to Kafka (Debezium-free, polling relay in shared-kernel). Topics and envelope
  in `02-shared-kernel.md` §5. Consumers are idempotent (`processed_event` table).
- **Reference data:** consumer services keep **local read models** (e.g. farm-service
  has `hatchery_directory` with hatchery id/name, slaughterhouse-service has
  `farm_directory`) fed by `*.site.registered` events, so dropdowns work offline from
  other services (NFR: no cascade failure).

### 3.3 Cross-cutting conventions (apply in every service)
- Package layout: `domain/{model,enums,repository,event}`, `application/{service,dto/
  request,dto/response,mapper}`, `infrastructure/{persistence,messaging,integration,
  scheduler,pdf}`, `api/{controller,exception}`, `config`.
- Entities extend `BaseEntity` (`UUID id`, `createdAt`, `updatedAt`, `createdBy`,
  `updatedBy`, `@Version long version`). Soft delete only where business needs history
  (`deletedAt`), otherwise status fields.
- Cross-context references are **UUID columns only** (no FK, no JPA relation).
- Business rules live in domain/service layer, guarded by `@PreAuthorize` at **service**
  layer; controllers are thin.
- Every write endpoint is `@Audited(action="...")` → append-only `audit_log` in the
  service schema + `ppms.audit` Kafka topic (reporting aggregates).
- Error envelope `{timestamp,status,error,code,message,path,fieldErrors[]}`; business
  errors carry a stable `code` (e.g. `FARM_WITHDRAWAL_ACTIVE`) the frontend maps to i18n.
- Lists: `?page&size(≤100)&sort=field,dir&<filters>` → `PageResponse<T>`.
- IDs are UUIDs; human codes exist for everything operators touch (lot code, batch
  code, order number) and are generated server-side with site-prefixed sequences.
- Dates: `LocalDate` for business days, `Instant` (UTC ISO-8601) for timestamps; site
  timezone `Europe/Bucharest` used for "today" computations.
- Flyway per service, `V<PREFIX><NNN>__desc.sql` (`ID`, `AD`, `PF`, `HA`, `FA`, `SL`,
  `NO`, `RP`). Repeatable `R__seed_*.sql` only for reference data.
- Tests: JUnit 5 + Mockito unit tests for every rule; `@SpringBootTest` + Testcontainers
  (Postgres + Kafka) integration tests per feature; ≥ 70 % service-layer coverage
  (JaCoCo gate in CI).
- OpenAPI via springdoc at `/v3/api-docs`, swagger-ui behind gateway at
  `/docs/<service>`.

### 3.4 Roles (Spring authorities = `ROLE_<name>`, each assignment scoped)
System: `SYSTEM_ADMIN`, `MANAGER`(read-all), `HR_ADMIN`, `SALES`(slaughterhouse sales),
`ACCOUNTANT`(read costing/invoices, export).
Parent flock: `FLOCK_SUPERVISOR`, `FLOCK_WORKER`, `FLOCK_VET`, `FLOCK_EGG_ROOM`.
Hatchery: `HATCHERY_SUPERVISOR`, `HATCHERY_OPERATOR`, `HATCHERY_VET`, `HATCHERY_QC`.
Farm: `FARM_MANAGER`, `FARM_SUPERVISOR`, `FARM_WORKER`, `FARM_VET`, `FARM_WEIGHER`,
`FARM_STOREKEEPER`.
Slaughterhouse: `SLAUGHTER_MANAGER`, `SLAUGHTER_RECEPTION`, `SLAUGHTER_INSPECTOR`,
`SLAUGHTER_LINE_SUPERVISOR`, `SLAUGHTER_COLD_CHAIN`, `SLAUGHTER_QA`, `SLAUGHTER_SALES`.
Scope types: `GLOBAL`, `BREEDER_SITE`, `HATCHERY`, `FARM`, `SLAUGHTERHOUSE`.
Fine-grained **permissions** (`farm:daily-sheet:write`) are attached to roles and
embedded in the JWT (`perms` claim) so services can check either.

---

## 4. Implementation order (summary — details in `11-roadmap.md`)

| # | File | Deliverable |
|---|---|---|
| 1 | `01-infrastructure.md` | repo hygiene, docker-compose (Postgres, Kafka, Mailpit, MinIO, observability), Dockerfiles, CI, k8s skeleton |
| 2 | `02-shared-kernel.md` | library every service compiles against |
| 3 | `03-identity-service.md` | login/refresh/logout, user admin, password reset, MFA-ready, JWKS |
| 4 | `04-admin-service.md` | roles/permissions/assignments, employees & shifts, settings, site directory |
| 5 | `05-parent-flock-service.md` | PF1–PF8 |
| 6 | `06-hatchery-service.md` | H1–H7 |
| 7 | `07-farm-service.md` | F1–F9 |
| 8 | `08-slaughterhouse-service.md` | S1–S9 (incl. sales/invoicing) |
| 9 | `09-cross-cutting-services.md` | gateway, notification, reporting/traceability/costing |
| 10 | `10-frontend.md` | React SPA (MUI), PWA, RO/EN, module by module |
| 11 | `11-roadmap.md` | phases, milestones, DoD, test matrix, go-live checklist |

---

## 5. Glossary (RO ↔ EN used in UI and code)

| EN (code) | RO (UI default) |
|---|---|
| Parent flock / breeder flock | Efectiv de reproducție / lot părinți |
| Hatching egg (settable) | Ou de incubație (incubabil) |
| Setter / Hatcher | Incubator (setter) / Eclozionator |
| Candling | Ovoscopie (mirare) |
| Hatch | Ecloziune |
| Day-old chick | Pui de o zi |
| Lot (broiler flock in a house) | Lot / serie |
| Daily sheet | Fișă zilnică |
| Mortality / culls | Mortalitate / eliminări (sacrificări de necesitate) |
| Feed conversion ratio (FCR) | Indice de conversie (IC) |
| Withdrawal period | Timp de așteptare |
| Food Chain Information (FCI) | Informații privind lanțul alimentar (ILA) |
| Delivery note | Aviz de însoțire a mărfii |
| Lairage | Zonă de așteptare (odihnă) |
| Ante-/post-mortem inspection | Inspecție ante-/post-mortem |
| Condemnation | Confiscat |
| Carcass yield | Randament la sacrificare |
| Cold store | Depozit frigorific |
| Best-before | Data limită de consum / a se consuma de preferință înainte de |
| Dispatch order | Comandă de livrare |
| Reorder point | Stoc minim de reaprovizionare |
| Sanitary break (downtime) | Vid sanitar |
