# Poultry Production Management System (PPMS)
## Business & Requirements Documentation — Batch 1, Version 2

**Version:** 2.0
**Date:** August 2026
**Author:** Business Analysis
**Scope:** Full system — Parent Flock, Hatchery, Broiler Farm, Slaughterhouse
**Change from v1:** Parent Flock promoted to a dedicated bounded context and service.
EggSupplier simplified in Hatchery. System map, functional requirements, entity
catalogue, and repository structure updated throughout.

---

## Table of Contents

1. [Business Context & Problem Statement](#1-business-context--problem-statement)
2. [System Overview & Vision](#2-system-overview--vision)
3. [Bounded Contexts & System Boundaries](#3-bounded-contexts--system-boundaries)
4. [Actors & Roles](#4-actors--roles)
5. [Functional Requirements](#5-functional-requirements)
    - 5.1 [Parent Flock System](#51-parent-flock-system)
    - 5.2 [Hatchery System](#52-hatchery-system)
    - 5.3 [Broiler Farm System](#53-broiler-farm-system)
    - 5.4 [Slaughterhouse System](#54-slaughterhouse-system)
    - 5.5 [Cross-System / Shared](#55-cross-system--shared)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Entity Catalogue by System & Feature](#7-entity-catalogue-by-system--feature)
    - 7.1 [Parent Flock Entities](#71-parent-flock-entities)
    - 7.2 [Hatchery Entities](#72-hatchery-entities)
    - 7.3 [Broiler Farm Entities](#73-broiler-farm-entities)
    - 7.4 [Slaughterhouse Entities](#74-slaughterhouse-entities)
    - 7.5 [Shared / Cross-System Entities](#75-shared--cross-system-entities)
8. [Repository & Application Organisation](#8-repository--application-organisation)
9. [Development Roadmap](#9-development-roadmap)

---

## 1. Business Context & Problem Statement

### 1.1 Industry Background

Integrated poultry production is a vertically integrated supply chain connecting
four distinct operational units under one legal entity:

- The **Parent Flock Farm** (fermă de reproducție) — breeds hens that produce
  fertilised eggs continuously.
- The **Hatchery** (stație de incubație) — receives eggs, incubates them, and
  produces day-old chicks.
- The **Broiler Farm** (fermă de creștere) — receives chicks, grows them to
  market weight, and delivers live birds.
- The **Slaughterhouse** (abator) — receives live birds, processes them, and
  dispatches finished product to customers.

Each unit has a separate physical location, separate staff with specialised
roles, and a formal handoff point to the next unit. The company may also source
eggs from external suppliers when own production is insufficient, but the
internal parent flock operation is the primary source.

### 1.2 What Went Wrong in the Original Design

**Mistake 1 — Monolithic god systems:** Each subsystem was designed as a single
thick module covering everything. One change broke unrelated functionality.

**Mistake 2 — Missing the parent flock as a first-class domain:** The original
design buried parent flock management inside the Hatchery module, mixing two
operationally distinct units into one. In a real company, the fermă de
reproducție has its own management, its own vet, and its own budget centre. It
deserves its own bounded context.

**Mistake 3 — No clear ownership of inter-system data:** When one unit hands
off product to the next, both systems need a formal transfer order with a
tracked lifecycle (planned → confirmed → received). The original design had no
such concept, creating data integrity risks.

**Mistake 4 — Features assumed rather than derived from actual roles:** Real
poultry operations have very specific roles with very narrow responsibilities.
Modelling too broadly makes the system unusable in practice.

### 1.3 Business Goals

- Provide full traceability from fertilised egg to packaged meat (farm-to-fork
  audit trail crossing all four bounded contexts).
- Replace paper-based daily logs — mortality sheets, feed consumption records,
  treatment cards, egg collection sheets — with structured digital records.
- Enforce veterinary withdrawal periods as a hard system constraint with no
  override capability at any role level.
- Model inter-unit handoffs as formal, lifecycle-managed transfer orders so
  discrepancies between dispatched and received quantities are always visible.
- Give management a cross-system reporting layer without giving operational
  staff access beyond their own domain.
- Design the system so that Kafka-based event publishing can be added after each
  bounded context is stabilised, without requiring internal refactoring.

---

## 2. System Overview & Vision

PPMS is a collection of four independent Spring Boot microservices sharing one
common library (shared-kernel). Each service owns its own database schema. They
communicate at handoff points through persistent transfer order entities whose
status is the integration contract. Kafka will be layered on top of these status
transitions in a later phase.

```
[Parent Flock Service]
        |
        |  EggTransferOrder (status: PLANNED → DISPATCHED → RECEIVED)
        ↓
[Hatchery Service]
        |
        |  ChickTransferOrder (status: PLANNED → DISPATCHED → RECEIVED)
        ↓
[Farm Service]
        |
        |  FarmDeliveryOrder (status: PLANNED → CONFIRMED → DISPATCHED → RECEIVED)
        ↓
[Slaughterhouse Service]
        |
        |  DispatchOrder → Customer
        ↓
     [Market]

All four services depend on:
[Shared Kernel] — User, Role, AuditLog, Alert, Notification
```

### Integration Strategy (current and future)

**Now (Phase 1–5):** Transfer orders are created by the producing service and
polled or fetched by the consuming service via REST. The status field on the
transfer order is the shared contract. No broker infrastructure required.

**Later (Phase 6+):** Each status transition publishes a domain event to a Kafka
topic. Consuming services subscribe rather than poll. The internal domain model
does not change — only the delivery mechanism.

**JWT Authentication:** Each service validates JWT tokens independently using a
shared secret configured via environment variable. No dedicated auth service is
needed at this stage.

---

## 3. Bounded Contexts & System Boundaries

| Context | Core Responsibility | Does NOT Own |
|---|---|---|
| **Parent Flock** | Breeder flock management, egg production, egg quality, egg dispatch to hatchery | Anything after eggs leave the gate |
| **Hatchery** | Receiving eggs (own or external), incubation, candling, hatching, chick quality, chick dispatch to farm | How the eggs were produced; anything after chicks leave the gate |
| **Broiler Farm** | Chick reception, lot lifecycle, daily operations (mortality, feed, water, treatment), weekly performance, delivery scheduling | What happened in the egg or before; slaughter operations |
| **Slaughterhouse** | Receiving live birds, lairage, ante-mortem inspection, slaughter line, yield, cold chain, customer dispatch | Farm or hatchery operations |
| **Shared Kernel** | User accounts, roles, permissions, audit logging, alerts, notifications | Any domain-specific business logic |

### Cross-Context References — Rules

- Modules reference each other **by ID only**. No JPA `@ManyToOne` crosses a
  module boundary.
- Each module has its own representation of a shared concept. Example: the
  Hatchery has an `EggBatch` entity representing what it received. The Parent
  Flock has an `EggTransferOrder` entity representing what it sent. These are
  different entities with different responsibilities that happen to share an ID.
- The producing module always creates the transfer order. The consuming module
  updates the status on reception.

---

## 4. Actors & Roles

These roles map directly to Spring Security authorities in the RBAC model.
A user account can hold multiple roles scoped to specific operational units
(a person can be FARM_VET at Farm A but not Farm B).

### System-Wide Roles

| Role | Description |
|---|---|
| `SYSTEM_ADMIN` | Manages users, roles, and system-wide configuration. No operational write access. |
| `MANAGER` | Read access across all four bounded contexts for dashboards and reporting. Cannot create or modify operational records. |

### Parent Flock Roles

| Role | Description |
|---|---|
| `FLOCK_SUPERVISOR` | Full CRUD over parent flock records. Approves and dispatches egg transfer orders. |
| `FLOCK_WORKER` | Records daily feed and egg collection data for assigned houses. Read-only otherwise. |
| `FLOCK_VET` | Records health checks, vaccination, and biosecurity events for breeder flocks. Read-only on production data. |

### Hatchery Roles

| Role | Description |
|---|---|
| `HATCHERY_SUPERVISOR` | Full CRUD over hatchery operations. Approves and dispatches chick transfer orders. |
| `HATCHERY_OPERATOR` | Records incubation readings, candling results, and hatch counts. Cannot approve transfers. |
| `HATCHERY_VET` | Records biosecurity and health checks. Read-only on production data. |

### Broiler Farm Roles

| Role | Description |
|---|---|
| `FARM_MANAGER` | Full CRUD on farm records. Approves delivery orders to slaughterhouse. |
| `FARM_SUPERVISOR` | Manages daily operations for assigned house(s). Cannot approve deliveries. |
| `FARM_WORKER` | Records daily mortality and consumption sheets. Read-only otherwise. |
| `FARM_VET` | Prescribes and records treatments. Reads all health records. Cannot edit production data. |
| `FARM_WEIGHER` | Records weekly weight samples only. No other write access. |

### Slaughterhouse Roles

| Role | Description |
|---|---|
| `SLAUGHTER_MANAGER` | Full CRUD. Approves outbound shipments to customers. |
| `SLAUGHTER_INSPECTOR` | Records ante-mortem and post-mortem inspection results. Read-only on production data. |
| `SLAUGHTER_LINE_SUPERVISOR` | Records line throughput, yield, and carcass grading. |
| `SLAUGHTER_COLD_CHAIN` | Manages cold storage and customer dispatch. |
| `SLAUGHTER_QA` | Records quality checks and can place holds on production batches. |

---

## 5. Functional Requirements

Priority codes — **M:** Must-have (MVP), **S:** Should-have, **C:** Could-have.

---

### 5.1 Parent Flock System

#### Feature PF1 — Breeder Flock Management

| ID | Requirement | Priority |
|---|---|---|
| PF1-FR-01 | The system shall allow registration of parent flocks with breed, entry date, initial hen count, assigned house, and health status. | M |
| PF1-FR-02 | The system shall track hen count changes over time (mortality, culling events) so that the current active hen count is always accurate. | M |
| PF1-FR-03 | The system shall track flock age in weeks automatically from entry date and alert when a flock approaches the end of productive life (configurable threshold, default 60 weeks). | S |
| PF1-FR-04 | The system shall register parent flock houses with capacity (hen count), dimensions, and house status (ACTIVE / CLEANING / MAINTENANCE). | M |
| PF1-FR-05 | The system shall prevent assigning a new flock to a house that is not in ACTIVE or EMPTY status. | M |

#### Feature PF2 — Breeder Daily Operations

| ID | Requirement | Priority |
|---|---|---|
| PF2-FR-01 | The system shall allow one daily operations sheet per breeder house per calendar date. Duplicate submissions must be rejected. | M |
| PF2-FR-02 | The system shall record daily hen mortality with count and cause (disease, injury, culled, unknown). | M |
| PF2-FR-03 | The system shall record daily feed consumption per house by feed type and quantity (kg). | M |
| PF2-FR-04 | The system shall record daily egg collection: total collected, cracked, floor eggs, and net usable count. | M |
| PF2-FR-05 | The system shall calculate and display daily laying rate (eggs collected / active hens × 100) and flag values below the configured minimum (default 75%). | M |
| PF2-FR-06 | The system shall record daily water consumption (litres) per house. | S |

#### Feature PF3 — Breeder Health Management

| ID | Requirement | Priority |
|---|---|---|
| PF3-FR-01 | Only a user with FLOCK_VET role shall create health and vaccination records. | M |
| PF3-FR-02 | The system shall record vaccination events per flock: vaccine name, batch number, dose, route, date, and administering vet. | M |
| PF3-FR-03 | The system shall record biosecurity inspection results per house with findings and corrective actions. | S |
| PF3-FR-04 | The system shall record any disease events or flock health alerts with diagnosis, affected hen count, and treatment prescribed. | M |
| PF3-FR-05 | The system shall maintain a vaccination schedule per flock and alert the vet when a scheduled vaccination is due within 7 days. | S |

#### Feature PF4 — Egg Collection & Quality Management

| ID | Requirement | Priority |
|---|---|---|
| PF4-FR-01 | The system shall aggregate daily egg collection records into weekly production summaries per flock. | M |
| PF4-FR-02 | The system shall record egg quality inspection results before dispatch: crack rate, contamination rate, size distribution, and age at inspection (must be ≤ 7 days from collection). | M |
| PF4-FR-03 | The system shall reject an egg batch from being dispatched if the crack rate exceeds the configured maximum (default 3%) or egg age exceeds 7 days. | M |
| PF4-FR-04 | The system shall record egg storage conditions (temperature °C, humidity %) per storage unit while eggs await dispatch. | S |
| PF4-FR-05 | The system shall alert if storage temperature or humidity falls outside configured safe ranges. | S |

#### Feature PF5 — Egg Transfer to Hatchery

| ID | Requirement | Priority |
|---|---|---|
| PF5-FR-01 | The system shall allow the flock supervisor to create an EggTransferOrder specifying destination hatchery, planned quantity, egg source flock, and scheduled dispatch date. | M |
| PF5-FR-02 | The system shall prevent creation of an EggTransferOrder if the batch has failed quality inspection. | M |
| PF5-FR-03 | The system shall track EggTransferOrder status: PLANNED → APPROVED → DISPATCHED → RECEIVED → CANCELLED. | M |
| PF5-FR-04 | The system shall notify the destination hatchery of an incoming egg transfer at least 24 hours before the scheduled dispatch date. | S |
| PF5-FR-05 | The system shall generate a printable egg delivery note with flock traceability reference and quality inspection summary. | S |
| PF5-FR-06 | Upon dispatch confirmation, the system shall deduct the dispatched quantity from the available egg stock of that collection period. | M |

#### Feature PF6 — Parent Flock Inventory

| ID | Requirement | Priority |
|---|---|---|
| PF6-FR-01 | The system shall track consumable stocks for the parent flock operation: breeder feed (by type), vaccines, medications, litter, and cleaning agents. | M |
| PF6-FR-02 | The system shall record stock receipts with supplier, batch number, expiration date, and quantity. | M |
| PF6-FR-03 | The system shall alert when any consumable falls below its configured reorder point. | M |
| PF6-FR-04 | The system shall consume feed and medication stocks on a FIFO basis (earliest expiry first). | S |

---

### 5.2 Hatchery System

#### Feature H1 — Egg Reception

| ID | Requirement | Priority |
|---|---|---|
| H1-FR-01 | The system shall receive an EggTransferOrder from the Parent Flock Service and create an EggReception record on arrival. | M |
| H1-FR-02 | The system shall record the actual received quantity, which may differ from the ordered quantity due to transit damage, with discrepancy notes. | M |
| H1-FR-03 | The system shall support egg reception from external suppliers (not only from the Parent Flock Service). External supplier eggs are registered manually with supplier reference and delivery note number. | M |
| H1-FR-04 | The system shall create an EggBatch record upon reception regardless of source (internal transfer or external supplier), providing a uniform entity for downstream incubation processes. | M |
| H1-FR-05 | The system shall record a hatchery-side incoming quality check on received eggs before accepting them into stock. | M |
| H1-FR-06 | The system shall reject an EggBatch that fails incoming quality inspection and notify the source accordingly. | M |

#### Feature H2 — Incubation Management

| ID | Requirement | Priority |
|---|---|---|
| H2-FR-01 | The system shall create an IncubationBatch when eggs are set in an incubator, linking it to the source EggBatch and recording set date, incubator, and set quantity. | M |
| H2-FR-02 | The system shall record candling results at day 7 and day 18 with fertile, infertile, and dead-in-shell counts. | M |
| H2-FR-03 | The system shall calculate fertility rate and estimated hatch yield after each candling event. | M |
| H2-FR-04 | The system shall record per-shift environmental readings per incubator: temperature (°C), humidity (%), CO₂ (ppm), and turning frequency. | S |
| H2-FR-05 | The system shall alert the hatchery supervisor if any environmental reading falls outside configured safe ranges. | S |
| H2-FR-06 | The system shall support the transfer of eggs from setter to hatcher at day 18, updating incubator occupancy accordingly. | M |

#### Feature H3 — Hatch Recording

| ID | Requirement | Priority |
|---|---|---|
| H3-FR-01 | The system shall record hatch results per IncubationBatch: total hatched, DOA chicks, culled weak chicks, and saleable chicks. | M |
| H3-FR-02 | The system shall calculate hatch rate (hatched / fertile eggs set × 100) and flag batches below the configured minimum (default 80%). | M |
| H3-FR-03 | The system shall record chick quality grading counts per grade (A, B, C, Reject). | M |
| H3-FR-04 | The system shall link every hatch record to its originating IncubationBatch and through it to the originating EggBatch and flock for full traceability. | M |

#### Feature H4 — Chick Transfer to Farm

| ID | Requirement | Priority |
|---|---|---|
| H4-FR-01 | The system shall allow the hatchery supervisor to create a ChickTransferOrder specifying destination farm, destination house, grade counts, and scheduled dispatch date. | M |
| H4-FR-02 | The system shall enforce that only Grade A and Grade B chicks are dispatched unless the farm manager has set an explicit Grade C acceptance flag on the destination lot. | M |
| H4-FR-03 | The system shall track ChickTransferOrder status: PLANNED → APPROVED → DISPATCHED → RECEIVED → CANCELLED. | M |
| H4-FR-04 | Upon dispatch, the system shall deduct the dispatched chick count from the available saleable chick stock. | M |
| H4-FR-05 | The system shall notify the destination farm of an incoming chick transfer at least 24 hours before dispatch. | S |
| H4-FR-06 | The system shall generate a printable chick delivery note with full traceability back to the originating egg batch and parent flock. | S |

#### Feature H5 — Hatchery Inventory

| ID | Requirement | Priority |
|---|---|---|
| H5-FR-01 | The system shall track hatchery consumable stocks: fumigants, hatchery vaccines, spray solutions, and cleaning agents. | M |
| H5-FR-02 | The system shall record stock receipts with supplier, batch number, expiration date, and quantity. | M |
| H5-FR-03 | The system shall alert when any consumable falls below its configured reorder point. | M |
| H5-FR-04 | The system shall consume stocks on a FIFO basis (earliest expiry first). | S |

---

### 5.3 Broiler Farm System

#### Feature F1 — Farm & House Setup

| ID | Requirement | Priority |
|---|---|---|
| F1-FR-01 | The system shall allow registration of farms with name, location, and owner contact. | M |
| F1-FR-02 | The system shall allow registration of poultry houses per farm with capacity, dimensions, and ventilation type. | M |
| F1-FR-03 | The system shall track house occupancy status: EMPTY, ACTIVE, CLEANING, MAINTENANCE. | M |
| F1-FR-04 | The system shall prevent assigning a new lot to a house that is not in EMPTY status. | M |

#### Feature F2 — Chick Reception & Lot Creation

| ID | Requirement | Priority |
|---|---|---|
| F2-FR-01 | The system shall allow the farm supervisor to receive a ChickTransferOrder and record the actual received quantities, which may differ from dispatched due to transit mortality. | M |
| F2-FR-02 | The system shall automatically create a Lot upon reception with a unique code (format: FARM-HOUSE-YYYY-MM-DD) linked to the originating ChickTransferOrder. | M |
| F2-FR-03 | The system shall record reception quality assessment: alive on arrival, DOA, and weak counts. | M |
| F2-FR-04 | The system shall validate that the receiving house has sufficient capacity for the received quantity. | M |
| F2-FR-05 | The system shall change house status to ACTIVE upon successful lot creation. | M |

#### Feature F3 — Daily Operations

| ID | Requirement | Priority |
|---|---|---|
| F3-FR-01 | The system shall allow one daily operations sheet per house per calendar date. Duplicate submissions for the same house-date must be rejected. | M |
| F3-FR-02 | The system shall record daily mortality count, cause (disease, suffocation, crushing, unknown, other), and disposal method (composting, incineration, rendering). | M |
| F3-FR-03 | The system shall maintain a running current-bird-count per lot (initial count minus cumulative mortality minus cumulative partial deliveries). | M |
| F3-FR-04 | The system shall record daily feed consumption (kg) by feed type and calculate per-bird consumption (g/bird/day). | M |
| F3-FR-05 | The system shall record daily water consumption (litres) and flag abnormal per-bird values (below 2× feed consumption ratio) as an alert. | S |
| F3-FR-06 | The system shall link daily feed and water consumption to specific inventory stock batches for traceability. | S |

#### Feature F4 — Veterinary Treatment Management

| ID | Requirement | Priority |
|---|---|---|
| F4-FR-01 | Only a user with FARM_VET role shall create treatment records. | M |
| F4-FR-02 | The system shall record treatments with: medication name, stock batch, dose per bird, route of administration, start date, duration (days), and prescribing vet. | M |
| F4-FR-03 | The system shall automatically calculate withdrawal period end date (treatment end date + withdrawal days configured per medication). | M |
| F4-FR-04 | The system shall hard-block creation of a delivery order for any lot with an active or unexpired withdrawal period. This block cannot be overridden by any role, including FARM_MANAGER and SYSTEM_ADMIN. | M |
| F4-FR-05 | The system shall alert the farm manager 48 hours before a withdrawal period expires. | S |
| F4-FR-06 | The system shall consume medication from inventory stock (FIFO) when a treatment is recorded, and reject the treatment if sufficient stock is not available. | M |

#### Feature F5 — Weekly Performance Monitoring

| ID | Requirement | Priority |
|---|---|---|
| F5-FR-01 | The system shall allow a FARM_WEIGHER to record weekly weight samples per lot with sample size (number of birds weighed) and total sample weight. | M |
| F5-FR-02 | The system shall calculate per lot per week: Cumulative Mortality Rate (%), Average Daily Gain (ADG g/day), and Feed Conversion Ratio (FCR = total feed kg / total estimated live weight kg). | M |
| F5-FR-03 | The system shall compare calculated KPIs against configurable breed-specific performance standards and generate alerts for deviations beyond the configured tolerance. | S |
| F5-FR-04 | The system shall produce a weekly performance summary per lot accessible to FARM_MANAGER and MANAGER roles. | M |
| F5-FR-05 | The system shall assess delivery eligibility weekly: a lot is eligible when average live weight ≥ target weight AND no active withdrawal period exists. | M |

#### Feature F6 — Delivery Management

| ID | Requirement | Priority |
|---|---|---|
| F6-FR-01 | The system shall allow the farm manager to create a FarmDeliveryOrder for an eligible lot specifying destination slaughterhouse, scheduled date, and planned bird count. | M |
| F6-FR-02 | The system shall validate withdrawal period clearance before allowing delivery order creation. This is a hard block with no override. | M |
| F6-FR-03 | The system shall track FarmDeliveryOrder status: PLANNED → CONFIRMED (by slaughterhouse) → DISPATCHED → RECEIVED → CANCELLED. | M |
| F6-FR-04 | The system shall support partial deliveries: multiple delivery orders from the same lot on different dates, each deducting from the running bird count. | M |
| F6-FR-05 | The system shall notify the destination slaughterhouse of an incoming delivery upon order creation. | S |
| F6-FR-06 | Upon lot completion (all birds delivered or lot manually closed), the system shall change the house status to CLEANING. | M |

#### Feature F7 — Farm Inventory Management

| ID | Requirement | Priority |
|---|---|---|
| F7-FR-01 | The system shall maintain stock of: feed (by type), medications, vaccines, litter material, and cleaning agents. | M |
| F7-FR-02 | The system shall record stock receipts with supplier, delivery note number, batch number, expiration date, and quantity. | M |
| F7-FR-03 | The system shall alert when any critical stock (feed in active use, medications with ongoing treatments) falls below its configured reorder point. | M |
| F7-FR-04 | The system shall consume feed and medication stocks on a FIFO basis (earliest expiry first). | M |
| F7-FR-05 | The system shall record a quality inspection result (pass/fail with notes) for each received stock batch. | S |

---

### 5.4 Slaughterhouse System

#### Feature S1 — Live Bird Reception

| ID | Requirement | Priority |
|---|---|---|
| S1-FR-01 | The system shall receive a FarmDeliveryOrder and create a LiveBirdReception record upon truck arrival. | M |
| S1-FR-02 | The system shall record arrival time, truck registration, transport duration, transport temperature, and driver identity. | M |
| S1-FR-03 | The system shall record actual received bird count versus ordered count, with discrepancies flagged for review and notification sent to the originating farm. | M |
| S1-FR-04 | The system shall record lairage start and end times and validate that minimum resting duration (configurable, default 2 hours) is met before clearing for slaughter. | M |

#### Feature S2 — Ante-Mortem Inspection

| ID | Requirement | Priority |
|---|---|---|
| S2-FR-01 | Only a user with SLAUGHTER_INSPECTOR role shall record inspection results. | M |
| S2-FR-02 | The system shall record ante-mortem inspection outcome: passed count, condemned count with reason, inspection outcome status, and inspector identity. | M |
| S2-FR-03 | The system shall allow an inspector to place a reception batch on HOLD pending veterinary authority review. | M |
| S2-FR-04 | The system shall prevent a batch from entering the slaughter line until ante-mortem inspection is recorded and outcome is PASSED or PARTIALLY_PASSED. | M |

#### Feature S3 — Slaughter Line Operations

| ID | Requirement | Priority |
|---|---|---|
| S3-FR-01 | The system shall create a SlaughterSession per batch processed, linked to the LiveBirdReception record. | M |
| S3-FR-02 | The system shall record start time, end time, line number, line speed (birds/hour), and line supervisor. | M |
| S3-FR-03 | The system shall record total birds slaughtered, total live weight (truck scale), and total hot carcass weight. | M |
| S3-FR-04 | The system shall calculate Slaughter Yield % (hot carcass weight / live weight × 100) and alert when below the configured minimum (default 74%). | M |
| S3-FR-05 | The system shall record post-mortem condemnation counts and weights by category (septicaemia, airsacculitis, contamination, bruising, other). | M |

#### Feature S4 — Carcass Grading & Further Processing

| ID | Requirement | Priority |
|---|---|---|
| S4-FR-01 | The system shall record carcass grading output: Grade A whole bird count and weight, Grade B count and weight, and by-product (offal) weight. | M |
| S4-FR-02 | The system shall support recording of cut production (breast fillet, thigh, drumstick, wing) with weight per cut type per session. | S |
| S4-FR-03 | The system shall link each production batch to its originating SlaughterSession, and through it to the originating farm lot, hatchery batch, and parent flock, for full traceability. | M |
| S4-FR-04 | The system shall record packaging details: pack type, weight per pack, total packs, and best-before date calculated as slaughter date + configured shelf life per product type. | S |

#### Feature S5 — Cold Chain & Storage Management

| ID | Requirement | Priority |
|---|---|---|
| S5-FR-01 | The system shall register cold-store chambers with capacity (kg), current stock (computed), and target temperature range. | M |
| S5-FR-02 | The system shall record stock-in events when graded product enters cold storage, and stock-out events on dispatch. | M |
| S5-FR-03 | The system shall alert if a chamber temperature reading falls outside the configured safe range. | S |
| S5-FR-04 | The system shall prevent dispatch of any batch whose best-before date has passed. | M |

#### Feature S6 — Customer Dispatch

| ID | Requirement | Priority |
|---|---|---|
| S6-FR-01 | The system shall allow the slaughterhouse manager to create a DispatchOrder to a customer specifying product lines, quantities, and delivery date. | M |
| S6-FR-02 | The system shall maintain a customer register with name, category (retailer, wholesaler, restaurant, export), VAT number, and contact details. | M |
| S6-FR-03 | The system shall generate a dispatch note carrying traceability codes that trace back through slaughter session → farm lot → hatchery batch → parent flock. | M |
| S6-FR-04 | The system shall deduct dispatched quantities from the relevant cold-store chamber stock upon dispatch confirmation. | M |

---

### 5.5 Cross-System / Shared

#### Feature X1 — User & Access Management

| ID | Requirement | Priority |
|---|---|---|
| X1-FR-01 | The system shall maintain one user account per person. Roles are assigned scoped to specific operational units (a user can hold FARM_VET at Farm A but not Farm B). | M |
| X1-FR-02 | The system shall enforce password complexity: minimum 8 characters, at least one uppercase letter, one digit, one special character. | M |
| X1-FR-03 | The system shall lock an account after 5 consecutive failed login attempts. Only a SYSTEM_ADMIN can unlock it. | M |
| X1-FR-04 | The system shall issue JWT tokens with a configurable expiry (default 8 hours). Each service validates the JWT independently using a shared secret. | M |
| X1-FR-05 | The system shall maintain an append-only audit log of every write operation: actor, action type, entity type, entity ID, change payload, timestamp, and IP address. | M |

#### Feature X2 — Notifications & Alerts

| ID | Requirement | Priority |
|---|---|---|
| X2-FR-01 | The system shall store in-app notifications per user with read/unread status. | M |
| X2-FR-02 | The system shall optionally send email notifications for CRITICAL alerts (withdrawal period active on delivery attempt, stock below reorder point, KPI breach). | S |
| X2-FR-03 | Each alert shall have a severity level: INFO, WARNING, or CRITICAL. | M |
| X2-FR-04 | CRITICAL alerts shall require an acknowledgement with a mandatory reason before they can be dismissed. | S |

#### Feature X3 — Reporting & Dashboard

| ID | Requirement | Priority |
|---|---|---|
| X3-FR-01 | The system shall provide a cross-system dashboard for MANAGER role showing: active lots per farm, scheduled deliveries, cold-store occupancy, and open critical alerts. | S |
| X3-FR-02 | The system shall support CSV export for all list-view data. | S |
| X3-FR-03 | The system shall provide a traceability lookup endpoint: given a dispatch batch code, return the full chain back to the originating parent flock and egg batch. | C |

---

## 6. Non-Functional Requirements

### 6.1 Performance

| ID | Requirement |
|---|---|
| NFR-P-01 | Single-record read endpoints shall respond within 300 ms under normal load (up to 50 concurrent users per service). |
| NFR-P-02 | Paginated list endpoints (max 100 items per page) shall respond within 800 ms. |
| NFR-P-03 | Report generation and traceability chain lookups shall complete within 5 seconds. |
| NFR-P-04 | Each service shall handle up to 200 concurrent users independently without degradation. |

### 6.2 Availability & Reliability

| ID | Requirement |
|---|---|
| NFR-A-01 | Each service shall target 99.5% uptime during operational hours (06:00–22:00 local time). |
| NFR-A-02 | A failure in one service shall not cascade to other services. The farm service must remain operational if the hatchery service is down. |
| NFR-A-03 | The system shall perform automated database backups every 4 hours. Recovery Point Objective (RPO) ≤ 4 hours. |
| NFR-A-04 | Recovery Time Objective (RTO) per service from a full failure shall be ≤ 2 hours. |

### 6.3 Security

| ID | Requirement |
|---|---|
| NFR-S-01 | All API communication shall use HTTPS with TLS 1.2 minimum. |
| NFR-S-02 | Passwords shall be stored using bcrypt with a cost factor of ≥ 12. |
| NFR-S-03 | All endpoints shall require a valid JWT token except explicitly listed public endpoints (login, actuator health check). |
| NFR-S-04 | Role-based access control shall be enforced at the service layer (@PreAuthorize), not only at the controller layer. |
| NFR-S-05 | The withdrawal period hard-block (F4-FR-04 and F6-FR-02) shall be implemented as a domain invariant checked in the domain service, not only as an API validation. |
| NFR-S-06 | The audit log shall be append-only. No role, including SYSTEM_ADMIN, shall be permitted to update or delete audit records. |
| NFR-S-07 | The system shall protect against OWASP Top 10 vulnerabilities: parameterised queries for SQL injection prevention, input validation on all DTOs, and no sensitive data in JWT payloads. |

### 6.4 Maintainability & Scalability

| ID | Requirement |
|---|---|
| NFR-M-01 | The system shall be structured as four independent Spring Boot services plus one shared-kernel library, each in its own Maven module within a multi-module parent POM. |
| NFR-M-02 | Each service shall own its own database schema. No JPA entity shall reference a table in another service's schema. Cross-service references shall be by stored ID only. |
| NFR-M-03 | Cross-service communication shall use persistent transfer order status as the integration contract. This design shall be compatible with future replacement by Kafka event publishing without requiring internal domain model changes. |
| NFR-M-04 | Service-layer business logic shall have ≥ 70% unit test coverage. Integration tests shall use Testcontainers for a real PostgreSQL instance. |
| NFR-M-05 | All public API endpoints shall be documented via OpenAPI 3.0 (Springdoc). Each service exposes its own /swagger-ui at its own port. |
| NFR-M-06 | Each service shall be independently containerisable. A docker-compose file shall start all four services plus a shared PostgreSQL instance for local development. |

### 6.5 Usability (API Contract for Future Frontend)

| ID | Requirement |
|---|---|
| NFR-U-01 | All error responses shall use a consistent envelope: { timestamp, status, error, message, path }. |
| NFR-U-02 | All list endpoints shall support pagination (page, size), sorting (sort, direction), and domain-relevant filters. |
| NFR-U-03 | Validation errors on 400 responses shall return a per-field error array: [{ field, rejectedValue, message }]. |
| NFR-U-04 | All date-time fields in API responses shall use ISO 8601 format in UTC. |

### 6.6 Compliance & Regulatory

| ID | Requirement |
|---|---|
| NFR-C-01 | Veterinary withdrawal periods shall be enforced as a domain invariant. No execution path in any service shall allow a delivery order to be created for a lot with an active withdrawal period. |
| NFR-C-02 | All traceability records shall be retained for a minimum of 5 years in accordance with EU Regulation 178/2002 on food traceability. |
| NFR-C-03 | Slaughter inspection records shall store the identity of the inspector, who must hold a registered SLAUGHTER_INSPECTOR role in the system. |
| NFR-C-04 | The system shall support export of records in structured CSV format for veterinary authority audits. |

---

## 7. Entity Catalogue by System & Feature

For each entity: Name, role in the feature, and key fields (indicative, not
final schema). Cross-service references are stored as plain ID fields — no
foreign key constraints across schema boundaries.

---

### 7.1 Parent Flock Entities

#### Feature PF1 — Breeder Flock Management

| Entity | Role | Key Fields |
|---|---|---|
| `ParentFlockHouse` | Physical breeder house. Resource entity. Tracks its own occupancy status. | `id`, `houseCode`, `capacity` (hens), `lengthM`, `widthM`, `status` (EMPTY / ACTIVE / CLEANING / MAINTENANCE) |
| `ParentFlock` | Root aggregate for a breeder flock. Tracks active hen count and productive life. | `id`, `flockCode`, `breed`, `entryDate`, `initialHenCount`, `currentHenCount` (maintained by events), `houseId`, `healthStatus` (HEALTHY / QUARANTINE / CULLED), `expectedCullWeek` |
| `FlockHenMortalityEvent` | Each recorded hen mortality or culling event. Updates currentHenCount on ParentFlock. | `id`, `flockId`, `eventDate`, `count`, `cause` (DISEASE / INJURY / CULLED / UNKNOWN), `recordedBy` |

#### Feature PF2 — Breeder Daily Operations

| Entity | Role | Key Fields |
|---|---|---|
| `BreederDailySheet` | One-per-house-per-date aggregate for all daily breeder records. Enforces uniqueness. | `id`, `flockId`, `operationDate`, `submittedBy`, `submittedAt`, `status` (DRAFT / SUBMITTED) |
| `BreederFeedRecord` | Daily feed consumption per house. Child of BreederDailySheet. | `id`, `dailySheetId`, `feedStockBatchId`, `feedType`, `quantityKg` |
| `EggCollectionRecord` | Daily egg collection result. Child of BreederDailySheet. | `id`, `dailySheetId`, `totalCollected`, `crackedCount`, `floorEggCount`, `netUsableCount`, `layingRatePercent` (calculated), `isLayingRateBelowThreshold` |
| `BreederWaterRecord` | Daily water consumption. Child of BreederDailySheet. | `id`, `dailySheetId`, `quantityLitres` |

#### Feature PF3 — Breeder Health Management

| Entity | Role | Key Fields |
|---|---|---|
| `BreederVaccinationRecord` | Records a vaccination event on a flock. Only FLOCK_VET can create. | `id`, `flockId`, `vaccineStockBatchId`, `vaccineName`, `batchNumber`, `dosePerBird`, `route`, `vaccinationDate`, `administeredBy` |
| `BreederHealthEvent` | Records disease events or health alerts on a flock. | `id`, `flockId`, `eventDate`, `diagnosis`, `affectedHenCount`, `treatmentPrescribed`, `recordedBy` |
| `BiosecurityInspection` | Biosecurity check result per house. | `id`, `houseId`, `inspectionDate`, `findings`, `correctiveActions`, `inspectedBy` |
| `VaccinationScheduleEntry` | Planned vaccination event for a flock (drives alert generation). | `id`, `flockId`, `vaccineName`, `scheduledWeekOfAge`, `scheduledDate`, `status` (PENDING / COMPLETED / OVERDUE) |

#### Feature PF4 — Egg Collection & Quality Management

| Entity | Role | Key Fields |
|---|---|---|
| `WeeklyEggProductionSummary` | Aggregated weekly egg production per flock. Computed by scheduler. | `id`, `flockId`, `weekNumber`, `year`, `totalCollected`, `totalCracked`, `totalFloorEggs`, `totalNetUsable`, `averageDailyLayingRate`, `generatedAt` |
| `EggStorageUnit` | Physical egg storage room or cabinet. Resource entity. | `id`, `unitCode`, `capacityDozens`, `targetMinTempC`, `targetMaxTempC` |
| `EggStorageConditionLog` | Periodic temperature and humidity readings for a storage unit. | `id`, `storageUnitId`, `recordedAt`, `temperatureCelsius`, `humidityPercent`, `isOutOfRange`, `recordedBy` |
| `EggQualityInspection` | Pre-dispatch quality inspection result for a collection period. | `id`, `flockId`, `inspectionDate`, `periodStart`, `periodEnd`, `sampleSize`, `crackRatePercent`, `contaminationRatePercent`, `ageAtInspectionDays`, `outcome` (PASSED / REJECTED), `rejectionReason`, `inspectedBy` |

#### Feature PF5 — Egg Transfer to Hatchery

| Entity | Role | Key Fields |
|---|---|---|
| `EggTransferOrder` | Integration entity between Parent Flock and Hatchery. Lifecycle-managed. | `id`, `transferCode`, `sourceFlockId`, `eggQualityInspectionId`, `destinationHatcheryId`, `plannedQuantity`, `scheduledDispatchDate`, `status` (PLANNED / APPROVED / DISPATCHED / RECEIVED / CANCELLED), `approvedBy`, `dispatchedAt`, `actualDispatchedQuantity` |
| `EggDeliveryNote` | Printable/exportable transfer document linked to a dispatched order. | `id`, `transferOrderId`, `generatedAt`, `documentReference`, `generatedBy` |

#### Feature PF6 — Parent Flock Inventory

| Entity | Role | Key Fields |
|---|---|---|
| `FlockConsumableType` | Catalogue of consumable types for parent flock operations. | `id`, `name`, `category` (FEED / VACCINE / MEDICATION / LITTER / CLEANING), `unit`, `reorderPoint` |
| `FlockStockBatch` | A received delivery of a consumable with full batch traceability. | `id`, `consumableTypeId`, `supplierId`, `receiptDate`, `batchNumber`, `expiryDate`, `quantityReceived`, `quantityRemaining`, `qualityInspectionStatus` (PASS / FAIL) |
| `FlockStockTransaction` | Each stock deduction. FIFO enforced at service layer. | `id`, `stockBatchId`, `transactionDate`, `quantityUsed`, `relatedEntityType` (DAILY_SHEET / VACCINATION), `relatedEntityId`, `performedBy` |

---

### 7.2 Hatchery Entities

#### Feature H1 — Egg Reception

| Entity | Role | Key Fields |
|---|---|---|
| `EggSupplier` | External egg supplier reference. Used only when eggs come from outside the company. | `id`, `supplierName`, `registrationNumber`, `contactPerson`, `phone`, `certificationExpiryDate` |
| `EggReception` | Records the physical arrival of an egg batch at the hatchery gate. | `id`, `sourceType` (INTERNAL_TRANSFER / EXTERNAL_SUPPLIER), `eggTransferOrderId` (nullable, set for internal), `supplierId` (nullable, set for external), `supplierDeliveryNote` (nullable), `arrivalDate`, `orderedQuantity`, `receivedQuantity`, `discrepancyNotes`, `receivedBy` |
| `EggBatch` | Hatchery's representation of a received egg batch. Uniform entry point for incubation regardless of source. | `id`, `batchCode`, `eggReceptionId`, `quantity`, `incomingQualityStatus` (ACCEPTED / REJECTED), `rejectionReason`, `availableForSetting` (quantity not yet assigned to incubation) |

#### Feature H2 — Incubation Management

| Entity | Role | Key Fields |
|---|---|---|
| `Incubator` | Physical incubation machine. Resource entity tracking occupancy. | `id`, `incubatorCode`, `type` (SETTER / HATCHER / COMBINED), `capacity`, `currentLoad`, `status` (IDLE / IN_USE / MAINTENANCE) |
| `IncubationBatch` | Core aggregate for one incubation cycle. Links egg batch to hatcher. | `id`, `batchCode`, `eggBatchId`, `setterId`, `hatcherId` (nullable, set at transfer), `setDate`, `setQuantity`, `expectedTransferDate`, `expectedHatchDate`, `status` (INCUBATING / TRANSFERRED_TO_HATCHER / HATCHED / FAILED) |
| `CandlingRecord` | Candling result at day 7 or day 18. Child of IncubationBatch. | `id`, `incubationBatchId`, `candlingDay`, `fertileCount`, `infertileCount`, `deadInShellCount`, `estimatedHatchYield` (calculated), `conductedBy`, `conductedAt` |
| `IncubatorEnvironmentLog` | Per-shift environmental readings. | `id`, `incubatorId`, `loggedAt`, `temperatureCelsius`, `humidityPercent`, `co2Ppm`, `turningFrequency`, `isOutOfRange`, `loggedBy` |

#### Feature H3 — Hatch Recording

| Entity | Role | Key Fields |
|---|---|---|
| `HatchRecord` | Hatch outcome for one IncubationBatch. One per batch. | `id`, `incubationBatchId`, `hatchDate`, `totalHatched`, `doaCount`, `culledWeakCount`, `saleableCount`, `hatchRatePercent` (calculated), `isBelowMinimum`, `recordedBy` |
| `ChickGradeCount` | Grade breakdown within a HatchRecord. | `id`, `hatchRecordId`, `grade` (A / B / C / REJECT), `count` |

#### Feature H4 — Chick Transfer to Farm

| Entity | Role | Key Fields |
|---|---|---|
| `ChickTransferOrder` | Integration entity between Hatchery and Farm. | `id`, `transferCode`, `hatchRecordId`, `destinationFarmId`, `destinationHouseId`, `gradeACount`, `gradeBCount`, `gradeCCount`, `totalPlanned`, `scheduledDispatchDate`, `status` (PLANNED / APPROVED / DISPATCHED / RECEIVED / CANCELLED), `approvedBy`, `approvedAt`, `actualDispatchedCount` |
| `ChickDeliveryNote` | Printable document with full traceability. | `id`, `transferOrderId`, `generatedAt`, `documentReference`, `traceabilitySummary` (JSON: flock → egg batch → incubation batch), `generatedBy` |

#### Feature H5 — Hatchery Inventory

| Entity | Role | Key Fields |
|---|---|---|
| `HatcheryConsumableType` | Consumable catalogue for hatchery. | `id`, `name`, `category` (VACCINE / DISINFECTANT / FUMIGANT / OTHER), `unit`, `reorderPoint` |
| `HatcheryStockBatch` | Received stock batch. | `id`, `consumableTypeId`, `supplierId`, `receiptDate`, `batchNumber`, `expiryDate`, `quantityReceived`, `quantityRemaining`, `qualityStatus` (PASSED / FAILED) |
| `HatcheryStockTransaction` | Stock deduction event. FIFO enforced at service layer. | `id`, `stockBatchId`, `usedAt`, `quantityUsed`, `relatedEntityType`, `relatedEntityId`, `usedBy` |

---

### 7.3 Broiler Farm Entities

#### Feature F1 — Farm & House Setup

| Entity | Role | Key Fields |
|---|---|---|
| `Farm` | Top-level organisational unit. Owner of all farm sub-entities. | `id`, `farmCode`, `name`, `location`, `ownerName`, `ownerPhone`, `registrationNumber` |
| `PoultryHouse` | Physical growing house. Tracks real-time occupancy status. | `id`, `farmId`, `houseNumber`, `capacity`, `lengthM`, `widthM`, `ventilationType` (NATURAL / TUNNEL / FORCED), `status` (EMPTY / ACTIVE / CLEANING / MAINTENANCE) |

#### Feature F2 — Chick Reception & Lot Creation

| Entity | Role | Key Fields |
|---|---|---|
| `ChickReception` | Farm-side record of a received ChickTransferOrder. | `id`, `chickTransferOrderId`, `receivingFarmId`, `receivingHouseId`, `receptionDate`, `orderedCount`, `receivedAliveCount`, `doaOnArrivalCount`, `weakOnArrivalCount`, `discrepancyNotes`, `receivedBy` |
| `Lot` | Central aggregate for the farm module. Lifecycle of one chick batch in one house. | `id`, `lotCode`, `chickReceptionId`, `poultryHouseId`, `startDate`, `initialBirdCount`, `currentBirdCount` (maintained), `status` (ACTIVE / COMPLETED / CLOSED_EARLY), `closedAt` |

#### Feature F3 — Daily Operations

| Entity | Role | Key Fields |
|---|---|---|
| `DailyOperationSheet` | One-per-house-per-date aggregate. Uniqueness enforced. | `id`, `lotId`, `operationDate`, `submittedBy`, `submittedAt`, `status` (DRAFT / SUBMITTED) |
| `MortalityRecord` | Daily mortality entry. Child of DailyOperationSheet. | `id`, `dailySheetId`, `mortalityCount`, `cause`, `causeNotes`, `disposalMethod` |
| `FeedConsumptionRecord` | Daily feed usage. Child of DailyOperationSheet. | `id`, `dailySheetId`, `feedStockBatchId`, `feedType`, `quantityKg`, `perBirdConsumptionG` (calculated) |
| `WaterConsumptionRecord` | Daily water usage. Child of DailyOperationSheet. | `id`, `dailySheetId`, `quantityLitres`, `perBirdConsumptionL` (calculated), `isAbnormal` |

#### Feature F4 — Veterinary Treatment Management

| Entity | Role | Key Fields |
|---|---|---|
| `Medication` | Medication catalogue. Reference data. | `id`, `name`, `activeSubstance`, `form` (ORAL / INJECTABLE / WATER_SOLUBLE), `withdrawalDays`, `registrationNumber` |
| `TreatmentRecord` | Treatment on a lot. Only FARM_VET can create. | `id`, `lotId`, `medicationId`, `medicationStockBatchId`, `prescribingVetUserId`, `startDate`, `durationDays`, `endDate` (calculated), `withdrawalEndDate` (calculated), `dosePerBird`, `routeOfAdministration`, `totalMedicationUsed` |

#### Feature F5 — Weekly Performance Monitoring

| Entity | Role | Key Fields |
|---|---|---|
| `WeightSampleRecord` | Weekly weight samples. Only FARM_WEIGHER can create. | `id`, `lotId`, `weekNumber`, `sampleDate`, `sampleSize`, `totalSampleWeightG`, `averageWeightG` (calculated), `recordedBy` |
| `WeeklyPerformanceSummary` | Calculated KPI snapshot per lot per week. Generated by scheduler. | `id`, `lotId`, `weekNumber`, `cumulativeMortalityRate`, `averageDailyGainG`, `feedConversionRatio`, `totalFeedConsumedKg`, `estimatedTotalLiveWeightKg`, `deliveryEligible`, `generatedAt` |

#### Feature F6 — Delivery Management

| Entity | Role | Key Fields |
|---|---|---|
| `FarmDeliveryOrder` | Integration entity between Farm and Slaughterhouse. | `id`, `deliveryCode`, `lotId`, `destinationSlaughterhouseId`, `plannedBirdCount`, `scheduledDate`, `withdrawalClearanceVerifiedAt`, `status` (PLANNED / CONFIRMED / DISPATCHED / RECEIVED / CANCELLED), `createdBy`, `approvedBy` |

#### Feature F7 — Farm Inventory Management

| Entity | Role | Key Fields |
|---|---|---|
| `FarmConsumableType` | Consumable catalogue per farm. | `id`, `farmId`, `name`, `category` (FEED / MEDICATION / VACCINE / LITTER / CLEANING), `unit`, `reorderPoint` |
| `FarmStockBatch` | Received stock batch with full traceability. | `id`, `consumableTypeId`, `supplierId`, `deliveryNoteNumber`, `receiptDate`, `batchNumber`, `expiryDate`, `quantityReceived`, `quantityRemaining`, `qualityInspectionStatus` (PASS / FAIL) |
| `FarmStockTransaction` | Stock deduction event. FIFO enforced at service layer. | `id`, `stockBatchId`, `transactionDate`, `quantityUsed`, `relatedEntityType` (DAILY_SHEET / TREATMENT), `relatedEntityId`, `performedBy` |

---

### 7.4 Slaughterhouse Entities

#### Feature S1 — Live Bird Reception

| Entity | Role | Key Fields |
|---|---|---|
| `Slaughterhouse` | Physical facility. Reference entity. | `id`, `name`, `location`, `licenceNumber`, `capacityBirdsPerDay` |
| `LiveBirdReception` | Arrival record linked to a FarmDeliveryOrder. | `id`, `farmDeliveryOrderId`, `arrivalDateTime`, `truckRegistration`, `driverName`, `transportDurationHours`, `transportTempCelsius`, `orderedCount`, `receivedCount`, `discrepancyNotes`, `status` (ARRIVED / IN_LAIRAGE / CLEARED / ON_HOLD / REJECTED) |
| `LairageRecord` | Resting period record. | `id`, `receptionId`, `lairageStart`, `lairageEnd`, `durationMinutes` (calculated), `meetsMinimumRequirement` |

#### Feature S2 — Ante-Mortem Inspection

| Entity | Role | Key Fields |
|---|---|---|
| `AntemortemInspection` | Inspection before slaughter. Only SLAUGHTER_INSPECTOR can create. | `id`, `receptionId`, `inspectedBy`, `inspectionDateTime`, `passedCount`, `condemnedCount`, `condemnationReason`, `outcome` (PASSED / PARTIALLY_PASSED / CONDEMNED / ON_HOLD), `notes` |
| `InspectionHold` | Active hold record pending authority review. | `id`, `receptionId`, `placedBy`, `placedAt`, `reason`, `resolvedBy`, `resolvedAt`, `resolution` (CLEARED / CONDEMNED) |

#### Feature S3 — Slaughter Line Operations

| Entity | Role | Key Fields |
|---|---|---|
| `SlaughterLine` | Physical processing line. Resource entity. | `id`, `lineNumber`, `capacityBirdsPerHour`, `status` (ACTIVE / MAINTENANCE) |
| `SlaughterSession` | One processing run of one reception batch on one line. | `id`, `antemortemInspectionId`, `slaughterLineId`, `supervisorId`, `startTime`, `endTime`, `birdsSlaughtered`, `totalLiveWeightKg`, `totalHotCarcassWeightKg`, `slaughterYieldPercent` (calculated), `isBelowMinimumYield`, `status` (IN_PROGRESS / COMPLETED) |
| `PostmortemCondemnation` | Post-slaughter condemnation by category. Child of SlaughterSession. | `id`, `slaughterSessionId`, `category` (SEPTICAEMIA / AIRSACCULITIS / CONTAMINATION / BRUISING / OTHER), `count`, `weightKg`, `recordedBy` |

#### Feature S4 — Carcass Grading & Further Processing

| Entity | Role | Key Fields |
|---|---|---|
| `CarcassGradingRecord` | Grading output of a session. | `id`, `slaughterSessionId`, `gradeACount`, `gradeAWeightKg`, `gradeBCount`, `gradeBWeightKg`, `offalWeightKg`, `recordedBy`, `recordedAt` |
| `ProductionBatch` | Traceable batch of finished product. Root of dispatch traceability. | `id`, `batchCode`, `slaughterSessionId`, `productType` (WHOLE_BIRD / BREAST_FILLET / THIGH / DRUMSTICK / WING / OFFAL), `grade`, `totalWeightKg`, `bestBeforeDate`, `status` (IN_PRODUCTION / IN_STORAGE / DISPATCHED) |
| `PackagingRecord` | Packaging detail per ProductionBatch. | `id`, `productionBatchId`, `packType`, `weightPerPackKg`, `totalPacks`, `labelledAt`, `labelledBy` |

#### Feature S5 — Cold Chain & Storage Management

| Entity | Role | Key Fields |
|---|---|---|
| `ColdStoreChamber` | Physical cold-store unit. Resource entity. | `id`, `chamberCode`, `capacityKg`, `currentStockKg` (computed), `targetMinTempC`, `targetMaxTempC`, `status` (OPERATIONAL / DEFROST / MAINTENANCE) |
| `ColdStoreEntry` | Product stock-in event. | `id`, `chamberId`, `productionBatchId`, `entryDateTime`, `quantityKg`, `enteredBy` |
| `ColdStoreExit` | Product stock-out event on dispatch. | `id`, `chamberId`, `productionBatchId`, `exitDateTime`, `quantityKg`, `relatedDispatchOrderId`, `exitedBy` |
| `ChamberTemperatureLog` | Periodic temperature reading. | `id`, `chamberId`, `recordedAt`, `temperatureCelsius`, `isOutOfRange`, `recordedBy` |

#### Feature S6 — Customer Dispatch

| Entity | Role | Key Fields |
|---|---|---|
| `Customer` | End buyer of finished product. | `id`, `name`, `category` (RETAILER / WHOLESALER / RESTAURANT / EXPORT), `vatNumber`, `contactPerson`, `phone`, `email`, `address` |
| `DispatchOrder` | Outbound order to a customer. | `id`, `dispatchCode`, `customerId`, `scheduledDispatchDate`, `status` (PLANNED / LOADED / DISPATCHED / DELIVERED), `createdBy`, `approvedBy` |
| `DispatchOrderLine` | One product line within a DispatchOrder. | `id`, `dispatchOrderId`, `productionBatchId`, `quantityKg` |
| `DispatchNote` | Printable transport document with full traceability chain. | `id`, `dispatchOrderId`, `vehicleRegistration`, `driverName`, `traceabilityChainJson` (snapshot: customer ← dispatch ← production batch ← slaughter session ← farm lot ← hatchery batch ← egg batch ← parent flock), `generatedAt`, `generatedBy` |

---

### 7.5 Shared / Cross-System Entities

#### Feature X1 — User & Access Management

| Entity | Role | Key Fields |
|---|---|---|
| `User` | System account. One per person. | `id`, `username`, `passwordHash`, `email`, `isActive`, `isLocked`, `failedLoginAttempts`, `passwordExpiresAt`, `lastLoginAt` |
| `Employee` | Person record independent of system access. | `id`, `firstName`, `lastName`, `nationalId`, `phone`, `jobTitle`, `hireDate` |
| `Role` | Named permission set scoped to a module. | `id`, `roleName`, `description`, `module` (PARENT_FLOCK / HATCHERY / FARM / SLAUGHTERHOUSE / SHARED) |
| `UserRoleAssignment` | Scoped role assignment. A user can hold a role at one operational unit but not another. | `id`, `userId`, `roleId`, `scopeEntityType` (FLOCK_HOUSE / FARM / HATCHERY / SLAUGHTERHOUSE), `scopeEntityId`, `assignedBy`, `assignedAt`, `expiresAt` (nullable) |
| `AuditLog` | Append-only record of every write operation across all services. | `id`, `userId`, `serviceModule`, `action` (CREATE / UPDATE / DELETE), `entityType`, `entityId`, `changesJson`, `performedAt`, `ipAddress` |

#### Feature X2 — Notifications & Alerts

| Entity | Role | Key Fields |
|---|---|---|
| `Alert` | System alert for a business rule breach or threshold crossing. | `id`, `severity` (INFO / WARNING / CRITICAL), `sourceModule`, `alertCode`, `relatedEntityType`, `relatedEntityId`, `message`, `generatedAt`, `status` (OPEN / ACKNOWLEDGED / DISMISSED) |
| `Notification` | Per-user in-app message derived from an alert or event. | `id`, `userId`, `alertId` (nullable), `subject`, `body`, `isRead`, `readAt`, `createdAt` |
| `AlertAcknowledgement` | Required acknowledgement record for CRITICAL alerts. | `id`, `alertId`, `acknowledgedBy`, `acknowledgedAt`, `acknowledgementReason` |

---

## 8. Repository & Application Organisation

### 8.1 Repository Structure

```
poultry-production-management-system/
│
├── docs/
│   ├── business/
│   │   └── requirements-v2.md          ← This document
│   ├── api/                            ← OpenAPI specs per service (generated)
│   └── diagrams/                       ← ERD, sequence diagrams (Batch 2)
│
├── shared-kernel/                      ← Plain Maven JAR library, no main class
│   ├── pom.xml
│   └── src/main/java/com/ppms/shared/
│       ├── domain/                     ← User, Employee, Role, Alert, Notification
│       ├── security/                   ← JWT utility, token validator
│       ├── audit/                      ← AuditLog entity + AOP interceptor
│       └── exception/                  ← Global error envelope, base exceptions
│
├── parent-flock-service/               ← Spring Boot app — port 8080
│   ├── pom.xml                         ← Depends on shared-kernel
│   └── src/main/java/com/ppms/parentflock/
│       ├── domain/
│       │   ├── model/                  ← ParentFlock, BreederDailySheet, EggTransferOrder, ...
│       │   ├── valueobject/
│       │   ├── repository/
│       │   └── event/                  ← EggBatchDispatched (future Kafka event)
│       ├── application/
│       │   ├── service/
│       │   ├── dto/
│       │   └── mapper/
│       ├── infrastructure/
│       │   ├── persistence/
│       │   └── scheduler/              ← Weekly summary generation, alert checks
│       └── api/
│           └── controller/
│
├── hatchery-service/                   ← Spring Boot app — port 8081
│   ├── pom.xml
│   └── src/main/java/com/ppms/hatchery/
│       ├── domain/
│       │   ├── model/                  ← EggBatch, IncubationBatch, HatchRecord, ChickTransferOrder, ...
│       │   ├── valueobject/
│       │   ├── repository/
│       │   └── event/                  ← ChickBatchDispatched (future Kafka event)
│       ├── application/
│       ├── infrastructure/
│       │   ├── persistence/
│       │   ├── integration/            ← REST client to parent-flock-service (EggTransferOrder status)
│       │   └── scheduler/
│       └── api/
│
├── farm-service/                       ← Spring Boot app — port 8082
│   ├── pom.xml
│   └── src/main/java/com/ppms/farm/
│       ├── domain/
│       │   ├── model/                  ← Lot, DailyOperationSheet, TreatmentRecord, FarmDeliveryOrder, ...
│       │   ├── valueobject/
│       │   ├── repository/
│       │   └── event/                  ← DeliveryOrderConfirmed (future Kafka event)
│       ├── application/
│       ├── infrastructure/
│       │   ├── persistence/
│       │   ├── integration/            ← REST client to hatchery-service (ChickTransferOrder status)
│       │   └── scheduler/              ← Weekly KPI generation, withdrawal period alerts
│       └── api/
│
├── slaughterhouse-service/             ← Spring Boot app — port 8083
│   ├── pom.xml
│   └── src/main/java/com/ppms/slaughterhouse/
│       ├── domain/
│       │   ├── model/                  ← LiveBirdReception, SlaughterSession, ProductionBatch, DispatchOrder, ...
│       │   ├── valueobject/
│       │   ├── repository/
│       │   └── event/                  ← SlaughterYieldRecorded (future Kafka event)
│       ├── application/
│       ├── infrastructure/
│       │   ├── persistence/
│       │   ├── integration/            ← REST client to farm-service (FarmDeliveryOrder status)
│       │   └── scheduler/
│       └── api/
│
├── docker/
│   ├── docker-compose.yml              ← All 4 services + PostgreSQL (4 schemas)
│   └── Dockerfile                      ← Multi-stage build template (one per service)
│
├── .github/
│   └── workflows/
│       ├── ci.yml                      ← Build + test on every PR (all modules)
│       └── release.yml                 ← Package on main merge
│
├── pom.xml                             ← Parent POM: version management only
└── .gitignore
```

### 8.2 Within Each Service — Package Convention

```
com.ppms.<service>/
│
├── domain/
│   ├── model/          ← @Entity classes (JPA). All fields package-private or private.
│   ├── valueobject/    ← @Embeddable value objects. No @Id.
│   ├── repository/     ← Interfaces extending JpaRepository. No implementation here.
│   └── event/          ← Plain POJOs representing domain events. No JPA annotations.
│
├── application/
│   ├── service/        ← Business logic. One service class per aggregate root.
│   │                      @Transactional lives here, not in controllers.
│   ├── dto/            ← Request and Response DTOs. No @Entity annotations.
│   └── mapper/         ← MapStruct interfaces for entity ↔ DTO conversion.
│
├── infrastructure/
│   ├── persistence/    ← Custom @Query methods, specifications (if needed).
│   ├── integration/    ← REST clients (RestTemplate or WebClient) to other services.
│   └── scheduler/      ← @Scheduled jobs: KPI generation, alert checks, summaries.
│
└── api/
    ├── controller/     ← @RestController. Thin layer: validate input, call service, return DTO.
    └── exception/      ← @ControllerAdvice for this service's specific exceptions.
```

### 8.3 Database Strategy

- One PostgreSQL instance, four schemas: `parent_flock`, `hatchery`, `farm`, `slaughterhouse`, plus `shared` for User/Role/AuditLog.
- Cross-schema foreign keys are **not used**. References between contexts are plain `UUID` or `BIGINT` ID columns with no DB-level constraint enforcing referential integrity across schemas.
- Migrations managed by **Flyway** per service. Each service only manages migrations for its own schema.
- Migration naming convention: `V<MODULE_PREFIX><VERSION>__<description>.sql`
  Example: `VPF001__create_parent_flock_tables.sql`, `VF005__add_withdrawal_end_date.sql`

### 8.4 Technology Stack

| Concern | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Security | Spring Security + JWT (jjwt) |
| API Docs | Springdoc OpenAPI 3.0 (per service, own port) |
| Mapping | MapStruct |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Build | Maven (multi-module parent POM) |
| Containerisation | Docker + docker-compose |
| CI | GitHub Actions |
| Future messaging | Apache Kafka (after Phase 5) |

---

## 9. Development Roadmap

Build in this order to have a runnable, tested endpoint at the end of every phase.

| Phase | Scope | Deliverable |
|---|---|---|
| **0 — Foundation** | Parent POM, shared-kernel, User/Role/JWT/AuditLog, docker-compose with PostgreSQL | Auth endpoints working, JWT validated across all four service stubs |
| **1 — Farm Core** | Farm, PoultryHouse, ChickReception, Lot (F1 + F2) | Lot creation from a manually seeded ChickTransferOrder |
| **2 — Farm Daily Ops** | DailyOperationSheet, Mortality, Feed, Water, TreatmentRecord with withdrawal block (F3 + F4), Farm Inventory (F7) | Full daily operations cycle with withdrawal period enforcement tested |
| **3 — Farm Performance & Delivery** | WeightSampleRecord, WeeklyPerformanceSummary, FarmDeliveryOrder lifecycle (F5 + F6) | End-to-end lot lifecycle: reception → daily ops → weekly KPIs → delivery order |
| **4 — Hatchery** | EggReception, EggBatch, IncubationBatch, HatchRecord, ChickTransferOrder, Hatchery Inventory (H1–H5) | ChickTransferOrder received by Farm service, Lot created automatically |
| **5 — Parent Flock** | ParentFlock, BreederDailySheet, EggQualityInspection, EggTransferOrder, Flock Inventory (PF1–PF6) | Full chain: parent flock → egg transfer → hatchery → chick transfer → farm lot |
| **6 — Slaughterhouse** | LiveBirdReception, AntemortemInspection, SlaughterSession, CarcassGrading, ColdChain, DispatchOrder (S1–S6) | Full end-to-end traceability: parent flock → dispatch note |
| **7 — Cross-system** | Alerts, Notifications, Dashboard endpoints, Traceability lookup (X1–X3) | MANAGER dashboard, full audit trail, CSV exports |
| **8 — Kafka** | Replace REST polling between services with Kafka topic publishing on status transitions | Same domain model, event-driven delivery |
| **9 — Frontend** | React SPA starting with Farm module (most feature-rich), then Slaughterhouse, then Hatchery, then Parent Flock | Usable UI for operational staff |

---

*End of Batch 1 Version 2 — Business & Requirements Documentation.*

*Next — Batch 2: ERD diagrams per bounded context and feature-level entity
relationship mapping showing cardinality and ownership.*