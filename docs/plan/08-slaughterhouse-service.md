# 08 — slaughterhouse-service (abator + vânzări)

Port 8086 · DB `ppms_slaughterhouse` · schema `slaughterhouse` · package
`com.ppms.slaughterhouse` · migrations prefix `VSL`. Reuses common blocks §A–§E from
`05-parent-flock-service.md`. Includes **sales & invoicing data** (customers, price
lists, sales orders, dispatch, invoice/aviz data — no accounting ledger).

---

## 1. Business process
1. **Planning**: weekly kill plan from farm delivery requests (`delivery.planned`
   events): slot per day/hour per line → confirm back to farm (`delivery.confirmed`).
   Capacity: birds/hour per line × hours.
2. **Reception**: truck arrives → gate time, truck reg, driver, transport duration (from
   farm departure), truck temperature, **weigh-in gross**, FCI document check (must be
   present & ≥ 24 h, otherwise official vet decision), movement document; unloading to
   **lairage** (fans, misting; min rest 1–2 h, max total transport+lairage 12 h); DOA
   count; weigh-out tare → **live weight received**; discrepancy vs farm declared →
   notify farm.
3. **Ante-mortem inspection** (official veterinarian, DSVSA): per batch (= one farm
   delivery = one `LiveBirdReception`): passed / condemned (count, reason), HOLD,
   decision `PASSED / PARTIALLY_PASSED / REJECTED / LOGISTIC_SLAUGHTER` (e.g.
   Salmonella-positive flocks slaughtered at the end of the day). Batch cannot enter the
   line otherwise.
4. **Slaughter session** per batch per line: start/end, line speed, hanging → stunning
   (electrical water-bath parameters: V/Hz/mA or CAS gas concentrations — recorded for
   Reg. 1099/2009) → bleeding → scalding (temp °C) → plucking → evisceration →
   **post-mortem inspection** (condemnations by cause: septicaemia/toxaemia, cachexia,
   ascites, airsacculitis, cellulitis/dermatitis, contamination, bruising/fractures,
   DOA/dead in lairage, other; whole and partial) → washing → **chilling** (air/immersion;
   core ≤ 4 °C) → weighing hot/cold carcass → **yield**.
5. **Grading & cutting**: whole carcasses Grade A / B (weight classes e.g. 900–1100 g,
   1100–1300…), cut-up (breast fillet, inner fillet, whole leg, thigh, drumstick, wing,
   back/carcass frame, neck, skin), offal (liver, heart, gizzard), by-products (feet,
   heads, blood, feathers, viscera → rendering, category 3 ABP records), **cut yields**.
6. **Packaging & labelling**: product lines (SKU), pack type (tray MAP, bag, bulk crate,
   carton), unit weight (fixed/variable), label with **production lot code**, slaughter
   date, best-before (slaughter date + shelf life per product category/packaging),
   storage temp, oval EC mark (approval number); fresh vs frozen (blast freezer).
7. **Cold chain**: chambers (chilled 0–4 °C, frozen ≤ −18 °C), stock-in by pallet/crate
   (`StockUnit`) with lot code, temperature readings (sensor or manual, every 2–4 h),
   FEFO picking, blocked expired stock, QA holds (micro sampling 2073/2005 — neck skins
   Salmonella/Campylobacter weekly), temperature breach handling.
8. **Sales & dispatch**: customers (retail chains, wholesalers, HoReCa, export), price
   lists (per customer/SKU, validity), sales orders (requested delivery date, lines),
   picking (FEFO, lot allocation), **dispatch order** per truck/route with dispatch note
   (aviz) listing lot codes → traceability, loading temp, seals; **invoice data** (number
   series, VAT, totals) exported to accounting (CSV/XML hook, e-Factura-ready payload);
   returns/credit notes (quality claims).
9. **Session close**: per-session yields, condemnation %, DOA %, cost per kg carcass
   (live birds purchase/transfer value + processing cost), feedback event to farm
   (post-mortem findings → next FCI).
KPIs: DOA %, dead-in-lairage %, transport time compliance, yield hot/cold %, condemnation
% by cause per farm/lot, line speed & downtime, cut-up yields vs standard, give-away %
(fixed weight packs), cold-store occupancy %, temperature compliance, FEFO compliance,
order fill rate, on-time dispatch, revenue & margin per customer/SKU, claims rate.

## 2. Roles & permissions
| Role | Can |
|---|---|
| `SLAUGHTER_MANAGER` | all at own plant; approve plan; approve dispatch; price lists; close sessions |
| `SLAUGHTER_RECEPTION` | receptions, weighings, lairage, FCI check |
| `SLAUGHTER_INSPECTOR` | ante-/post-mortem inspection, holds, decisions (official vet; identity must have `VET_LICENCE` certification — enforced by admin-service assignment rule) |
| `SLAUGHTER_LINE_SUPERVISOR` | sessions, line parameters, counts/weights, grading, cutting, packaging |
| `SLAUGHTER_QA` | QA holds, micro samplings, temperature breach decisions, HACCP CCP records, releases |
| `SLAUGHTER_COLD_CHAIN` | chambers, stock-in/out, readings, picking |
| `SLAUGHTER_SALES` / `SALES` | customers, orders, dispatch orders (not approve), invoice data |
| `ACCOUNTANT` | read invoices/costing, exports |
| `MANAGER` | read |
Permissions `sl:site:*`, `sl:plan:*`, `sl:reception:*`, `sl:inspection:write`,
`sl:session:*`, `sl:grading:*`, `sl:packaging:*`, `sl:cold-chain:*`, `sl:qa:*`,
`sl:customer:*`, `sl:pricing:{read,write}`, `sl:sales-order:*`, `sl:dispatch:{read,write,approve}`,
`sl:invoice:{read,issue}`, `sl:inventory:*`, `sl:costing:*`.

## 3. Domain model (schema `slaughterhouse`)

### 3.1 Structure
| Entity | Fields |
|---|---|
| `Slaughterhouse` | §A (+ ecApprovalNo "RO xxx EC", linesCount, dailyCapacityBirds) |
| `SlaughterLine` | plantId, code, nominalSpeedBph, stunningType `WATERBATH/CAS`, status |
| `LairageBay` | plantId, code, capacityModules, fans/misting |
| `ColdChamber` | plantId, code, type `CHILL/FREEZER/BLAST/DISPATCH_DOCK`, capacityKg, capacityPallets, targetTempMin/Max, currentStockKg (computed), status |
| `ProductSku` | code, nameRo/En, category `WHOLE_FRESH/WHOLE_FROZEN/CUTS_FRESH/CUTS_FROZEN/OFFAL/BY_PRODUCT/MAP_PACK`, cutType?, gradeRequirement, packType, unitWeightKg? (fixed) or variable, shelfLifeDays, storage `CHILL/FROZEN`, eanCode, active |
| `Vehicle`/`Driver` (light) | reg, type, refrigerated, capacity; driver name, licence, phone |

### 3.2 Planning & reception
| Entity | Fields |
|---|---|
| `IncomingDeliveryRequest` | read model from `farm.delivery.planned`: orderId, orderNo, farmId/code, lotCode, scheduledDate, plannedBirds, plannedAvgWeight, type, fciStatus, status `REQUESTED/CONFIRMED/ARRIVED/CLOSED/CANCELLED` |
| `KillPlan` | plantId, date, status `DRAFT/PUBLISHED`, slots [{lineId, startAt, endAt, deliveryRequestId, plannedBirds}] ; publish → `delivery.confirmed` events |
| `LiveBirdReception` | receptionNo (`LBR-SL1-2026-001203`), plantId, farmDeliveryOrderId?, farmId/code, lotCode, traceability jsonb (from event), arrivedAt, vehicleReg, driverName, departureFromFarmAt, transportMinutes (computed), transportTempC, modules, crates, declaredBirds, fciReceived (bool), fciDocumentRef, fciCheckedBy, fciOk, movementDocNo, grossKg, tareKg, liveWeightKg (computed), doaCount, deadInLairage, countedBirds (= line count later), lairageBayId, lairageStartAt, lairageEndAt, status `ARRIVED/IN_LAIRAGE/CLEARED/ON_HOLD/REJECTED/PROCESSED/CLOSED`, discrepancyNotes |
| `WeighingRecord` | receptionId, type `GROSS/TARE/LIVE_SAMPLE`, kg, scaleId, at, ticketNo, source `MANUAL/SCALE_API` |

### 3.3 Inspection
| Entity | Fields |
|---|---|
| `AnteMortemInspection` | receptionId, inspectorId (must hold SLAUGHTER_INSPECTOR), at, birdsInspected, passed, condemned, reasons [{cause, count}], findings, decision `PASSED/PARTIALLY_PASSED/REJECTED/LOGISTIC_SLAUGHTER/HOLD`, holdReason, authorityRef, documentRef |
| `PostMortemInspection` | sessionId, inspectorId, lines [{cause `SEPTICAEMIA_TOXAEMIA/CACHEXIA/ASCITES/AIRSACCULITIS/CELLULITIS_DERMATITIS/CONTAMINATION/BRUISING_FRACTURES/DOA/ABNORMAL_COLOUR/OTHER`, wholeCount, wholeKg, partialCount, partialKg}], totalCondemnedPct, notes |

### 3.4 Processing
| Entity | Fields |
|---|---|
| `SlaughterSession` | sessionNo (`SS-SL1-2026-10-17-L1-03`), plantId, lineId, receptionId, startAt, endAt, supervisorId, lineSpeedBph, downtimeMinutes, downtimeReasons[], birdsHung, birdsSlaughtered, stunning {type, voltage, frequencyHz, currentMa or gasPct, durationS}, scaldTempC, chillType, chillExitTempC, liveWeightKg (from reception), hotCarcassKg, coldCarcassKg, yieldHotPct, yieldColdPct, status `OPEN/CLOSED`, productionLotCode (`L-SL1-261017-03` printed on labels), kpis jsonb |
| `CcpRecord` (HACCP) | sessionId, ccp `SCALD_TEMP/CHILL_TEMP/METAL_DETECTION/EVISCERATION_CONTAMINATION/WATER_CHLORINE`, at, value, limitMin/Max, ok, correctiveAction, by |
| `CarcassGrading` | sessionId, gradeA {count, kg}, gradeB {count, kg}, weightClassBreakdown [{classFrom, classTo, count, kg}], downgradeReasons |
| `CutProduction` | sessionId, cutType `BREAST_FILLET/INNER_FILLET/WHOLE_LEG/THIGH/DRUMSTICK/WING/BACK/NECK/SKIN/LIVER/HEART/GIZZARD/FEET/HEAD/MIXED_OFFAL`, inputCarcassCount, inputKg, outputKg, yieldPct (vs input or vs live), standardYieldPct |
| `ByProductRecord` | sessionId, category `FEATHERS/BLOOD/VISCERA_CAT3/HEADS_FEET/CONDEMNED_CAT2`, kg, destination (rendering plant), abpDocNo |
| `PackagingRun` | sessionId, skuId, productionLotCode, packedAt, packs, netKg, fixedWeightGiveAwayKg?, bestBeforeDate (slaughter date + sku.shelfLifeDays; frozen: freezing date based), labelTemplate, qaStatus `RELEASED/HOLD`, destination chamber |
| `StockUnit` | unitCode (pallet/crate barcode `SU-…`), skuId, productionLotCode, sessionId, packs, netKg, bestBeforeDate, chamberId, status `IN_STOCK/RESERVED/PICKED/DISPATCHED/BLOCKED/DISPOSED`, stockedInAt, tempAtStockIn |

### 3.5 Cold chain & QA
| Entity | Fields |
|---|---|
| `ChamberReading` | chamberId, at, tempC, rhPct?, source, doorOpen? |
| `TemperatureBreach` | chamberId, from, to, maxTempC, affectedStockUnitIds[], qaDecision `RELEASE/REWORK/DISPOSE`, decidedBy, notes |
| `QaHold` | targetType (StockUnit/PackagingRun/Session/Reception), targetId, reason, placedBy/At, released… |
| `MicroSampling` | sessionId or chamberId, date, type `NECK_SKIN_SALMONELLA/CAMPYLOBACTER/SURFACE_SWAB/WATER`, samples, lab, result, resultDate, documentRef, cfu? |
| `StockMovement` (cold chain) | stockUnitId, type `STOCK_IN/TRANSFER/PICK/DISPATCH/RETURN/DISPOSE/BLOCK/UNBLOCK`, fromChamberId, toChamberId, at, by, refType/refId |

### 3.6 Sales
| Entity | Fields |
|---|---|
| `Customer` | code, name, category `RETAIL_CHAIN/WHOLESALER/HORECA/EXPORT/PROCESSOR/OTHER`, cui/vatNo, regCom, address, deliveryAddresses[], contacts[], paymentTermsDays, creditLimitRon?, currency `RON/EUR`, active, labelRequirements (private label?), docsLanguage |
| `PriceList` | name, customerId? (null = general), currency, validFrom, validTo, lines [{skuId, pricePerKg or pricePerUnit, minQty?}] |
| `SalesOrder` | orderNo (`SO-2026-004512`), customerId, deliveryAddressId, requestedDeliveryDate, lines [{skuId, qtyKg or packs, unitPrice (snapshot), discountPct}], status `DRAFT/CONFIRMED/PICKING/PARTIALLY_DISPATCHED/DISPATCHED/INVOICED/CANCELLED`, salesRepId, notes, customerPoNo |
| `PickList` | salesOrderId(s), createdAt, lines [{skuId, qty, allocatedStockUnits[{unitId, packs, kg}]}] FEFO, pickedBy, status |
| `DispatchOrder` | dispatchNo (`DO-SL1-2026-002310`), plantId, date, vehicleReg, driverName, refrigerated, setTempC, sealNos[], route/stops [{customerId, salesOrderIds[], addressId}], lines [{salesOrderLineId, stockUnitId, skuId, packs, netKg, productionLotCode, bestBefore}], loadingStartAt/EndAt, loadingTempC, status `PLANNED/LOADING/APPROVED/DISPATCHED/DELIVERED/CANCELLED`, approvedBy, dispatchNoteDocId (aviz), traceabilitySummary jsonb |
| `Invoice` (data only) | invoiceNo (series + seq from settings), customerId, dispatchOrderIds[], issueDate, dueDate, lines [{skuId, description, qty, unit, unitPrice, vatRatePct, net, vat, gross}], totals, currency, status `DRAFT/ISSUED/CANCELLED/CREDITED`, exportPayload jsonb (e-Factura-ready UBL fields), exportedAt |
| `CreditNote` | invoiceId, reason, lines, totals |
| `CustomerClaim` | customerId, dispatchOrderId, date, type `QUALITY/QUANTITY/TEMPERATURE/LABEL/LATE`, description, affectedLots[], resolution, creditNoteId? |

### 3.7 Inventory & costing — §C (PACKAGING, CLEANING, DISINFECTANT, CONSUMABLE,
LABELS, CO2/GAS, CRATES), §D (cost center `SlaughterSession`: live bird value (transfer
price × kg or farm cost from `lot.closed` cost if internal), labour (duty hours), energy,
water, packaging (stock), ABP disposal, lab → **cost per kg carcass / per SKU kg**;
margin = invoice net − allocated cost, computed in reporting).

## 4. Business rules
1. Plan: slot capacity ≤ line speed × minutes; publishing plan emits `delivery.confirmed
   {orderId, slot}` to farm; changes after confirmation re-emit with `rescheduled=true`.
2. Reception: for internal deliveries requires `IncomingDeliveryRequest` (override with
   reason); `transportMinutes + lairage` ≤ `sl.transport.maxHours`×60 else WARNING;
   FCI missing → `SL_FCI_MISSING` → status `ON_HOLD` until inspector decision (`fciOk`
   or reject); `liveWeightKg = gross − tare` must be > 0; DOA % > `sl.doa.alertPct` →
   WARNING to farm manager & plant manager (`SL_DOA_HIGH`); birds discrepancy vs
   declared > 1 % → `SL_COUNT_DISCREPANCY` → event to farm.
3. Lairage: `lairageEndAt − lairageStartAt ≥ sl.lairage.minMinutes` before `CLEARED`
   (`SL_LAIRAGE_TOO_SHORT`, inspector may clear earlier with documented welfare reason
   e.g. heat stress → audited override allowed **only** for this rule); > max → CRITICAL.
4. Ante-mortem: only `SLAUGHTER_INSPECTOR`; decision required before session start
   (`SL_ANTEMORTEM_REQUIRED`); `REJECTED` → reception `REJECTED` + event to farm;
   `LOGISTIC_SLAUGHTER` forces slot to end of day (plan validation).
5. Session: one `OPEN` session per line; `birdsSlaughtered ≤ countedBirds − doa −
   condemnedAM`; close requires post-mortem inspection, hot carcass weight, grading;
   yield < `sl.yield.minPct` → WARNING; CCP out of limits → CRITICAL to QA with
   corrective action mandatory; `productionLotCode` generated at open (date + plant +
   line + seq) and stamped on all outputs.
6. Grading/cuts: Σ grade A/B kg + cuts input ≤ cold carcass kg (tolerance 2 %)
   (`SL_MASS_BALANCE`); cut yield deviation > tolerance → INFO.
7. Packaging: best-before = `slaughterDate + sku.shelfLifeDays` (frozen: freezing
   date + frozen shelf life); QA hold blocks stock-in to dispatch dock and picking.
8. Cold chain: stock-in requires chamber `type` compatible with SKU storage; capacity
   exceeded → WARNING; readings out of range → WARNING; sustained > 30 min or > +7 °C →
   CRITICAL + automatic `TemperatureBreach` with affected units `BLOCKED` pending QA.
9. Picking FEFO by `bestBeforeDate` then `stockedInAt`; **expired units cannot be
   picked/dispatched** (`SL_BEST_BEFORE_PASSED`, no override); units expiring within
   `sl.dispatch.minRemainingShelfLifeDays` (customer-specific override on `Customer`) →
   WARNING; blocked/held units not pickable.
10. Dispatch approve (manager) requires all lines allocated, loading temp ≤ 4 °C (chill)
    / ≤ −18 (frozen) recorded, vehicle refrigerated for chilled/frozen; `DISPATCHED`
    emits event, deducts chamber stock, generates aviz PDF with lot codes & traceability
    (lot → farm → hatchery → flock); customer-specific label/docs language.
11. Invoice: issued from DISPATCHED orders of one customer; number from series (gapless,
    DB sequence per series, `sl.invoice.seriesPrefix`); VAT from `sl.vat.ratePct`
    (per SKU override); credit limit exceeded → WARNING at order confirm (`SALES` cannot
    override; manager can); cancellation only same day before export, else credit note.
12. Claims link to lots → traceability; a quality claim with `TEMPERATURE` type on a
    dispatch re-opens QA review of remaining units from the same production lot (INFO).
13. Session close publishes `session.closed` with per-lot findings (condemnation causes,
    DOA, yield) → farm stores feedback for next FCI; also `ppms.slaughterhouse.processing`
    for reporting.

## 5. API (`/api/v1`)
Sites §A (`/plants`, `/lines`, `/lairage-bays`, `/chambers`, `/skus`, `/vehicles`).
Planning: `GET /incoming-deliveries?date`, `GET/PUT /kill-plans?date`, `POST /kill-plans/{id}/publish`.
Reception: `POST /receptions` (from incoming or manual), `PUT /receptions/{id}`,
`POST …/weighings`, `/lairage/start`, `/lairage/end`, `/fci-check`, `GET /receptions?date&status`.
Inspection: `POST /receptions/{id}/ante-mortem`, `POST /sessions/{id}/post-mortem`,
`POST /receptions/{id}/hold` / `/release`.
Processing: `POST /sessions` (open), `PUT /sessions/{id}`, `POST …/ccp`, `/grading`,
`/cuts`, `/by-products`, `/packaging-runs`, `/close`, `GET /sessions?date&line`,
`GET /sessions/{id}` (full sheet + KPIs + traceability).
Cold chain: `POST /stock-units/stock-in`, `/stock-units/{id}/move`, `/block`, `/unblock`,
`/dispose`, `GET /stock?chamberId&skuId&lot&expiringDays`, `GET/POST /chambers/{id}/readings`,
`GET/POST /temperature-breaches` + `/decision`, `/qa-holds`, `/micro-samplings`.
Sales: CRUD `/customers`, `/price-lists`, `GET /pricing/resolve?customerId&skuId&date`,
`GET/POST /sales-orders`, `POST …/confirm`, `/cancel`, `POST /pick-lists` (from orders,
FEFO proposal) `PUT …/picked`, `GET/POST /dispatch-orders`, `POST …/approve`, `/dispatch`,
`/delivered`, `GET …/dispatch-note.pdf`, `POST /invoices` (from dispatch orders),
`GET /invoices`, `POST /invoices/{id}/cancel`, `POST /credit-notes`, `GET /invoices/{id}/export?format=csv|ubl-json`,
`/claims`. Traceability: `GET /trace/production-lot/{code}` (local chain) — full
chain in reporting-service. KPIs: `GET /kpi/sessions?from&to&groupBy=day|farm|lot`.
Inventory §C, costing §D (`GET /sessions/{id}/cost`), lookups §B, documents, exports.

## 6. Events
Published: `ppms.slaughterhouse.reception` (`delivery.confirmed {orderId, slot,
rescheduled}`, `received {orderId, receptionNo, birds, doa, liveWeightKg, arrivedAt}`,
`rejected`, `discrepancy`), `ppms.slaughterhouse.processing` (`session.opened`,
`session.closed {sessionNo, productionLotCode, lotCode, farmId, birds, liveKg, hotKg,
yieldPct, condemnations[], doaPct}`, `grading.recorded`, `cuts.recorded`,
`ccp.breach`), `ppms.slaughterhouse.cold-chain` (`stock.in/out`, `temperature.breach`,
`qa.hold/release`), `ppms.slaughterhouse.sales` (`sales-order.confirmed`,
`dispatch.confirmed {dispatchNo, customerId, lines[{sku, kg, productionLotCode,
bestBefore}], traceability}`, `invoice.issued {totals}`, `claim.opened`),
`ppms.inventory`, `ppms.alerts`, `ppms.audit`, `ppms.site.directory`, costing.
Consumed: `ppms.farm.delivery.planned/dispatched/cancelled/fci.issued` (requests, FCI
document refs, traceability), `ppms.farm.lot.closed` (cost of live birds if internal
pricing uses farm cost), `ppms.admin.*`, `ppms.site.directory` (farms).

## 7. Jobs
Every 5 min: chamber breach evaluation; daily 05:00: expiring stock alerts (`sl.expiry.warningDays`
default 2), expired units auto `BLOCKED`, micro sampling schedule reminder (weekly
neck skins), kill plan reminder; nightly: invoice export batch (CSV) to MinIO.

## 8. Migrations `VSL001…VSL016`.

## 9. Tests
Unit: yield & mass balance, best-before, FEFO allocation, lairage/transport rules, VAT
and invoice totals (rounding 2 dp, per-line), number series gapless under concurrency.
IT: farm `delivery.planned` → plan publish → confirmed event → reception → AM inspection →
session → PM → grading → packaging → stock-in → sales order → pick → dispatch (expired
unit refused) → invoice → events; `session.closed` consumed by farm (cross IT in farm);
role matrix (line supervisor records inspection → 403).

## 10. Task list
| # | Feature | Acceptance |
|---|---|---|
| S0 | Bootstrap, §A plant/lines/bays/chambers/SKUs, §B read models, incoming consumer | — |
| S1 | Kill plan + confirmations | IT event to farm |
| S2 | Reception, weighings, lairage, FCI check, DOA/discrepancy alerts & events | IT |
| S3 | Ante-mortem (inspector), holds, decisions | IT 403/422 |
| S4 | Sessions, CCP, post-mortem, grading, cuts, by-products, close + KPIs + feedback event | IT mass balance |
| S5 | Packaging runs, best-before, stock units, cold chain movements, readings, breaches, QA holds, micro samplings | IT |
| S6 | Customers, price lists, sales orders, FEFO pick lists | IT |
| S7 | Dispatch orders, approval rules, aviz PDF with traceability, events | IT expired refused |
| S8 | Invoices (series, VAT), credit notes, exports (CSV/UBL-json), claims | unit totals |
| S9 | Inventory §C, costing §D (cost/kg carcass) | IT |
| S10 | KPIs, exports, metrics (`ppms_sl_cold_store_occupancy{chamber}`) | — |

## Exit criteria
End-to-end chain demo complete: dispatch note of a customer order traces back to parent
flock codes; invoice export file produced; farm receives post-mortem feedback.
