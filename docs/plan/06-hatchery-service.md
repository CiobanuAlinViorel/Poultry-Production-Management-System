# 06 — hatchery-service (stație de incubație)

Port 8082 · DB `ppms_hatchery` · schema `hatchery` · package `com.ppms.hatchery` ·
migrations prefix `VHA`. Reuses common blocks §A–§E from `05-parent-flock-service.md`.

---

## 1. Business process
1. **Egg reception**: internal (EggTransferOrder from parent flock, pre-announced by
   event) or external supplier (manual with delivery note). Incoming check: count trays,
   temperature of load, cracked/dirty %, sample weight, egg age, flock age; accept, accept
   with downgrade, or reject (notify source). Each acceptance creates an `EggBatch`
   (hatchery's own entity).
2. **Egg storage** 15–18 °C; eggs older than 7 days get "SPIDES"/longer pre-warming or
   are prioritised (FEFO by collection date when planning sets).
3. **Setting**: pre-warm 6–12 h at 24–26 °C, load setter trolleys; `IncubationBatch`
   = eggs from one or more `EggBatch`es set in one setter at one time (keep flock
   traceability by `setLines`). Setter program: 37.6–37.8 °C, 53–57 % RH, turn every
   hour, CO₂ < 0.4 %. Readings per shift (3/day) or sensor.
4. **Candling** day 7–10: remove clears (infertile) + early deads → fertility %.
   Optional day 18 candling at **transfer**: remove late deads; eggs moved to hatcher
   baskets; `egg weight loss %` sample (target 11–13 %).
5. **Hatcher** days 18–21: 36.8–37.2 °C, RH rising to 65–75 %; hatch window monitoring.
6. **Pull/hatch day 21**: count hatched, culls (weak, deformed, unhealed navel), DOA;
   **grading** A (saleable), B (second), C (only with farm acceptance), cull; chick
   weight sample (40–45 g; chick yield 67–68 % of egg weight); sexing optional;
   **vaccination** (Marek SC/in-ovo, IB/ND spray) with vaccine stock consumption; boxing
   (100 or 102/box).
7. **Chick dispatch**: `ChickTransferOrder` to a farm/house, climate truck (24–26 °C,
   RH 50–60 %), chicks ≤ 24 h old; delivery note with hatch & flock traceability.
   Reception feedback from farm (DOA, counts) closes the order.
8. **Hatcher/setter sanitation** between batches; hatchery waste (shells, unhatched,
   culls) weights for by-product disposal records.
KPIs: fertility %, hatch of set %, hatch of fertile %, embryonic mortality early/mid/
late %, culls %, chick yield %, chick quality score, eggs set per week, saleable chicks
per week, per flock-of-origin performance (feeds back to breeder evaluation), cost per
saleable chick.

## 2. Roles & permissions
| Role | Can |
|---|---|
| `HATCHERY_SUPERVISOR` | all at own hatchery; approve receptions/rejections; approve & dispatch chick orders; set programs |
| `HATCHERY_OPERATOR` | receptions (draft), readings, setting, candling, transfer, hatch counts, grading, boxing |
| `HATCHERY_QC` | incoming egg checks, chick quality scoring, lab samplings, holds |
| `HATCHERY_VET` | vaccinations, biosecurity, health events, holds |
| `FARM_STOREKEEPER` | inventory |
| `MANAGER`/`ACCOUNTANT` | read, costing |
Permissions `ha:site:*`, `ha:reception:{read,write,approve}`, `ha:incubation:*`,
`ha:hatch:*`, `ha:chick-transfer:{read,write,approve,dispatch}`, `ha:qc:*`,
`ha:health:*`, `ha:inventory:*`, `ha:costing:*`.

## 3. Domain model (schema `hatchery`)

### 3.1 Structure
| Entity | Fields |
|---|---|
| `Hatchery` | §A (+ weeklySetCapacity) |
| `Incubator` | hatcheryId, code, type `SETTER/HATCHER/COMBI`, manufacturer, model, capacityEggs, trolleys, status `AVAILABLE/IN_USE/CLEANING/MAINTENANCE/OUT_OF_SERVICE`, currentBatchIds[] (multi-stage setters hold several batches), isMultiStage |
| `EggStoreRoom` | hatcheryId, code, capacityEggs, targetTemp/Rh |
| `ChickHoldingRoom` | hatcheryId, code, capacityBoxes |

### 3.2 Reception
| Entity | Fields |
|---|---|
| `IncomingEggAnnouncement` | read model from `egg-transfer.dispatched` event: orderId, orderNo, sourceSiteId/code, lines, scheduledDate, status `ANNOUNCED/ARRIVED/CLOSED` |
| `EggReception` | receptionNo (`ER-HA1-2026-000045`), hatcheryId, source `INTERNAL/EXTERNAL`, eggTransferOrderId?, externalSupplierId?, externalDeliveryNoteNo?, arrivedAt, vehicleReg, driverName, loadTempC, declaredQty, countedQty, trays, cases, status `DRAFT/CHECKED/ACCEPTED/PARTIALLY_ACCEPTED/REJECTED`, checkedBy, approvedBy, notes, documentRef (photo/scan) |
| `IncomingEggCheck` | receptionId, sampleSize, crackedPct, dirtyPct, smallPct, misshapenPct, avgWeightG, eggAgeDays, flockAgeWeeks, result `PASS/DOWNGRADE/FAIL`, inspector |
| `EggBatch` | batchCode (`HA1-EB-2026-10-16-03`), hatcheryId, receptionId, originFlockCode?, originFlockId?, originSiteId?, originSiteCode, breedCode, collectionDate, flockAgeWeeks, qtyAccepted, qtyAvailable, qtySet, qtyDiscarded, storeRoomId, status `IN_STORE/PARTIALLY_SET/FULLY_SET/DISCARDED`, qualityGrade |
| `EggStoreReading` | roomId, at, temp, rh, source |

### 3.3 Incubation
| Entity | Fields |
|---|---|
| `IncubationBatch` | batchCode (`HA1-IB-2026-W42-07`), hatcheryId, setterId, setAt, expectedCandlingDate, expectedTransferDate, expectedHatchDate, qtySet, status `SET/CANDLED/TRANSFERRED/HATCHED/CLOSED/ABORTED`, programId, hatcherId?, transferredAt?, hatchedAt?, notes |
| `IncubationSetLine` | incubationBatchId, eggBatchId, qty, trolleyNos[] (traceability by flock inside a set) |
| `IncubationProgram` | hatcheryId?, name, phase setpoints [{dayFrom, dayTo, tempC, rhPct, co2MaxPpm, turningPerHour}] |
| `IncubatorReading` | incubatorId, incubationBatchId?, at, shift, tempC, rhPct, co2Ppm, turningOk, alarmFlags, source `MANUAL/SENSOR`, recordedBy |
| `CandlingRecord` | incubationBatchId, day (10 or 18), date, sampleOrFull `FULL/SAMPLE`, trayCount, clearInfertile, earlyDead, midDead?, lateDead?, cracked, contaminated, fertile (computed), fertilityPct, performedBy |
| `EggWeightLossSample` | incubationBatchId, day, sampleSize, avgWeightG, lossPct |
| `TransferRecord` | incubationBatchId, at, hatcherId, eggsTransferred, eggsRemoved, baskets, performedBy |

### 3.4 Hatch
| Entity | Fields |
|---|---|
| `HatchRecord` | incubationBatchId, pulledAt, totalHatched, culls, deadInShell (unhatched count), pippedNotHatched, saleable (computed), hatchOfSetPct, hatchOfFertilePct, embryonicMortalityEarly/Mid/LatePct, chickAvgWeightG, chickYieldPct, qualityScore (Pasgar/Tona avg), performedBy |
| `ChickGrading` | hatchRecordId, gradeA, gradeB, gradeC, culls, sexed (bool), males?, females? |
| `ChickBatch` | chickBatchCode (`HA1-CB-2026-10-16-07`), hatchRecordId, hatcheryId, hatchDate, grade, qtyAvailable, qtyBoxed, boxes, holdingRoomId, status `AVAILABLE/RESERVED/DISPATCHED/CULLED`, originEggBatchIds[], originFlockCodes[] |
| `HatcheryVaccinationEvent` | chickBatchId(s), vaccineCatalogId, stockBatchId, route (`IN_OVO` → incubationBatchId at day 18, `SC`, `SPRAY`), doses, performedBy (vet or operator under vet) |
| `HatchWasteRecord` | incubationBatchId, shellsKg, unhatchedKg, cullsKg, disposalRoute |

### 3.5 Chick transfer
| `ChickTransferOrder` | orderNo (`CTO-HA1-2026-000311`), hatcheryId, destinationFarmId, destinationHouseId? (from site_directory sub-units), scheduledDate, lines [{chickBatchId, grade, qty}], totalQty, boxes, acceptGradeC (bool, must be confirmed by farm — flag arrives via `farm.lot.planned` or set manually with reference), status `PLANNED/APPROVED/DISPATCHED/RECEIVED/REJECTED/CANCELLED`, vehicleReg, driverName, departureAt, truckTempC, truckRhPct, approvedBy, dispatchedBy, receivedQty, doaReported, receivedAt, deliveryNoteDocId, unitPriceRon? |

### 3.6 QC & health
`QcHold { targetType (EggBatch/IncubationBatch/ChickBatch), targetId, reason, placedBy,
placedAt, releasedBy, releasedAt }`, `HatcherySampling` (Salmonella fluff/meconium
samples per hatch, lab result), `HatcheryBiosecurityInspection`, `SanitationRecord`
(incubator/room, cleaned/disinfected, product, by, at → sets `AVAILABLE`).

### 3.7 Inventory & costing — §C (categories FUMIGANT, DISINFECTANT, VACCINE, SPRAY,
CHICK_BOX, EGG_TRAY, CLEANING, CONSUMABLE), §D (cost center `IncubationBatch` → cost
per saleable chick; egg cost from transfer `unitPrice` or external invoice price).

## 4. Business rules
1. Reception for an internal order requires announcement present (`HA_ORDER_UNKNOWN`
   otherwise but allowed with supervisor override flag + reason — events may lag).
   Accepting publishes `egg-reception.received {orderId, receivedQty, rejectedQty}`
   (parent-flock closes order).
2. `IncomingEggCheck.result=FAIL` → reception REJECTED, no EggBatch, event `rejected`
   with reasons, CRITICAL alert to source site supervisor + hatchery supervisor.
3. Egg age at set ≤ `ha.egg.maxAgeAtSetDays` (default 10, hard 14) → `HA_EGG_TOO_OLD`;
   warning > 7. FEFO suggestion when planning a set.
4. Setting: incubator must be `AVAILABLE` (or `IN_USE` multi-stage with free capacity);
   Σ set ≤ capacity (`HA_INCUBATOR_CAPACITY`); each set line reduces `EggBatch.qtyAvailable`.
5. Candling day-10 record required before transfer; transfer requires
   `hatcher.AVAILABLE` + capacity; hatch record requires transfer; one hatch record per
   incubation batch; totals must reconcile: `hatched + deadInShell + pipped ≤ fertile after
   transfer` (`HA_COUNTS_INCONSISTENT`).
6. KPI alerts: fertility < `ha.fertility.minPct` WARNING; hatch of set <
   `ha.hatchability.minPct` WARNING (CRITICAL if < min−10); per origin flock, 3
   consecutive low batches → INFO to MANAGER ("breeder flock quality").
7. Readings out of setpoint range → WARNING; sustained > 30 min or temp deviation > 0.5 °C
   → CRITICAL to supervisor (dedup per incubator per hour).
8. `ChickTransferOrder`: Grade C lines only if `acceptGradeC=true` with reference
   (`HA_GRADE_C_NOT_ACCEPTED`); chicks age at scheduled dispatch ≤ `ha.chick.maxAgeHoursAtDispatch`
   → otherwise `HA_CHICKS_TOO_OLD` (supervisor override w/ reason to 36 h); QC hold on
   chick batch blocks dispatch; APPROVED reserves, DISPATCHED deducts and emits event +
   PDF; farm reception event sets RECEIVED/REJECTED and records DOA; unsold chicks after
   48 h → must be recorded as culled (`cull` action) with reason.
9. In-ovo vaccination only between day 17.5–19 (transfer window); vaccine stock FEFO.
10. Incubator → `CLEANING` automatically after hatch pull; `SanitationRecord` → `AVAILABLE`.
11. Edits after `CLOSED` batch forbidden; supervisor `reopen` audited.

## 5. API (`/api/v1`)
Sites (§A: `/hatcheries`, `/incubators`, `/rooms`). Reception: `GET /incoming-announcements`,
`POST /egg-receptions` (from announcement or external), `PUT /egg-receptions/{id}`,
`POST …/check`, `POST …/accept` `{acceptedQty, downgradeQty, storeRoomId}`, `POST …/reject`,
`GET /egg-batches?status&originFlock&ageDays`, `POST /egg-batches/{id}/discard`,
`GET/POST /rooms/{id}/readings`.
Incubation: `GET/POST /incubation-programs`, `POST /incubation-batches` `{setterId, programId,
lines[]}` (returns planned dates), `GET /incubation-batches?status&setter&week`,
`GET /incubation-batches/{id}` (timeline), `POST …/candlings`, `POST …/weight-loss`,
`POST …/transfer` `{hatcherId, eggsRemoved}`, `POST …/hatch` (hatch record + grading in
one payload), `POST …/waste`, `POST …/abort` `{reason}`, `GET/POST /incubators/{id}/readings`,
`POST /incubators/{id}/sanitation`, `GET /planning/setting-calendar?week` (capacity vs
planned sets; FEFO suggestion).
Chicks: `GET /chick-batches?status&grade`, `POST /chick-batches/{id}/box`, `/cull`,
`POST /vaccinations`, `GET/POST /chick-transfer-orders`, `POST …/approve`, `/dispatch`,
`/cancel`, `GET …/delivery-note.pdf`.
QC/health: `/qc-holds` POST/release, `/samplings`, `/biosecurity-inspections`.
Inventory §C, costing §D, lookups §B, `/documents`. KPI: `GET /kpi/hatch?from&to&groupBy=week|flock|setter`.

## 6. Events
Published: `ppms.hatchery.egg-reception` (`received`, `rejected`),
`ppms.hatchery.incubation` (`batch.set {batchCode, setterId, qty, lines[{eggBatchCode,
originFlockCode, qty}]}`, `candling.recorded {fertilityPct…}`, `transferred`,
`hatched {hatchOfSetPct, saleable, grades}`), `ppms.hatchery.chick-transfer`
(`planned/approved/dispatched {orderNo, destinationFarmId, destinationHouseId, lines[{chickBatchCode,
grade, qty, hatchDate, originFlockCodes, breedCode}], transport…}`, `cancelled`),
`ppms.inventory`, `ppms.alerts`, `ppms.audit`, `ppms.site.directory`, costing.
Consumed: `ppms.parent-flock.egg-transfer.dispatched` (→ announcement),
`ppms.farm.chick-reception.received/rejected` (→ order status, DOA), `ppms.farm.lot`
(`lot.planned` carries acceptGradeC flag & house), `ppms.admin.*`, `ppms.site.directory`
(farms + houses, breeder sites).

## 7. Jobs
Every 5 min: incubator reading breach evaluation (sensor feeds); daily: egg age alerts,
chick age alerts, expiring stock, expected events due today (candling/transfer/hatch →
INFO task list for operators); weekly: KPI roll-up per origin flock.

## 8. Migrations `VHA001…VHA012` (same split as 05 §8).

## 9. Tests
Unit: fertility/hatch formulas, count reconciliation, FEFO set planning, state machines,
age rules. IT: announcement → reception → accept → EggBatch → set → candling →
transfer → hatch → grading → chick order → dispatch → farm reception event closes;
reject path notifies parent flock; capacity violations 422; grade C rule; PDF stored.

## 10. Task list
| # | Feature | Acceptance |
|---|---|---|
| H0 | Bootstrap, §A sites/incubators/rooms, §B read models, announcement consumer | directory event seen in admin |
| H1 | Egg reception (internal + external) + checks + EggBatch + events to parent flock | IT both paths |
| H2 | Incubation programs, batches, set lines, readings + breach alerts, candling, weight loss, transfer | IT |
| H3 | Hatch record, grading, chick batches, waste, vaccination (stock), sanitation | IT reconciliation |
| H4 | Chick transfer orders + PDF + farm reception consumer | end-to-end IT |
| H5 | QC holds, samplings, biosecurity | IT hold blocks dispatch |
| H6 | Inventory §C | IT |
| H7 | Costing §D (cost/saleable chick) | IT |
| H8 | Planning calendar, KPI endpoints, exports, metrics | — |

## Exit criteria
Demo: parent-flock dispatch event → hatchery reception → full 21-day cycle (with
`TestClock`) → chick order dispatched → simulated farm reception closes it; KPIs per
origin flock available.
