# Parent Flock Service — Full Specification
## PPMS · Bounded Context: Parent Flock (Fermă de Reproducție)
**Version:** 1.0  
**Based on:** Requirements v2 + industry research correction  
**Status:** Ready for development review

---

## Table of Contents

1. [Real-World Context & What the Original Spec Missed](#1-real-world-context--what-the-original-spec-missed)
2. [Flock Lifecycle — The Two Phases](#2-flock-lifecycle--the-two-phases)
3. [Egg Classification — The Heart of the Matter](#3-egg-classification--the-heart-of-the-matter)
4. [Egg Disposition — Where the Eggs Go](#4-egg-disposition--where-the-eggs-go)
5. [Bounded Context Boundary](#5-bounded-context-boundary)
6. [Actors & Roles](#6-actors--roles)
7. [Functional Requirements (Revised & Extended)](#7-functional-requirements-revised--extended)
    - 7.1 [PF1 — Breeder Flock Management (with Rearing Phase)](#71-pf1--breeder-flock-management-with-rearing-phase)
    - 7.2 [PF2 — Breeder Daily Operations](#72-pf2--breeder-daily-operations)
    - 7.3 [PF3 — Health & Veterinary Management](#73-pf3--health--veterinary-management)
    - 7.4 [PF4 — Egg Collection & Classification](#74-pf4--egg-collection--classification)
    - 7.5 [PF5 — Egg Storage Management](#75-pf5--egg-storage-management)
    - 7.6 [PF6 — Egg Disposition: Transfer to Hatchery](#76-pf6--egg-disposition-transfer-to-hatchery)
    - 7.7 [PF7 — Egg Disposition: External Commercial Sale (NEW)](#77-pf7--egg-disposition-external-commercial-sale-new)
    - 7.8 [PF8 — Inventory Management](#78-pf8--inventory-management)
    - 7.9 [PF9 — Performance Reporting & KPIs](#79-pf9--performance-reporting--kpis)
8. [Entity Catalogue (Revised)](#8-entity-catalogue-revised)
9. [Business Rules Summary](#9-business-rules-summary)
10. [API Endpoints Overview](#10-api-endpoints-overview)
11. [What Changes Relative to v2 Requirements](#11-what-changes-relative-to-v2-requirements)

---

## 1. Real-World Context & What the Original Spec Missed

### What the original spec got right

The original v2 document correctly identified that the parent flock is a distinct bounded context, separated from the hatchery. That was the most important architectural decision, and it stands. The entities, service structure, and integration contract via `EggTransferOrder` are all sound.

### What the original spec missed — and why it matters

**1. The flock has two operationally distinct phases.**
A parent flock does not start producing eggs when it arrives at the house. Pullets (young hens) are first raised in a rearing phase from day-old chick to approximately week 23–25 of age. Only then are they transferred to the production (laying) house. The two phases are managed differently: different feed programs, different light programs, different health monitoring, different KPIs. Merging them into one flat "active flock" record loses critical management data and creates confusion in daily operations.

**2. Not all eggs go to the hatchery.**
This is the most significant real-world correction. On a commercial breeder farm, eggs collected each day fall into distinct categories with completely different commercial destinations:

- **First-grade hatching eggs** (clean, correct size, intact shell, collected from nests) → sent to the hatchery. This is the primary product.
- **Second-grade / reject eggs** (floor eggs, cracked, oversized, undersized, misshapen, double-yolk, dirty) → these **cannot be incubated** and must be disposed of or sold commercially as food-industry eggs or consumer table eggs.
- **Truly unsaleable eggs** (rotten, leaking, heavily blood-stained, contaminated with feces to the point of health risk) → destroyed / discarded.

In a real integrated company, the reject egg stream is a meaningful secondary revenue source. Companies typically sell reject eggs to:
- Food processing plants (liquid egg, pasteurised egg products, bakeries)
- Local retail markets (if compliant with food safety grading for Class B eggs under EU Regulation 589/2008)
- Animal feed processors (broken, contaminated eggs)

The original spec's `EggCollectionRecord` tracks `crackedCount` and `floorEggCount` as numbers, but then routes ALL eggs through a single quality inspection and then into an `EggTransferOrder` to the hatchery. That is wrong. The crack count and floor egg count belong to a separate stream that never reaches the hatchery.

**3. The egg quality inspection is pre-dispatch, but sorting happens at collection.**
In practice, eggs are sorted at the farm at collection time: first-grade goes in the hatching-egg tray, second-grade / rejects go in a separate collection. The formal quality inspection that gates the `EggTransferOrder` is a confirmation step on the already-sorted first-grade stock, not the sorting step itself.

---

## 2. Flock Lifecycle — The Two Phases

A parent flock moves through two distinct phases across its life. The system must model both.

```
Day-old pullet chick arrives (from grandparent hatchery or purchased stock)
          |
          ▼
  ┌──────────────────────────────────┐
  │   REARING PHASE                  │
  │   ~0 to 23 weeks                 │
  │   Rearing house                  │
  │   No eggs produced               │
  │   KPIs: bodyweight, uniformity,  │
  │         feed/bird/day, mortality │
  └──────────────────────────────────┘
          |
          | Transfer to production house
          | (photo-stimulation triggers lay onset)
          ▼
  ┌──────────────────────────────────┐
  │   PRODUCTION PHASE               │
  │   ~23 to 60+ weeks               │
  │   Laying house                   │
  │   Eggs produced daily            │
  │   KPIs: laying rate, egg weight, │
  │         fertility, hatchability  │
  └──────────────────────────────────┘
          |
          | Flock reaches end of productive life
          | (low laying rate, age threshold)
          ▼
     FLOCK CULLED / DEPOT LIQUIDATION
```

The two phases are typically in **separate physical houses** (a rearing house and a production house). The transfer between them is a formal event that the system must record.

---

## 3. Egg Classification — The Heart of the Matter

Every egg collected on the farm is classified at the point of collection into one of these categories. This classification determines the egg's commercial destiny and must be tracked in the system.

| Class | Description | Hatchable? | Commercial Destination |
|---|---|---|---|
| **Grade 1 / Hatching Egg** | Clean, correct weight (50–68g), intact shell, oval shape, collected from nest box | Yes | Hatchery (internal transfer) |
| **Grade 2 / Reject — Sellable** | Floor egg, lightly dirty, cracked-but-intact membrane, oversized, undersized, misshapen, double-yolk | No | External sale — food industry (egg products, bakeries) or consumer market (Class B) |
| **Reject — Unsaleable** | Leaking, heavily contaminated, rotten, blood spots, broken shell exposing contents | No | Destroyed / rendered |

The **Grade 2 / Reject — Sellable** stream is what the original spec did not model. These eggs have real commercial value and generating a sales record and revenue tracking for them is a legitimate business requirement.

### Typical percentages in a healthy flock (for context, not hard-coded values)

- Grade 1 hatching eggs: 85–92% of total collected
- Grade 2 sellable rejects: 5–12% of total collected (floor eggs 3–5%, other rejects 2–7%)
- Unsaleable / destroyed: 1–3%

These percentages shift significantly with flock age and season, which is why the system must track them individually.

---

## 4. Egg Disposition — Where the Eggs Go

The following diagram shows the complete egg flow this service must support:

```
Daily Egg Collection per House
            │
            ▼
    ┌──────────────────────────────────────────┐
    │         Egg Classification at Farm       │
    │  (done at collection, recorded per sheet)│
    └──────────────────────────────────────────┘
            │
     ┌──────┴────────────────────┐
     │                           │
     ▼                           ▼
Grade 1 — Hatching Eggs    Grade 2 + Unsaleable
     │                           │
     ▼                           ├──→ Grade 2 (sellable) → EggSaleOrder
Egg Storage Unit                 │      → External buyer (food industry / market)
(awaiting dispatch)              │      → Delivery note
     │                           │
     ▼                           └──→ Destroyed → Destruction Record
Pre-dispatch Quality Inspection
(crack rate, age, contamination check
 on the Grade 1 stock only)
     │
     ├──→ PASSED → EggTransferOrder
     │       → Hatchery (internal)
     │       → Dispatch note with flock traceability
     │
     └──→ REJECTED → Grade 1 batch downgraded to Grade 2
              → Goes to EggSaleOrder or destruction
```

---

## 5. Bounded Context Boundary

| This service OWNS | This service does NOT own |
|---|---|
| Parent flock registration and lifecycle (rearing + production phases) | Incubation and hatching results |
| Breeder house management | Anything after the egg leaves the farm gate |
| Daily operations: mortality, feed, water, egg collection and classification | Chick production data |
| Egg storage management | Broiler farm or slaughterhouse data |
| Egg quality inspection (pre-dispatch) | Commercial buyer ERP or invoicing |
| Internal egg transfer to hatchery (`EggTransferOrder`) | |
| External egg sale orders (`EggSaleOrder`) | |
| Flock inventory (feed, medication, vaccines) | |
| Parent flock KPI reporting | |

**Cross-context references:** Only by stored ID. No JPA `@ManyToOne` crosses schema boundaries.

---

## 6. Actors & Roles

| Role | Authority |
|---|---|
| `FLOCK_SUPERVISOR` | Full CRUD on all parent flock records. Creates and approves egg transfer orders and egg sale orders. Initiates flock culling. |
| `FLOCK_WORKER` | Records daily operations sheets (mortality, feed, water, egg collection) for assigned houses. Read-only otherwise. |
| `FLOCK_VET` | Creates health records, vaccination events, biosecurity inspections, disease events. Read-only on production data. |
| `MANAGER` | Read access across all records. Cannot create or modify. |
| `SYSTEM_ADMIN` | User management only. No operational write access. |

---

## 7. Functional Requirements (Revised & Extended)

Priority: **M** = Must-have (MVP), **S** = Should-have, **C** = Could-have.

---

### 7.1 PF1 — Breeder Flock Management (with Rearing Phase)

This feature is significantly expanded from v2 to cover the two-phase lifecycle.

#### PF1-A — House Management

| ID | Requirement | Priority |
|---|---|---|
| PF1-A-FR-01 | The system shall register breeder houses with: house code, type (`REARING` / `PRODUCTION`), capacity (hen count), dimensions (length m, width m), ventilation type, and current status. | M |
| PF1-A-FR-02 | House status transitions: `EMPTY` → `OCCUPIED` → `CLEANING` → `EMPTY` (and `MAINTENANCE` from any state). The system shall enforce valid transitions and prevent assigning a flock to a house not in `EMPTY` status. | M |
| PF1-A-FR-03 | The system shall prevent placing a REARING-type flock in a PRODUCTION-type house and vice versa. | M |

#### PF1-B — Flock Registration

| ID | Requirement | Priority |
|---|---|---|
| PF1-B-FR-01 | The system shall register a new parent flock at the point of pullet placement with: flock code, breed / genetic line, entry date, initial bird count (hens + cocks separately), assigned rearing house, source (purchased day-old chicks or own grandparent stock, with supplier/batch reference), and health status. | M |
| PF1-B-FR-02 | The system shall track hen count and cock count separately throughout the lifecycle, updating on every recorded mortality or culling event. | M |
| PF1-B-FR-03 | The system shall calculate and display flock age in weeks automatically from entry date. | M |
| PF1-B-FR-04 | A flock created in `REARING` phase must have `phaseStatus = REARING`. It cannot have egg collection records while in rearing phase. | M |

#### PF1-C — Rearing Phase Management

| ID | Requirement | Priority |
|---|---|---|
| PF1-C-FR-01 | The system shall support recording of weekly bodyweight samples during the rearing phase: sample size (number of birds weighed), total sample weight, calculated average weight, and target weight for that week of age. | M |
| PF1-C-FR-02 | The system shall compare actual average bodyweight against the breed-standard target weight for each week and generate a WARNING alert when the deviation exceeds a configured tolerance (default ±5%). | S |
| PF1-C-FR-03 | The system shall record flock uniformity (coefficient of variation %) from weight sample data. | S |
| PF1-C-FR-04 | The system shall alert the flock supervisor at the configured transfer-readiness week (default week 22) to assess the flock for transfer to production. | S |

#### PF1-D — Transfer from Rearing to Production

| ID | Requirement | Priority |
|---|---|---|
| PF1-D-FR-01 | The system shall allow the flock supervisor to record a rearing-to-production transfer event specifying: transfer date, source rearing house, destination production house, transferred hen count, transferred cock count, and actual body weight at transfer. | M |
| PF1-D-FR-02 | On transfer confirmation: source rearing house status changes to `CLEANING`; destination production house status changes to `OCCUPIED`; flock `phaseStatus` changes to `PRODUCTION`. | M |
| PF1-D-FR-03 | The system shall prevent recording egg collection for a flock whose `phaseStatus` is not `PRODUCTION`. | M |

#### PF1-E — End of Flock Lifecycle

| ID | Requirement | Priority |
|---|---|---|
| PF1-E-FR-01 | The system shall generate an alert when a flock's laying rate falls below the configured minimum laying rate for 3 consecutive weeks (configurable count), indicating the flock is approaching productive end-of-life. | S |
| PF1-E-FR-02 | The system shall generate an alert when a flock reaches the configured maximum productive age (default week 60). | M |
| PF1-E-FR-03 | The system shall allow the flock supervisor to record a culling / liquidation event with: culling date, reason (AGE / LOW_PERFORMANCE / DISEASE / ECONOMIC), bird count culled, and disposal method. | M |
| PF1-E-FR-04 | On flock closure, the production house status shall change to `CLEANING`. | M |

---

### 7.2 PF2 — Breeder Daily Operations

One operations sheet per house per calendar date. Only valid for production-phase flocks.

| ID | Requirement | Priority |
|---|---|---|
| PF2-FR-01 | The system shall allow one daily operations sheet per production house per calendar date. Duplicate submissions must be rejected with an appropriate error. | M |
| PF2-FR-02 | The system shall record daily mortality with: hen count, cock count, and cause (DISEASE / INJURY / SUFFOCATION / CULLED / UNKNOWN). The running live hen count and cock count on the flock are updated automatically. | M |
| PF2-FR-03 | The system shall record daily feed consumption by feed type and quantity (kg), linked to a specific inventory stock batch for FIFO traceability. | M |
| PF2-FR-04 | The system shall record daily water consumption (litres) per house and calculate per-bird daily water consumption. | S |
| PF2-FR-05 | The system shall record egg collection with the following breakdown per collection: `nestEggs` (Grade 1 — hatching), `floorEggs` (Grade 2 reject), `crackedEggs` (Grade 2 reject — cracked-but-intact membrane), `dirtyEggs` (Grade 2 reject — lightly soiled), `rejectedDestroyed` (unsaleable — leaking, rotten, contaminated). | M |
| PF2-FR-06 | The system shall calculate and display: `totalCollected` = sum of all categories; `grade1Count` = `nestEggs` (after deducting eggs failing size/shape check); `grade2SellableCount` = floor + cracked + dirty; `destroyedCount` = rejected destroyed; `dailyLayingRate` = (`grade1Count` + `grade2SellableCount`) / active hen count × 100. | M |
| PF2-FR-07 | The system shall flag the daily operations sheet if laying rate falls below the configured minimum (default 75%) and generate an INFO alert. | M |
| PF2-FR-08 | Daily sheets shall have status `DRAFT` (editable by worker) and `SUBMITTED` (locked after submission by supervisor). Only FLOCK_SUPERVISOR can unlock a submitted sheet for correction, with a mandatory reason logged. | M |

---

### 7.3 PF3 — Health & Veterinary Management

| ID | Requirement | Priority |
|---|---|---|
| PF3-FR-01 | Only a user with `FLOCK_VET` role shall create, update, or delete health, vaccination, and biosecurity records. | M |
| PF3-FR-02 | The system shall record vaccination events per flock: vaccine name, stock batch reference, dose per bird (hens and cocks may differ), route of administration, vaccination date, and administering vet. | M |
| PF3-FR-03 | The system shall maintain a vaccination schedule per flock (driven by breed-standard protocol or manual entry) and alert the vet when a scheduled vaccination is due within 7 days. | S |
| PF3-FR-04 | The system shall record disease events: diagnosis, affected bird count, estimated mortality impact, and treatment prescribed. | M |
| PF3-FR-05 | The system shall record medication treatments: medication name, stock batch, dose, route, start date, duration in days, prescribing vet. The system shall calculate `treatmentEndDate` and `withdrawalEndDate` (treatment end + withdrawal days configured on the medication). | M |
| PF3-FR-06 | The system shall hard-block creation of an `EggTransferOrder` for any flock where the egg withdrawal period is active (i.e., `withdrawalEndDate` > today). This block cannot be overridden by any role. Eggs laid during withdrawal must be classified as destroyed or downgraded to Grade 2 sold externally to non-food channels. | M |
| PF3-FR-07 | The system shall record biosecurity inspections per house with: inspection date, inspector identity, findings, and required corrective actions. | S |
| PF3-FR-08 | The system shall record flock serological and bacteriological monitoring results (test type, sample date, result) for audit trail purposes. | C |

---

### 7.4 PF4 — Egg Collection & Classification

This feature bridges daily collection records into the egg stock ready for disposition.

| ID | Requirement | Priority |
|---|---|---|
| PF4-FR-01 | The system shall maintain a running Grade 1 egg stock balance per flock from daily collection records, incrementing on each submitted daily sheet and decrementing when eggs are transferred to the hatchery or rejected at quality inspection. | M |
| PF4-FR-02 | The system shall maintain a running Grade 2 (sellable) egg stock balance per flock, incrementing from daily sheet Grade 2 counts and decrementing when eggs are dispatched via an EggSaleOrder. | M |
| PF4-FR-03 | The system shall aggregate daily egg collection into weekly production summaries per flock: total Grade 1, total Grade 2 by sub-category (floor/cracked/dirty), total destroyed, average daily laying rate, average egg weight if recorded. | M |
| PF4-FR-04 | The system shall record a pre-dispatch quality inspection on the accumulated Grade 1 stock before an `EggTransferOrder` is created. The inspection shall record: sample size, crack rate %, contamination rate %, size distribution, average egg age at inspection (days from lay date). | M |
| PF4-FR-05 | The system shall reject an `EggTransferOrder` if: crack rate exceeds configured maximum (default 3%), or average egg age at inspection exceeds 7 days, or contamination rate exceeds configured maximum (default 1%). On rejection, the batch is flagged for downgrade to Grade 2 or destruction. | M |
| PF4-FR-06 | The system shall track egg age automatically. Grade 1 eggs older than the maximum hold age (default 7 days) shall be automatically flagged as overdue and an alert generated, preventing them from being included in an `EggTransferOrder`. | M |

---

### 7.5 PF5 — Egg Storage Management

Eggs spend a period in on-farm cold storage between collection and dispatch. This is not the hatchery storage — it is the farm-side egg room.

| ID | Requirement | Priority |
|---|---|---|
| PF5-FR-01 | The system shall register egg storage units (rooms or cabinets) with: unit code, capacity (dozens or trays), target temperature range (°C), and target humidity range (%). | M |
| PF5-FR-02 | The system shall record periodic temperature and humidity readings per storage unit, automatically flagging readings outside the configured safe ranges and generating WARNING alerts. | M |
| PF5-FR-03 | The system shall track which egg collections are stored in which storage unit, allowing traceability from the dispatch order back to the specific storage unit and its condition logs. | S |
| PF5-FR-04 | The system shall alert the supervisor if a Grade 1 egg batch has been in storage for more than 5 days without an active `EggTransferOrder`. | S |

---

### 7.6 PF6 — Egg Disposition: Transfer to Hatchery

This is the primary commercial output of the parent flock operation.

| ID | Requirement | Priority |
|---|---|---|
| PF6-FR-01 | The system shall allow the flock supervisor to create an `EggTransferOrder` specifying: destination hatchery ID, source flock, planned quantity (Grade 1 only), scheduled dispatch date, and linked quality inspection record. | M |
| PF6-FR-02 | The system shall enforce pre-conditions before allowing creation of an `EggTransferOrder`: the linked quality inspection must have outcome PASSED; no active egg withdrawal period on the flock; planned quantity must not exceed available Grade 1 stock. | M |
| PF6-FR-03 | The system shall track `EggTransferOrder` status through the lifecycle: `PLANNED` → `APPROVED` → `DISPATCHED` → `RECEIVED` → `CANCELLED`. Only the flock supervisor can approve. | M |
| PF6-FR-04 | On `DISPATCHED` status: the system shall deduct `actualDispatchedQuantity` from the Grade 1 egg stock balance. The dispatched quantity may differ from planned (e.g., some eggs failed final visual check at loading). | M |
| PF6-FR-05 | The system shall notify the destination hatchery (via notification entity) at least 24 hours before the scheduled dispatch date. | S |
| PF6-FR-06 | The system shall generate a printable `EggDeliveryNote` for each dispatched order, containing: transfer code, source flock code, flock age at dispatch, house number, quality inspection summary, egg quantity, dispatch date, and dispatcher identity. This document enables full traceability at the hatchery gate. | S |
| PF6-FR-07 | When the hatchery service marks the order as `RECEIVED`, the parent flock service updates the `EggTransferOrder` status and records the hatchery-reported received quantity for reconciliation. | M |

---

### 7.7 PF7 — Egg Disposition: External Commercial Sale (NEW)

This feature did not exist in the original v2 spec and is required to model reality.

| ID | Requirement | Priority |
|---|---|---|
| PF7-FR-01 | The system shall maintain a register of external egg buyers (`EggBuyer`) with: name, buyer type (`FOOD_PROCESSOR` / `RETAIL_MARKET` / `ANIMAL_FEED` / `OTHER`), contact details, and applicable food safety certification (if selling to food industry). | M |
| PF7-FR-02 | The system shall allow the flock supervisor to create an `EggSaleOrder` for Grade 2 (sellable) eggs, specifying: buyer, egg category breakdown (floor / cracked / other), quantity, unit price, and scheduled dispatch date. | M |
| PF7-FR-03 | The `EggSaleOrder` shall have a lifecycle status: `PLANNED` → `APPROVED` → `DISPATCHED` → `DELIVERED` → `CANCELLED`. | M |
| PF7-FR-04 | The system shall enforce that eggs included in an `EggSaleOrder` are drawn only from the Grade 2 sellable stock balance — not from Grade 1 hatching egg stock. | M |
| PF7-FR-05 | On `DISPATCHED` status, the system shall deduct the dispatched quantity from the Grade 2 egg stock balance. | M |
| PF7-FR-06 | The system shall generate a sale delivery note per dispatched `EggSaleOrder` with: sale code, buyer reference, egg categories and quantities, dispatch date, and vehicle / driver reference. | S |
| PF7-FR-07 | The system shall record the sale price per dozen (or per tray) on the `EggSaleOrder`. This is informational; invoicing is handled by an external accounting system. | M |
| PF7-FR-08 | The system shall record destruction events for unsaleable eggs: date, quantity, destruction method (incineration / composting / rendering), and supervisor authorising the destruction. | M |
| PF7-FR-09 | The system shall report the Grade 2 egg revenue contribution weekly alongside Grade 1 transfer volume, so management can see the full picture of egg output value. | S |

---

### 7.8 PF8 — Inventory Management

| ID | Requirement | Priority |
|---|---|---|
| PF8-FR-01 | The system shall maintain stock of consumables: breeder feed (by type: starter / grower / pre-layer / layer / cock feed), vaccines, medications, litter material, disinfectants, and cleaning agents. | M |
| PF8-FR-02 | The system shall record stock receipts with: supplier name, delivery note number, batch/lot number, expiry date, quantity received, unit of measure, and receiving supervisor. | M |
| PF8-FR-03 | The system shall record an incoming quality inspection result (PASS / FAIL with notes) on each stock batch. A FAILED batch shall be quarantined and excluded from automatic consumption. | S |
| PF8-FR-04 | The system shall consume feed and medication stocks on a FIFO basis (earliest expiry date first) and enforce this at the service layer whenever a consumption is recorded. | M |
| PF8-FR-05 | The system shall alert the supervisor when any consumable falls below its configured reorder point. | M |
| PF8-FR-06 | The system shall alert when a stock batch approaches its expiry date (default: alert 30 days before expiry). | S |
| PF8-FR-07 | Feed stock is linked to daily consumption records in `PF2`. Medication stock is linked to treatment records in `PF3`. Both provide full batch-level traceability. | M |

---

### 7.9 PF9 — Performance Reporting & KPIs

| ID | Requirement | Priority |
|---|---|---|
| PF9-FR-01 | The system shall generate a weekly production summary per flock covering: Grade 1 eggs produced, Grade 2 eggs produced by sub-category, eggs destroyed, total laying rate, total feed consumed (kg and kg/100 hens/day), total mortality (count and cumulative %), and egg transfer volume dispatched. | M |
| PF9-FR-02 | The system shall calculate and display Hen-Day Production % (eggs collected / live hens today × 100) and Hen-Housed Production % (eggs collected / initial hen count × 100) for each week. | M |
| PF9-FR-03 | The system shall provide a MANAGER-level dashboard showing: active production flocks, current laying rate per flock, Grade 1 stock available for dispatch, and any open CRITICAL alerts. | S |
| PF9-FR-04 | All list and summary data shall be exportable as CSV for veterinary authority audits, per NFR-C-04. | M |

---

## 8. Entity Catalogue (Revised)

### 8.1 PF1 — Flock & House

| Entity | Role | Key Fields |
|---|---|---|
| `ParentFlockHouse` | Physical house. Type constrains what phase of flock can occupy it. | `id`, `houseCode`, `houseType` (REARING / PRODUCTION), `capacity` (hens), `lengthM`, `widthM`, `ventilationType`, `status` (EMPTY / OCCUPIED / CLEANING / MAINTENANCE) |
| `ParentFlock` | Root aggregate. Tracks both rearing and production phase data. | `id`, `flockCode`, `geneticLine`, `breed`, `entryDate`, `phaseStatus` (REARING / PRODUCTION / CULLED), `currentHenCount`, `currentCockCount`, `initialHenCount`, `initialCockCount`, `rearingHouseId`, `productionHouseId` (set at phase transfer), `healthStatus` (HEALTHY / QUARANTINE / TREATMENT / CULLED), `sourceType` (OWN_STOCK / PURCHASED), `supplierRef` |
| `FlockBirdCountEvent` | Records every change in bird count (mortality or culling). Updates `currentHenCount` / `currentCockCount` on `ParentFlock`. | `id`, `flockId`, `eventDate`, `birdType` (HEN / COCK), `changeCount` (negative = mortality/cull), `cause` (DISEASE / INJURY / SUFFOCATION / CULLED / UNKNOWN), `recordedBy` |
| `RearingWeightSample` | Weekly weight sample during rearing phase. | `id`, `flockId`, `weekOfAge`, `sampleDate`, `sampleSize`, `totalWeightG`, `averageWeightG`, `targetWeightG`, `uniformityPercent`, `recordedBy` |
| `RearingToProductionTransfer` | Records the formal phase change event. | `id`, `flockId`, `transferDate`, `rearingHouseId`, `productionHouseId`, `transferredHenCount`, `transferredCockCount`, `bodyWeightAtTransferG`, `authorisedBy` |
| `FlockCullingRecord` | End-of-lifecycle culling event. | `id`, `flockId`, `cullingDate`, `reason` (AGE / LOW_PERFORMANCE / DISEASE / ECONOMIC), `birdsCulled`, `disposalMethod`, `authorisedBy` |

### 8.2 PF2 — Daily Operations

| Entity | Role | Key Fields |
|---|---|---|
| `BreederDailySheet` | One-per-house-per-date aggregate. Enforces uniqueness per `(productionHouseId, operationDate)`. | `id`, `flockId`, `productionHouseId`, `operationDate`, `submittedBy`, `submittedAt`, `status` (DRAFT / SUBMITTED), `unlockedBy`, `unlockReason` |
| `BreederMortalityRecord` | Daily mortality. Child of `BreederDailySheet`. | `id`, `dailySheetId`, `henMortalityCount`, `cockMortalityCount`, `cause`, `causeNotes` |
| `BreederFeedRecord` | Daily feed consumption per feed type. Child of `BreederDailySheet`. | `id`, `dailySheetId`, `feedStockBatchId`, `feedType` (STARTER / GROWER / PRE_LAYER / LAYER / COCK_FEED), `quantityKg` |
| `BreederWaterRecord` | Daily water consumption. Child of `BreederDailySheet`. | `id`, `dailySheetId`, `quantityLitres`, `perBirdLitres` (calculated) |
| `EggCollectionRecord` | Daily egg collection with full grade breakdown. Child of `BreederDailySheet`. | `id`, `dailySheetId`, `nestEggCount`, `floorEggCount`, `crackedEggCount`, `dirtyEggCount`, `destroyedCount`, `grade1Count` (calculated: nest eggs meeting size/shape), `grade2SellableCount` (calculated: floor + cracked + dirty), `dailyLayingRatePercent` (calculated), `isBelowLayingRateThreshold` |

### 8.3 PF3 — Health

| Entity | Role | Key Fields |
|---|---|---|
| `BreederVaccinationRecord` | Vaccination event. Only `FLOCK_VET` creates. | `id`, `flockId`, `vaccineStockBatchId`, `vaccineName`, `batchNumber`, `dosePerHen`, `dosePerCock`, `route`, `vaccinationDate`, `administeredBy` |
| `VaccinationScheduleEntry` | Planned vaccination event for a flock. | `id`, `flockId`, `vaccineName`, `scheduledWeekOfAge`, `scheduledDate`, `status` (PENDING / COMPLETED / OVERDUE) |
| `BreederTreatmentRecord` | Medication treatment on a flock. | `id`, `flockId`, `medicationId`, `medicationStockBatchId`, `prescribingVetUserId`, `startDate`, `durationDays`, `endDate` (calculated), `withdrawalDays`, `withdrawalEndDate` (calculated), `dosePerBird`, `route`, `totalMedicationUsed` |
| `BreederHealthEvent` | Disease event or health alert. | `id`, `flockId`, `eventDate`, `diagnosis`, `affectedHenCount`, `affectedCockCount`, `treatmentPrescribed`, `recordedBy` |
| `BiosecurityInspection` | Biosecurity check per house. | `id`, `productionHouseId`, `inspectionDate`, `findings`, `correctiveActions`, `inspectedBy` |

### 8.4 PF4 — Egg Classification Ledger

| Entity | Role | Key Fields |
|---|---|---|
| `EggStockLedger` | Running balance of Grade 1 and Grade 2 eggs per flock. One record per flock. Maintained by service layer on every collection or dispatch event. | `id`, `flockId`, `grade1Available`, `grade2Available`, `lastUpdatedAt` |
| `EggStockLedgerEntry` | Each credit or debit on the egg stock. Provides full audit trail. | `id`, `flockId`, `entryDate`, `grade` (GRADE_1 / GRADE_2), `direction` (CREDIT / DEBIT), `quantity`, `reason` (DAILY_COLLECTION / TRANSFER_DISPATCHED / SALE_DISPATCHED / QUALITY_DOWNGRADE / DESTRUCTION / CORRECTION), `relatedEntityType`, `relatedEntityId` |
| `WeeklyEggProductionSummary` | Computed weekly summary per flock. | `id`, `flockId`, `weekNumber`, `year`, `totalGrade1`, `totalFloorEggs`, `totalCrackedEggs`, `totalDirtyEggs`, `totalDestroyed`, `totalLiveHens` (average for week), `henDayProductionPercent`, `henHousedProductionPercent`, `generatedAt` |

### 8.5 PF4-B — Pre-dispatch Quality Inspection

| Entity | Role | Key Fields |
|---|---|---|
| `EggQualityInspection` | Pre-dispatch inspection on Grade 1 stock before creating an `EggTransferOrder`. | `id`, `flockId`, `inspectionDate`, `periodStart`, `periodEnd`, `sampleSize`, `crackRatePercent`, `contaminationRatePercent`, `sizeDistribution` (JSON: undersized / ideal / oversized counts), `averageAgeAtInspectionDays`, `outcome` (PASSED / REJECTED), `rejectionReason`, `inspectedBy` |

### 8.6 PF5 — Egg Storage

| Entity | Role | Key Fields |
|---|---|---|
| `EggStorageUnit` | Farm-side egg room / cabinet. | `id`, `unitCode`, `capacityDozens`, `targetMinTempC`, `targetMaxTempC`, `targetMinHumidityPercent`, `targetMaxHumidityPercent`, `status` (OPERATIONAL / DEFROST / MAINTENANCE) |
| `EggStorageConditionLog` | Periodic temperature and humidity readings. | `id`, `storageUnitId`, `recordedAt`, `temperatureCelsius`, `humidityPercent`, `isOutOfRange`, `recordedBy` |

### 8.7 PF6 — Egg Transfer to Hatchery

| Entity | Role | Key Fields |
|---|---|---|
| `EggTransferOrder` | Integration entity. Produced by this service, consumed by Hatchery Service. | `id`, `transferCode`, `sourceFlockId`, `eggQualityInspectionId`, `destinationHatcheryId`, `plannedQuantity`, `scheduledDispatchDate`, `status` (PLANNED / APPROVED / DISPATCHED / RECEIVED / CANCELLED), `approvedBy`, `approvedAt`, `dispatchedAt`, `actualDispatchedQuantity`, `hatcheryReceivedQuantity` (set by hatchery on receipt) |
| `EggDeliveryNote` | Printable traceability document. Generated on DISPATCHED status. | `id`, `transferOrderId`, `generatedAt`, `documentReference`, `flockCode`, `flockAgeWeeks`, `houseNumber`, `qualityInspectionSummaryJson`, `quantity`, `generatedBy` |

### 8.8 PF7 — External Egg Sale (NEW)

| Entity | Role | Key Fields |
|---|---|---|
| `EggBuyer` | External buyer of Grade 2 eggs. | `id`, `name`, `buyerType` (FOOD_PROCESSOR / RETAIL_MARKET / ANIMAL_FEED / OTHER), `contactPerson`, `phone`, `email`, `address`, `foodSafetyCertRef` (nullable) |
| `EggSaleOrder` | Commercial sale of Grade 2 eggs to external buyers. | `id`, `saleCode`, `sourceFlockId`, `buyerId`, `eggCategories` (JSON: breakdown of floor/cracked/dirty counts and quantities), `totalQuantity`, `unitPricePerDozen`, `scheduledDispatchDate`, `status` (PLANNED / APPROVED / DISPATCHED / DELIVERED / CANCELLED), `approvedBy`, `dispatchedAt`, `actualDispatchedQuantity` |
| `EggSaleDeliveryNote` | Sale delivery document. | `id`, `saleOrderId`, `generatedAt`, `documentReference`, `vehicleRegistration`, `driverName`, `generatedBy` |
| `EggDestructionRecord` | Records destruction of unsaleable eggs. | `id`, `flockId`, `destructionDate`, `quantity`, `eggCategory` (LEAKING / ROTTEN / CONTAMINATED / OTHER), `destructionMethod` (INCINERATION / COMPOSTING / RENDERING), `authorisedBy` |

### 8.9 PF8 — Inventory

| Entity | Role | Key Fields |
|---|---|---|
| `FlockConsumableType` | Consumable catalogue per farm operation. | `id`, `name`, `category` (FEED / VACCINE / MEDICATION / LITTER / DISINFECTANT / CLEANING), `unit`, `reorderPoint`, `feedSubtype` (nullable — STARTER / GROWER / PRE_LAYER / LAYER / COCK_FEED) |
| `FlockStockBatch` | Received delivery with full batch traceability. | `id`, `consumableTypeId`, `supplierName`, `deliveryNoteNumber`, `receiptDate`, `batchNumber`, `expiryDate`, `quantityReceived`, `quantityRemaining`, `qualityInspectionStatus` (PASS / FAIL / PENDING), `qualityNotes` |
| `FlockStockTransaction` | Each stock debit (FIFO). | `id`, `stockBatchId`, `transactionDate`, `quantityUsed`, `relatedEntityType` (DAILY_SHEET / TREATMENT / VACCINATION), `relatedEntityId`, `performedBy` |

---

## 9. Business Rules Summary

These are the invariants that must be enforced at the **domain service layer**, not only at the API controller layer.

| # | Rule | Enforcement Level |
|---|---|---|
| BR-01 | A house not in `EMPTY` status cannot be assigned a new flock. | Domain service |
| BR-02 | Egg collection cannot be recorded for a flock in `REARING` phase. | Domain service |
| BR-03 | `EggTransferOrder` cannot be created if flock has an active egg withdrawal period. | Domain service (hard block, no override) |
| BR-04 | `EggTransferOrder` cannot be created if the quality inspection outcome is not PASSED. | Domain service |
| BR-05 | `EggTransferOrder` planned quantity cannot exceed available Grade 1 stock balance. | Domain service |
| BR-06 | `EggSaleOrder` quantity cannot exceed available Grade 2 sellable stock balance. | Domain service |
| BR-07 | Only `FLOCK_VET` can create health, vaccination, and treatment records. | Spring Security `@PreAuthorize` + domain service |
| BR-08 | Duplicate daily sheets (same house + same date) must be rejected. | Database unique constraint + domain service |
| BR-09 | Feed and medication stock consumed on FIFO basis (earliest expiry first). | Domain service on every consumption write |
| BR-10 | Rearing-type flock cannot occupy a Production-type house and vice versa. | Domain service |
| BR-11 | Grade 1 eggs older than configured maximum age cannot be included in an `EggTransferOrder`. | Domain service |
| BR-12 | Audit log records are append-only. No update or delete on `AuditLog`. | Database constraint + no delete endpoint |

---

## 10. API Endpoints Overview

This is an indicative (non-exhaustive) list of REST endpoints. Full contracts are generated via Springdoc OpenAPI.

**Service base URL:** `http://parent-flock-service:8080/api/v1`

### Flock & House

```
GET    /houses                             List all houses (paginated, filterable by status/type)
POST   /houses                             Register new house (FLOCK_SUPERVISOR)
PATCH  /houses/{id}/status                 Change house status (FLOCK_SUPERVISOR)

GET    /flocks                             List flocks (paginated, filterable by phase/status)
POST   /flocks                             Register new flock (FLOCK_SUPERVISOR)
GET    /flocks/{id}                        Get flock detail including current counts
POST   /flocks/{id}/phase-transfer         Record rearing→production transfer (FLOCK_SUPERVISOR)
POST   /flocks/{id}/culling                Record flock culling (FLOCK_SUPERVISOR)
POST   /flocks/{id}/rearing-weight-samples Record rearing weight sample (FLOCK_WORKER / FLOCK_SUPERVISOR)
```

### Daily Operations

```
GET    /daily-sheets                       List sheets (filterable by flock/date)
POST   /daily-sheets                       Create new daily sheet (FLOCK_WORKER)
GET    /daily-sheets/{id}                  Get sheet with all child records
PATCH  /daily-sheets/{id}/submit           Submit sheet (FLOCK_SUPERVISOR)
PATCH  /daily-sheets/{id}/unlock           Unlock submitted sheet for correction (FLOCK_SUPERVISOR)
```

### Health

```
GET    /flocks/{flockId}/vaccinations      List vaccinations
POST   /flocks/{flockId}/vaccinations      Record vaccination (FLOCK_VET)
GET    /flocks/{flockId}/treatments        List treatments
POST   /flocks/{flockId}/treatments        Record treatment (FLOCK_VET)
GET    /flocks/{flockId}/health-events     List health events
POST   /flocks/{flockId}/health-events     Record health event (FLOCK_VET)
GET    /houses/{houseId}/biosecurity       List biosecurity inspections
POST   /houses/{houseId}/biosecurity       Record biosecurity inspection (FLOCK_VET)
```

### Egg Classification & Stock

```
GET    /flocks/{flockId}/egg-stock         Get current Grade 1 and Grade 2 stock balances
GET    /flocks/{flockId}/egg-stock/ledger  Full ledger of all credits and debits (paginated)
GET    /flocks/{flockId}/weekly-summaries  Weekly production summaries
```

### Quality Inspection

```
GET    /flocks/{flockId}/quality-inspections       List inspections
POST   /flocks/{flockId}/quality-inspections       Create pre-dispatch inspection (FLOCK_SUPERVISOR)
GET    /quality-inspections/{id}                   Get inspection detail
```

### Egg Storage

```
GET    /storage-units                      List storage units
POST   /storage-units                      Register unit (FLOCK_SUPERVISOR)
POST   /storage-units/{id}/condition-logs  Record temperature/humidity reading (FLOCK_WORKER)
GET    /storage-units/{id}/condition-logs  List readings (filterable by date range)
```

### Egg Transfer to Hatchery

```
GET    /egg-transfer-orders                List orders (filterable by status)
POST   /egg-transfer-orders                Create order (FLOCK_SUPERVISOR)
PATCH  /egg-transfer-orders/{id}/approve   Approve order (FLOCK_SUPERVISOR)
PATCH  /egg-transfer-orders/{id}/dispatch  Mark dispatched with actual quantity (FLOCK_SUPERVISOR)
PATCH  /egg-transfer-orders/{id}/receive   Mark received (called by Hatchery service via REST)
PATCH  /egg-transfer-orders/{id}/cancel    Cancel order (FLOCK_SUPERVISOR)
GET    /egg-transfer-orders/{id}/delivery-note  Get/generate delivery note document
```

### Egg Commercial Sale (NEW)

```
GET    /egg-buyers                         List buyers
POST   /egg-buyers                         Register buyer (FLOCK_SUPERVISOR)
GET    /egg-sale-orders                    List sale orders (filterable by status/buyer)
POST   /egg-sale-orders                    Create sale order (FLOCK_SUPERVISOR)
PATCH  /egg-sale-orders/{id}/approve       Approve order (FLOCK_SUPERVISOR)
PATCH  /egg-sale-orders/{id}/dispatch      Mark dispatched (FLOCK_SUPERVISOR)
PATCH  /egg-sale-orders/{id}/deliver       Mark delivered (FLOCK_SUPERVISOR)
GET    /egg-sale-orders/{id}/delivery-note Get sale delivery note
POST   /flocks/{flockId}/destruction-records   Record egg destruction (FLOCK_SUPERVISOR)
```

### Inventory

```
GET    /consumable-types                   List consumable catalogue
POST   /consumable-types                   Add consumable type (FLOCK_SUPERVISOR)
GET    /stock-batches                      List stock batches (filterable by type/expiry)
POST   /stock-batches                      Record new stock receipt (FLOCK_SUPERVISOR)
GET    /stock-transactions                 List transactions (audit trail)
```

---

## 11. What Changes Relative to v2 Requirements

| Area | v2 Original | Corrected in this Spec |
|---|---|---|
| **Flock lifecycle** | Single flat phase from entry to culling | Two distinct phases: REARING and PRODUCTION, with a formal transfer event between them |
| **Rearing phase management** | Not modelled | Weekly weight samples, uniformity tracking, transfer readiness alert — all new |
| **Egg collection record** | `crackedCount`, `floorEggCount`, `netUsableCount` — all lumped together before routing to hatchery | Full classification: `nestEggCount`, `floorEggCount`, `crackedEggCount`, `dirtyEggCount`, `destroyedCount` — each with its own stock ledger stream |
| **Egg stock model** | Single egg stock balance flowing toward hatchery only | Separate Grade 1 ledger (→ hatchery) and Grade 2 ledger (→ external sale), each maintained by an `EggStockLedger` entity |
| **External commercial egg sale** | Not modelled | Fully modelled: `EggBuyer`, `EggSaleOrder`, `EggSaleDeliveryNote`, `EggDestructionRecord` — feature PF7 |
| **Quality inspection scope** | Applied to "net usable" eggs heading to hatchery | Explicitly scoped to Grade 1 stock only; Grade 2 stream bypasses this inspection |
| **Withdrawal period block** | Blocks `EggTransferOrder` only | Also requires that eggs laid during withdrawal be diverted to Grade 2 external-sale or destruction (not just blocked from transfer) |
| **Cock count tracking** | Not mentioned | Tracked separately from hen count throughout lifecycle; vaccination doses differ for hens and cocks |
| **House type** | Single house type | `houseType` (REARING / PRODUCTION) added; service enforces phase-to-house-type consistency |

---

*End of Parent Flock Service Specification.*

*Next steps: PF7 External Sale feature needs confirmation of whether the company currently has external buyers or plans to — this affects whether PF7 is MVP or S priority. The rearing phase (PF1-C and PF1-D) should be confirmed against actual farm operations before implementation begins.*