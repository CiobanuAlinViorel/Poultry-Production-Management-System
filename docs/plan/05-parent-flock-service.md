# 05 — parent-flock-service (fermă de reproducție)

Port 8081 · DB `ppms_parent_flock` · schema `parent_flock` · package `com.ppms.parentflock`
· migrations prefix `VPF`. The spec `docs/specs/parent-flock-architecture.md` is a
useful reference for class shapes; this file supersedes it where they differ.

> This file also defines the **common building blocks reused by every domain service**
> (§A Site registry, §B Local read models, §C Inventory module, §D Costing module,
> §E Documents). Files 06–08 reference these sections instead of repeating them.

---

## A. Common block — Site registry (each domain service owns its sites)
Entities: `<Site>` (BreederSite / Hatchery / Farm / Slaughterhouse): code (unique,
short, used in human codes e.g. `BS1`), name, address, county, gpsLat/Lng, dsvsaRegistrationNo
(veterinary authorisation), managerEmployeeId, active, timezone. Sub-units (`House`,
`Incubator`, `Line`, `Chamber`) with capacity. On create/update publish
`ppms.site.directory` event `<type>.site.registered|updated` (payload: siteId, siteType,
code, name, address, county, active, subUnits[{id,type,code,name}]).
API: `GET/POST /sites`, `GET/PATCH /sites/{id}`, `POST /sites/{id}/deactivate`;
sub-unit CRUD under site. Role: site type's supervisor/manager role for own site;
`SYSTEM_ADMIN` for create.

## B. Common block — Local read models (Kafka-fed tables, never edited by users)
`employee_directory` (from `ppms.admin.employee`), `catalog_*` (medications, vaccines,
feed types, suppliers, breeds, performance standards from `ppms.admin.catalog`),
`site_directory` (other services' sites from `ppms.site.directory`, filtered to the
types this service needs). Each has `last_event_at`. Expose read-only
`GET /lookups/employees?siteId`, `/lookups/medications`, etc. so the UI for this module
gets everything from one origin even if admin-service is down.

## C. Common block — Inventory module (package `inventory` inside each service)
| Entity | Fields |
|---|---|
| `StockItem` | siteId, category (`FEED/MEDICATION/VACCINE/LITTER/CLEANING/DISINFECTANT/FUMIGANT/SPRAY/PACKAGING/CONSUMABLE/EGG_TRAY/CHICK_BOX`), catalogRefId?, code, name, unit (`KG/L/UNIT/DOSE/BAG`), reorderPoint, reorderQty, storageLocation, active, isCritical |
| `StockBatch` | stockItemId, batchNo (supplier lot), supplierId, deliveryNoteNo, receivedAt, expiresAt?, qtyReceived, qtyRemaining, unitCost (RON), qaStatus `PENDING/PASSED/FAILED/QUARANTINE`, qaNotes, documentRef? |
| `StockMovement` | stockBatchId, type `RECEIPT/CONSUMPTION/ADJUSTMENT/TRANSFER_OUT/TRANSFER_IN/WRITE_OFF/RETURN`, qty (+/−), reasonCode, refType/refId (e.g. `DAILY_SHEET`, `TREATMENT`), performedBy, occurredAt, costSnapshot |
| `StockCount` | siteId, countedAt, lines [{itemId, countedQty, systemQty, variance}], approvedBy |
Rules: FEFO consumption (`expiresAt asc nulls last, receivedAt asc`) across PASSED
batches; consumption with insufficient PASSED stock → 422 `STOCK_INSUFFICIENT` (callers
decide: treatment blocks; daily feed sheet **does not block** — it records a negative
"unposted" consumption and raises `INV_NEGATIVE_STOCK` WARNING so paper-vs-system drift
is visible, configurable `inv.allowNegativeFeed=true`); expired batch cannot be consumed
(`STOCK_EXPIRED`); below reorder point → `INV_BELOW_REORDER` (CRITICAL if `isCritical`)
with dedup per item per day; expiring within `inv.expiry.warningDays` → WARNING daily
job; QA FAILED batches blocked, movement `RETURN`/`WRITE_OFF` only.
API: CRUD `/inventory/items`, `POST /inventory/receipts` (creates batch + RECEIPT
movement; optional file upload of delivery note to DocumentStorage), `POST
/inventory/batches/{id}/qa`, `POST /inventory/consume` (manual), `POST
/inventory/adjustments`, `GET /inventory/stock?siteId` (on-hand per item with batches),
`GET /inventory/movements`, `POST /inventory/counts` + `/approve`, exports.
Events `ppms.inventory`: `stock.received`, `stock.consumed`, `stock.below-reorder`,
`stock.expiring`, `stock.written-off` (payload includes cost → costing & reporting).

## D. Common block — Costing module (package `costing`)
`CostCenter` = the production unit being costed (ParentFlock, IncubationBatch, Lot,
SlaughterSession). `CostEntry { costCenterType, costCenterId, siteId, category
(`CHICKS/EGGS/FEED/MEDICATION/VACCINE/LITTER/UTILITIES_ENERGY/UTILITIES_WATER/UTILITIES_GAS/LABOUR/TRANSPORT/CATCHING/PACKAGING/VET_SERVICES/DEPRECIATION/OTHER`),
amountRon, qty?, unit?, sourceType/sourceId (StockMovement, manual, allocation),
periodDate, note }`. Automatic entries from stock consumption (qty × batch unitCost),
transfer receptions (eggs/chicks at transfer price from `pf.transferPrice.*` settings or
the order's `unitPrice`), and manual/periodic entries (utilities, labour hours ×
`hr.hourlyCost` setting, depreciation per house per day). `CostSummary` view per cost
center: total, per category, per unit output (per hatching egg / per saleable chick /
per kg live weight / per kg carcass). API: `GET /costing/{type}/{id}`, `POST
/costing/entries`, `GET /costing/summary?siteId&period`, export. Roles: site manager
(write manual), `ACCOUNTANT`/`MANAGER` read. Events: `cost.entry.recorded` on
`ppms.<service>.costing` (reporting roll-ups).

## E. Common block — Documents
Generated PDFs (delivery notes, FCI, dispatch notes, reports) stored via
`DocumentStorage`; entity `GeneratedDocument { type, refType, refId, number, locale,
documentRef, generatedBy, generatedAt, sha256 }`; endpoint `GET /documents/{id}/download`
(`@AllowTokenQueryParam`). Templates in `resources/templates/pdf/*.html` (RO + EN
strings via message bundles), company header from settings (`company.name`, `company.cui`,
`company.address`, `company.logoUrl`).

---

## 1. Business process (breeder farm, Ross 308 PS reference)
- **Rearing phase** (0–~22 weeks): day-old parent chicks (females + males separately),
  controlled feeding (restricted), weekly body-weight & uniformity, light programme,
  vaccinations heavy (Marek at hatchery, ND/IB/IBD/AE/Reo/Salmonella…).
- **Transfer to production house** at ~20–22 wk; photostimulation; **first eggs ~23–24
  wk** (5 % production = "start of lay"), peak 85–87 % at 30–32 wk, slow decline to
  ~50–55 % at 60–64 wk → **depopulation** (spent hens sold to a slaughterhouse).
- Daily per house: mortality & culls (hens / males), feed kg (phase: breeder rearing,
  pre-lay, breeder 1/2/3), water L, eggs by collection round: total, settable (hatching),
  floor, cracked, dirty, double-yolk, small, misshapen; min/max temperature; egg weight
  sample.
- Weekly: body weight sample (hens & males, uniformity %), feed/hen/day g, eggs per hen
  housed cumulative, hatching-egg %; compare vs breed standard.
- Egg room: eggs sanitised (fumigation/UV), traypacked (30/tray, 12 trays/trolley case),
  stored 15–18 °C / 75–80 % RH, storage log twice a day; egg batches identified by
  flock + collection date; **dispatched within 7 days** ideally; quality check before
  dispatch (crack %, dirty %, weight class).
- Health: vaccination programme (schedule from breed/vet), serology sampling,
  Salmonella official sampling (NCP), disease events & treatments **with egg withdrawal
  period** (treated flock eggs not settable while active — hard block), biosecurity
  inspections (footbaths, visitor log, rodent control).
- Dispatch to hatchery by refrigerated van with aviz + quality summary.

## 2. Roles & permissions
| Role | Can |
|---|---|
| `FLOCK_SUPERVISOR` | everything at own breeder site, approve/dispatch egg transfers, close sheets, manage flocks/houses |
| `FLOCK_WORKER` | create/edit today's & yesterday's daily sheet for assigned houses (duty from admin or explicit house assignment), egg collection rounds, read own site |
| `FLOCK_EGG_ROOM` | egg batches, storage readings, quality inspections, prepare transfer orders (not approve) |
| `FLOCK_VET` | health module writes (vaccination, treatments, disease events, biosecurity, sampling), read all |
| `FARM_STOREKEEPER` (shared role name) | inventory receipts/QA/counts |
| `MANAGER`/`ACCOUNTANT` | read, costing |
Permission keys: `pf:site:*`, `pf:flock:*`, `pf:daily-sheet:*`, `pf:health:*`,
`pf:egg:*`, `pf:egg-transfer:{read,write,approve,dispatch}`, `pf:inventory:*`,
`pf:costing:*`.

## 3. Domain model (schema `parent_flock`)

### 3.1 Structure
| Entity | Fields |
|---|---|
| `BreederSite` | §A |
| `BreederHouse` | siteId, code, name, type `REARING/PRODUCTION`, capacityHens, capacityMales, areaM2, nestCount, status `EMPTY/ACTIVE/CLEANING/MAINTENANCE`, currentFlockId? |
| `EggStore` | siteId, code, capacityEggs, targetTempMin/Max, targetRhMin/Max |
| `ParentFlock` | siteId, flockCode (`BS1-PF-2026-03`), breedId, supplierId (PS chick source), hatchDate (chick age day 0), placementDate, initialFemales, initialMales, currentFemales, currentMales (computed, denormalised), currentHouseId, phase `REARING/PRE_LAY/PRODUCTION/DEPOPULATING/CLOSED`, firstEggDate?, peakLayPct?, peakWeek?, depopulationDate?, status `ACTIVE/CLOSED`, grade C/notes |
| `FlockHouseAssignment` | flockId, houseId, from, to? (history of moves rearing → production) |
| `FlockCountAdjustment` | flockId, date, type `MORTALITY/CULL/TRANSFER_IN/TRANSFER_OUT/SOLD/CORRECTION`, females, males, reason, refType/refId |

### 3.2 Daily operations
| Entity | Fields |
|---|---|
| `BreederDailySheet` | houseId, flockId, date (**unique house+date**), status `DRAFT/SUBMITTED/CLOSED`, recordedBy, dutyEmployeeIds[], submittedAt, closedBy/At, tempMin/Max, rhPct?, lightHours?, notes |
| `DailyMortality` | sheetId, sex `FEMALE/MALE`, count, cause `DISEASE/INJURY/PROLAPSE/CULLED_UNDERWEIGHT/CULLED_DEFORMITY/CANNIBALISM/HEAT_STRESS/UNKNOWN/OTHER`, disposal `RENDERING/INCINERATION/COMPOSTING`, note |
| `DailyFeed` | sheetId, feedTypeCatalogId, stockBatchId?, qtyKg, sex?/mixed |
| `DailyWater` | sheetId, litres, meterReading? |
| `EggCollectionRound` | sheetId, roundNo (1–5), time, total, settable, floor, cracked, dirty, doubleYolk, small, misshapen, soft/thin-shell, collectedBy |
| `EggWeightSample` | sheetId, sampleSize, avgWeightG |
Derived on sheet: `layingRatePct = Σ total / currentFemales ×100`, `hatchingEggPct =
settable/total`, `floorEggPct`, `feedPerHenG`, `waterFeedRatio`.

### 3.3 Weekly
| `BodyWeightSample` | flockId, week, sex, sampleSize, avgWeightG, cvPct, uniformityPct (±10 %), weighedBy |
| `WeeklyFlockSummary` | flockId, week (age), eggsTotal, eggsSettable, layingRateAvg, hatchingEggPct, mortalityF/M, cumMortalityPctF/M, feedPerHenDayG, waterPerHenMl, eggsPerHenHoused (cum), avgEggWeight, bodyWeightF/M, uniformity, standard values snapshot, deviations |

### 3.4 Health (vet-only writes)
| Entity | Fields |
|---|---|
| `VaccinationSchedule` | flockId, lines [{ageDays/week, vaccineCatalogId, route, status PLANNED/DONE/SKIPPED, dueDate}] generated from `VaccinationProgramTemplate` (breed/kind) |
| `VaccinationEvent` | flockId, scheduleLineId?, date, vaccineCatalogId, vaccineBatchNo, stockBatchId?, doses, route, birdsVaccinated, administeredBy (employee, must be vet or under vet), reaction notes |
| `TreatmentRecord` | flockId, medicationCatalogId, stockBatchId, startDate, durationDays, dosePerBird, route, prescribingVetId, diagnosis, **eggWithdrawalEndDate** (computed), meatWithdrawalEndDate, status |
| `DiseaseEvent` | flockId, date, diagnosis, affectedCount, labResultRef, treatmentIds, notifiableDisease (bool → CRITICAL alert), resolvedAt |
| `BiosecurityInspection` | siteId/houseId, date, checklist jsonb (footbath, visitor log, rodent bait, wild-bird proofing, dead-bird disposal), score, findings, correctiveActions, dueDate, inspector |
| `SamplingEvent` | flockId, date, type `SEROLOGY/SALMONELLA_NCP/BACTERIOLOGY/PARASITOLOGY`, samples, lab, result `PENDING/NEGATIVE/POSITIVE`, resultDate, documentRef |

### 3.5 Eggs
| Entity | Fields |
|---|---|
| `EggBatch` | batchCode (`BS1-PF03-2026-10-15`), flockId, collectionDate, eggStoreId, qtySettable (from rounds), qtyAvailable (− dispatched − downgraded), status `IN_STORE/QUALITY_CHECKED/RESERVED/DISPATCHED/DOWNGRADED/DISCARDED`, ageDays (computed), flockAgeWeeks |
| `EggQualityInspection` | eggBatchId, date, sampleSize, crackedPct, dirtyPct, smallPct, avgWeightG, weightClass, result `PASSED/FAILED/DOWNGRADED`, inspector, notes |
| `EggStoreReading` | eggStoreId, at, tempC, rhPct, recordedBy or `SENSOR` |
| `EggDowngrade` | eggBatchId, qty, destination `TABLE_EGGS/DESTROYED`, reason |

### 3.6 Transfers
| `EggTransferOrder` | orderNo (`ETO-BS1-2026-000123`), siteId, destinationHatcheryId (site_directory), scheduledDate, status `PLANNED/APPROVED/DISPATCHED/RECEIVED/REJECTED/CANCELLED`, lines [{eggBatchId, qty}], totalQty, trays, cases, vehicleReg, driverName, departureAt, transportTempC, approvedBy/At, dispatchedBy/At, receivedQty, receivedAt, discrepancyNotes, deliveryNoteDocId, unitPriceRon? |

### 3.7 Inventory & costing — §C, §D with item categories FEED (breeder), MEDICATION,
VACCINE, LITTER, CLEANING, DISINFECTANT, EGG_TRAY.

## 4. Business rules (domain invariants → `BusinessRuleViolationException` codes)
1. `PF_HOUSE_NOT_AVAILABLE`: flock placement only in house status `EMPTY` (or `ACTIVE`
   for the same flock's move); capacity ≥ birds.
2. `PF_SHEET_DUPLICATE`: one sheet per house per date; sheet date ≤ today, ≥ today−2 for
   workers (supervisor may backfill up to 14 days, `pf.sheet.backfillDays`).
3. Mortality/culls update `currentFemales/Males` through `FlockCountAdjustment`
   immediately on **submit**; editing a submitted sheet creates compensating adjustments;
   `CLOSED` sheets immutable (supervisor closes day; auto-close after 48 h).
4. Laying rate < `pf.layingRate.minPct` (only when flock in PRODUCTION and age ≥ 28 wk)
   → `PF_LAYING_RATE_LOW` WARNING; drop > 5 points vs 7-day average → CRITICAL.
5. Settable eggs from a flock with an **active egg withdrawal** are forced into
   `DOWNGRADED` (table eggs) and cannot form a settable `EggBatch`
   (`PF_EGG_WITHDRAWAL_ACTIVE`, no override).
6. `EggBatch` dispatch requires `EggQualityInspection.result=PASSED` within 48 h and
   `ageDays ≤ pf.egg.maxStorageDays(hard 10)`; age > 7 → WARNING on order; crack %
   > `pf.egg.maxCrackPct` → FAILED.
7. Transfer order: `PLANNED→APPROVED` by supervisor (reserves batches), `→DISPATCHED`
   deducts `qtyAvailable`, generates delivery note PDF, publishes `egg-transfer.dispatched`;
   `RECEIVED/REJECTED` set only by hatchery event; `CANCELLED` releases reservations.
   Order can be cancelled by supervisor before DISPATCHED.
8. Vaccination schedule due within `pf.vaccination.dueWarningDays` → INFO alert to vet;
   overdue → WARNING.
9. Treatment creation consumes medication FEFO; insufficient → 422.
10. Flock age ≥ `pf.flock.endOfLayWarningWeeks` → INFO alert weekly; `depopulate` action
    requires all eggs dispatched or discarded, sets house `CLEANING`.
11. Egg store reading out of range → `PF_EGGSTORE_OUT_OF_RANGE` WARNING (CRITICAL if
    > 2 h sustained or temp > 22 °C).
12. Body weight uniformity < 70 % → WARNING to supervisor & vet.
13. Notifiable disease (list in settings `pf.notifiableDiseases`: AI, ND, …) → CRITICAL to
    all site roles + MANAGER; flock `quarantine=true` blocks egg transfers.

## 5. API (`/api/v1`, all scoped by `siteId` claim check)
Sites/houses/stores (§A). Flocks: `GET/POST /flocks`, `GET/PATCH /flocks/{id}`,
`POST /flocks/{id}/move-house`, `POST /flocks/{id}/start-production`,
`POST /flocks/{id}/count-adjustments`, `POST /flocks/{id}/depopulate`,
`GET /flocks/{id}/summary` (KPIs vs standard), `GET /flocks/{id}/weekly`.
Daily: `GET /daily-sheets?houseId&from&to`, `GET /daily-sheets/by-date?houseId&date`
(returns or creates DRAFT), `PUT /daily-sheets/{id}` (full upsert of sections),
`POST /daily-sheets/{id}/egg-rounds`, `POST /daily-sheets/{id}/submit`, `/close`,
`/reopen`(supervisor). `GET /houses/{id}/today` (dashboard card: sheet status, counts).
Weekly: `POST /flocks/{id}/body-weights`, `GET /flocks/{id}/weekly-summaries`.
Health: `/flocks/{id}/vaccination-schedule` GET/POST generate/PATCH line,
`/vaccinations` POST, `/treatments` POST/GET, `/disease-events`, `/biosecurity-inspections`,
`/samplings` + result upload.
Eggs: `GET /egg-batches?siteId&status&flockId`, `POST /egg-batches/{id}/inspections`,
`POST /egg-batches/{id}/downgrade`, `GET/POST /egg-stores/{id}/readings`,
`GET /egg-stock?siteId` (available by batch, age colour-coded).
Transfers: `GET/POST /egg-transfer-orders`, `GET /egg-transfer-orders/{id}`,
`POST …/approve`, `/dispatch` `{vehicleReg, driverName, transportTempC, trays}`,
`/cancel`, `GET …/delivery-note.pdf`.
Inventory §C under `/inventory/**`, costing §D under `/costing/**`, lookups §B,
`/documents`.

## 6. Events
Published: `ppms.parent-flock.flock` (`flock.registered/moved/production-started/
count-changed/depopulated`), `ppms.parent-flock.production` (`daily-sheet.submitted
{houseId, flockId, date, eggsTotal, settable, mortalityF/M, feedKg, layingRate}`,
`weekly-summary.computed`, `body-weight.recorded`), `ppms.parent-flock.egg-transfer`
(`planned/approved/dispatched {orderNo, destinationHatcheryId, lines[{eggBatchCode,
flockCode, collectionDate, qty, flockAgeWeeks, breedCode}], scheduledDate, transport…}`,
`cancelled`), `ppms.parent-flock.health` (`vaccination.recorded`, `treatment.recorded`,
`disease.reported`, `sampling.result`), `ppms.inventory`, `ppms.alerts`, `ppms.audit`,
`ppms.site.directory`, costing.
Consumed: `ppms.hatchery.egg-reception` (`received {orderId, receivedQty, rejectedQty,
notes}` → order RECEIVED/REJECTED + discrepancy alert if |Δ| > 1 %), `ppms.admin.*`
(catalogs, employees, settings), `ppms.site.directory` (hatcheries list).

## 7. Jobs
- 00:30 daily: compute `ageDays` alerts for egg batches; expiring stock; vaccination due.
- Monday 01:00: `WeeklyFlockSummary` for previous week per flock (also recomputed on
  sheet edit for that week).
- Hourly: egg store sustained-breach detection; auto-close sheets > 48 h.

## 8. Migrations
`VPF001__base_tables_outbox_audit.sql` … `VPF002__sites_houses_stores`,
`VPF003__flocks`, `VPF004__daily_sheets`, `VPF005__weekly`, `VPF006__health`,
`VPF007__eggs`, `VPF008__transfers`, `VPF009__inventory`, `VPF010__costing`,
`VPF011__read_models`, `VPF012__documents`, `VPF013__indexes_constraints`
(unique `(house_id, date)`, partial unique on `house.current_flock_id`, check qty ≥ 0).

## 9. Tests
Unit: laying rate & derived KPIs, count adjustments, egg age/withdrawal rules, FEFO
consumption, transfer state machine, uniformity. IT per feature: duplicate sheet 409,
submit updates counts and emits event, treatment blocks settable eggs, inspection
FAILED blocks order, dispatch emits event + PDF stored (MinIO container or local FS),
reception event closes order, alerts emitted, scope checks (worker other site → 403).

## 10. Task list

| # | Feature | Tasks | Acceptance |
|---|---|---|---|
| PF0 | Bootstrap | module skeleton, V001, security, read-model consumers (§B), site registry (§A) with events | sites visible in admin-service directory |
| PF1 | Flock management | ParentFlock, houses, assignments, count adjustments, phases, summary vs standard | IT state machine |
| PF2 | Daily sheet | sheet + sections, rounds, submit/close, derived KPIs, duty pre-fill, alerts rule 4 | worker flow IT; duplicate 409 |
| PF3 | Weekly | body weights, weekly summary job + recompute, standard deviations | unit for formulas |
| PF4 | Health | schedule templates, vaccinations (stock consume), treatments (withdrawal), disease events, biosecurity, sampling | vet-only 403 IT; withdrawal blocks eggs |
| PF5 | Eggs | EggBatch auto-created from submitted rounds (per flock/day), inspections, downgrades, store readings + breach alerts, stock view | IT |
| PF6 | Egg transfer | order lifecycle, reservation, delivery note PDF, events, hatchery reception consumer | end-to-end IT with simulated hatchery event |
| PF7 | Inventory (§C) | all | IT FEFO & alerts |
| PF8 | Costing (§D) | automatic feed/med/vaccine/egg costs; cost per hatching egg; manual entries | IT |
| PF9 | Exports, OpenAPI, metrics (`ppms_pf_laying_rate{site,house}`) | — | — |

## Exit criteria
Full breeder cycle demo: register site/house/flock → 7 daily sheets → weekly summary →
inspection → transfer order dispatched → (simulated) hatchery reception closes order;
all KPIs visible via API; alerts in notification-service.
