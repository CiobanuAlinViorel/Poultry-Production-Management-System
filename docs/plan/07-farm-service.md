# 07 — farm-service (fermă de creștere pui de carne)

Port 8083 · DB `ppms_farm` · schema `farm` · package `com.ppms.farm` · migrations
prefix `VFA`. Reuses common blocks §A–§E from `05-parent-flock-service.md`. This is
the most feature-rich service and the first module the frontend ships.

---

## 1. Business process (broiler, Ross 308 / Cobb 500, 35–42 days)
1. **House preparation** (vid sanitar): removal of litter, washing, disinfection,
   Salmonella swabs (negative required), new litter, pre-heating 24–48 h (floor 28–30 °C,
   air 32–34 °C), feeders/drinkers check → house `READY`. Minimum downtime 10–14 days.
2. **Placement**: `ChickTransferOrder` announced; truck arrives; count boxes, sample
   count per box, DOA, weak; chick weight sample (≥ 40 g); **Lot** created
   (code `F1-H03-2026-10-16`); density planned (birds/m² from target kg/m² and target
   weight); water/feed availability check; chick paper; first 7 days critical
   (7-day mortality < 1 %, 7-day weight ≥ 4.5× placement weight).
3. **Daily sheet** per house: mortality (dead) + culls (leg problems, runts) with
   causes; feed delivered to house (kg, by feed phase; silo deliveries vs consumption),
   water (meter L), min/max temperature, RH, NH₃ (ppm), CO₂, light hours, litter
   condition score, ventilation notes, who was on duty, observations (behaviour,
   coughing, wet litter). System derives: live count, daily mortality %, cumulative %,
   feed g/bird, water ml/bird, water:feed ratio, alerts.
4. **Weekly** (or more often with automatic scales): weight sample ≥ 50–100 birds
   (sample size setting), uniformity CV %, ADG, FCR, EPEF vs breed standard ±
   tolerance; density forecast.
5. **Veterinary**: visits, diagnoses, treatments (antibiotics via water, antiparasitics,
   vitamins), vaccinations (drinking water: IB/ND/IBD/Gumboro), coccidiostat in feed
   (feed type carries withdrawal), lab samplings (serology, bacteriology, NCP
   Salmonella 3 weeks pre-slaughter — **mandatory before delivery**, result must be
   recorded), necropsy notes; every treatment has **withdrawal end date**; delivery
   blocked while any withdrawal active (zero override).
6. **Thinning / final delivery**: delivery eligibility (weight ≥ target, no withdrawal,
   NCP result recorded), `FarmDeliveryOrder` → slaughterhouse confirms date/slot →
   **feed withdrawal** 8–12 h before catching (water on) → catching crew & modules →
   truck weigh (farm scale, optional) → **FCI document** (Food Chain Information: farm,
   lot, placement date, count, mortality, treatments/withdrawals, diseases, lab results,
   previous slaughter findings, vet contact) sent ≥ 24 h before → dispatch with aviz +
   movement document; slaughterhouse reception feedback (received count, DOA, live
   weight, condemnations) closes order and feeds lot KPIs.
7. **Lot close**: all birds out (or manual close with reason), final KPIs (liveability,
   avg weight, FCR, EPEF, kg/m², cost/kg), house → `CLEANING`; sanitation record →
   `READY`.
Legal/standards: Directive 2007/43/EC (density ≤ 33 kg/m²; 39 with derogation, 42 with
low mortality history; records 3 years), Reg. 853/2004 FCI, Reg. 2160/2003 Salmonella
NCP, withdrawal per medicine SPC, treatment register (registru de tratamente).

## 2. Roles & permissions
| Role | Can |
|---|---|
| `FARM_MANAGER` | all at own farm(s); approve delivery orders; set lot targets; close lots; costing |
| `FARM_SUPERVISOR` | houses assigned: daily sheets (create/edit/submit), receptions, sanitation, weights, read all farm |
| `FARM_WORKER` | daily sheet for assigned houses (today / yesterday), read own houses |
| `FARM_VET` | treatments, vaccinations, visits, samplings, disease events, FCI vet section; read all |
| `FARM_WEIGHER` | weight samples only |
| `FARM_STOREKEEPER` | inventory |
| `MANAGER`/`ACCOUNTANT` | read, costing |
Permissions `fa:site:*`, `fa:lot:{read,write,close}`, `fa:daily-sheet:{read,write,submit,close}`,
`fa:treatment:*`, `fa:weight:write`, `fa:delivery:{read,write,approve,dispatch}`,
`fa:fci:{write,sign}`, `fa:sanitation:*`, `fa:inventory:*`, `fa:costing:*`.

## 3. Domain model (schema `farm`)

### 3.1 Structure
| Entity | Fields |
|---|---|
| `Farm` | §A (+ derogationDensityKgM2?, derogationValidUntil, salmonellaStatus) |
| `PoultryHouse` | farmId, code (`H03`), name, lengthM, widthM, areaM2, capacityBirds (computed from area × density setting / target weight, editable), ventilationType `NATURAL/TUNNEL/CROSS/COMBI`, heating, feederLines, drinkerLines, siloCount, status `EMPTY/READY/ACTIVE/CLEANING/MAINTENANCE`, currentLotId?, lastCleanedAt, lastSalmonellaResult |
| `Silo` | houseId, code, capacityKg, currentFeedTypeId?, estimatedLevelKg |
| `HouseAssignment` | houseId, employeeId, role hint, from, to (who may fill sheets; alt. use admin duty) |

### 3.2 Reception & lot
| Entity | Fields |
|---|---|
| `IncomingChickAnnouncement` | from `chick-transfer.dispatched`: orderId, orderNo, hatcheryId/code, destinationHouseId, lines, scheduledDate, status |
| `LotPlan` | farmId, houseId, plannedPlacementDate, plannedBirds, breedId, targetWeightKg, targetAgeDays, acceptGradeC, sourceHatcheryId?, status `PLANNED/FULFILLED/CANCELLED` (publishes `lot.planned` for hatchery) |
| `ChickReception` | receptionNo, farmId, houseId, chickTransferOrderId?, lotPlanId?, arrivedAt, vehicleReg, driverName, truckTempC, boxesDeclared, boxesCounted, declaredQty, countedQty, doaQty, weakQty, sampleBoxesCounted, avgChickWeightG, chickQualityScore, hatcheryCode, hatchDate, originFlockCodes[], breedCode, status `DRAFT/CONFIRMED/REJECTED`, confirmedBy, notes, photosDocRefs[] |
| `Lot` | lotCode (`<farmCode>-<houseCode>-YYYY-MM-DD`), farmId, houseId, receptionId, breedId, placementDate, ageDays (computed), initialBirds (= countedQty − doaQty), currentBirds (denormalised), cumMortality, cumCulls, birdsDelivered, targetWeightKg, targetAgeDays, acceptGradeC, status `ACTIVE/THINNING/DELIVERING/CLOSED`, closedAt, closeReason, finalKpis jsonb, originTraceability jsonb {hatcheryCode, chickBatchCodes, hatchDate, flockCodes, eggCollectionDates} |
| `LotCountAdjustment` | lotId, date, type `MORTALITY/CULL/DELIVERY/CORRECTION/TRANSFER`, qty, refType/refId |

### 3.3 Daily operations
| Entity | Fields |
|---|---|
| `DailySheet` | houseId, lotId, date (**unique house+date**), ageDays, status `DRAFT/SUBMITTED/CLOSED`, recordedBy, dutyEmployeeIds[], submittedAt, closedBy/At, tempMinC, tempMaxC, rhPct, nh3Ppm?, co2Ppm?, lightHours, litterScore 1–5, ventilationNote, observations, flockBehaviour `NORMAL/HUDDLING/PANTING/LETHARGIC` , notes |
| `MortalityEntry` | sheetId, kind `DEAD/CULLED`, count, cause `DISEASE/SUDDEN_DEATH_SDS/ASCITES/LEG_PROBLEMS/RUNTING/SUFFOCATION_PILING/CRUSHING/HEAT_STRESS/CANNIBALISM/UNKNOWN/OTHER`, disposal `RENDERING/INCINERATION/COMPOSTING/COLD_STORAGE_PENDING`, note |
| `FeedEntry` | sheetId, feedTypeCatalogId, siloId?, qtyKg, source `SILO_DELIVERY/BAGGED`, stockBatchId? |
| `FeedDelivery` | houseId/siloId, date, feedTypeId, qtyKg, supplierId, deliveryNoteNo, lotNo, cost (also creates StockBatch in inventory with location = silo) |
| `WaterEntry` | sheetId, litres, meterStart, meterEnd, medicatedWater (bool, treatmentId?) |
| `ClimateReading` | houseId, at, tempC, rhPct, nh3, co2, source `MANUAL/SENSOR` (fine-grained; daily sheet shows min/max) |
Derived per sheet: `liveBirdsEnd`, `dailyMortalityPct`, `cumMortalityPct`,
`feedPerBirdG`, `waterPerBirdMl`, `waterFeedRatio`, `cumFeedKg`, `estLiveWeightKg`
(from last weight sample + standard ADG interpolation), `densityKgM2` (est).

### 3.4 Weights & KPIs
| Entity | Fields |
|---|---|
| `WeightSample` | lotId, date, ageDays, sampleSize, totalWeightG, avgWeightG, minG, maxG, cvPct, uniformityPct, method `MANUAL/AUTO_SCALE`, weighedBy, scaleId? |
| `WeeklyPerformance` | lotId, week (1..), ageDaysEnd, liveBirds, cumMortalityPct, avgWeightG, adgG, weeklyFeedKg, cumFeedKg, fcr (cum), fcrAdjusted (to 2.0 kg: fcr − (avgWtKg−2.0)×0.3… configurable factor), waterFeedRatio, epef, densityKgM2, standard snapshot {weightG, fcr, mortality}, deviationPct, alerts[] |
| `LotKpiSnapshot` | lotId, date, computed daily for dashboard (currentBirds, cumMortality, estWeight, fcrToDate, epefToDate, daysToTarget) |

### 3.5 Veterinary
| Entity | Fields |
|---|---|
| `VetVisit` | farmId, lotIds[], date, vetId, findings, necropsyCount, necropsyFindings, recommendations, documentRef |
| `TreatmentRecord` | lotId, medicationCatalogId, stockBatchId, startDate, durationDays, endDate (computed), dosePerBirdMg or perLitre, route `DRINKING_WATER/FEED/SPRAY/INJECTION`, birdsTreated, totalQty, prescribingVetId, diagnosis, prescriptionNo, **withdrawalDays** (snapshot from catalogue), **withdrawalEndDate** = endDate + withdrawalDays, status `ACTIVE/COMPLETED/CANCELLED`, notes |
| `VaccinationEvent` | lotId, date, vaccineCatalogId, stockBatchId, route, doses, birds, performedBy, vetId |
| `DiseaseEvent` | lotId, date, diagnosis, suspected/confirmed, affected, labRef, notifiable (bool), resolvedAt |
| `LabSampling` | lotId, date, type `SALMONELLA_NCP/SEROLOGY/BACTERIOLOGY/WATER_QUALITY/FEED_ANALYSIS`, samples, lab, sentAt, result `PENDING/NEGATIVE/POSITIVE/INCONCLUSIVE`, resultDate, serovar?, documentRef |
| `FeedWithdrawalRecord` | lotId, deliveryOrderId, feedOffAt, waterOffAt?, performedBy |

### 3.6 Delivery
| Entity | Fields |
|---|---|
| `DeliveryEligibility` (computed, not stored… cached in `LotKpiSnapshot`) | eligible, reasons[] (`WEIGHT_BELOW_TARGET`, `WITHDRAWAL_ACTIVE until`, `NCP_RESULT_MISSING`, `NCP_POSITIVE`, `QUARANTINE`) |
| `FarmDeliveryOrder` | orderNo (`FDO-F1-2026-000542`), farmId, lotId, destinationSlaughterhouseId, type `THINNING/FINAL`, scheduledDate, scheduledSlot?, plannedBirds, plannedAvgWeightKg, plannedLiveWeightKg, status `PLANNED/APPROVED/CONFIRMED/FEED_WITHDRAWN/DISPATCHED/RECEIVED/REJECTED/CANCELLED`, confirmedAt (by slaughterhouse event), slaughterhouseSlotRef, catchingCrewId?, catchingStartAt, catchingEndAt, trucks [{vehicleReg, driverName, modules, crates, birdsLoaded, grossKg?, tareKg?, departureAt}], birdsDispatched, farmLiveWeightKg?, fciDocumentId, deliveryNoteDocId, movementDocNo, receivedBirds, doaAtSlaughter, slaughterLiveWeightKg, condemned, receivedAt, discrepancyNotes, approvedBy, dispatchedBy |
| `FoodChainInformation` | deliveryOrderId, generatedAt, data jsonb (farm & authorisation no., lot code, breed, placement date, hatchery, birds dispatched, age, mortality cum %, treatments [{product, dates, withdrawalEnd}], vaccinations, diseases, lab results (NCP), previous post-mortem findings for the farm (from slaughterhouse events), vet name & contact, feed types incl. medicated), signedByVetId, signedAt, documentRef, sentAt, status `DRAFT/SIGNED/SENT/ACKNOWLEDGED` |
| `CatchingCrew` | farmId or external supplier, name, members[], welfareCertificateNos |

### 3.7 Sanitation & downtime
| `SanitationRecord` | houseId, lotId (previous), litterRemovedAt, washedAt, disinfectedAt, disinfectantStockBatchId, qty, dryingDays, salmonellaSwabSamplingId, result, preHeatStartAt, readyAt, approvedBy → house `READY` |
| `HouseDowntime` (view) | houseId, from (lot close), to (next placement), days, vs `fa.downtime.minDays` |

### 3.8 Inventory & costing — §C (FEED, MEDICATION, VACCINE, LITTER, CLEANING,
DISINFECTANT, CONSUMABLE; feed deliveries create batches located in silos), §D (cost
center `Lot`: chicks (from CTO unitPrice or setting `fa.chickCostRon`), feed (deliveries ×
price), medication/vaccines (stock), litter, utilities (manual per house per period,
allocated by days), labour (duty hours × hourly cost), catching & transport (per
delivery), depreciation per house per day → **cost per kg live weight**, cost per bird).

## 4. Business rules (invariants)
1. `FA_HOUSE_NOT_READY`: reception only into `READY` house (EMPTY requires sanitation
   record; supervisor override flag `fa.allowPlacementWithoutSanitation=false`); downtime
   < `fa.downtime.minDays` → WARNING (not block).
2. `FA_CAPACITY_EXCEEDED`: `countedQty − doaQty ≤ capacityBirds`; planned density at
   target weight ≤ `fa.stocking.maxKgPerM2` (or farm derogation) → otherwise 422.
3. Lot code unique; `currentBirds = initial − Σ mortality − Σ culls − Σ delivered ±
   corrections`, maintained via `LotCountAdjustment` on sheet **submit**; negative → 422.
4. Daily sheet: one per house per date; date within [placementDate, today]; workers
   today/yesterday only; supervisor backfill `fa.sheet.backfillDays` (7); CLOSED immutable
   (auto-close 48 h); edits create compensating adjustments; a submitted sheet with
   mortality > `fa.mortality.dailyAlertPct` → WARNING; > 2× → CRITICAL (vet + manager);
   first-7-days cum mortality > 1.5 % → WARNING.
5. Water:feed ratio outside [`fa.water.feedRatioMin`, `Max`] (after day 3) → WARNING;
   water drop > 20 % day-over-day → CRITICAL (possible leak/illness/line failure).
6. Feed entry with `stockBatchId` consumes inventory FEFO per silo; medicated feed
   (coccidiostat/withdrawal) automatically creates/extends a **feed withdrawal
   constraint** on the lot (`withdrawalEndDate = lastFeedDate + feedType.withdrawalDays`).
7. **Treatment**: only `FARM_VET` (`@PreAuthorize` at service); stock FEFO consume
   (`STOCK_INSUFFICIENT` → 422 block); `withdrawalEndDate` computed from catalogue
   snapshot; publishes `treatment.recorded`; alert `FA_WITHDRAWAL_ENDING` at
   `fa.withdrawal.warningHours` before end (INFO) and `FA_WITHDRAWAL_ACTIVE_ON_DELIVERY_ATTEMPT`
   (CRITICAL) when a delivery is attempted during withdrawal.
8. **Delivery hard block (NFR-C-01)**: `DeliveryPolicy.assertDeliverable(lot, date)` in
   the domain layer checks: no `TreatmentRecord` or feed-withdrawal with
   `withdrawalEndDate ≥ scheduledDate` (`FARM_WITHDRAWAL_ACTIVE`, lists products & dates);
   no quarantine/notifiable disease open (`FARM_QUARANTINE`); NCP Salmonella sampling
   result recorded and not POSITIVE within the lot (`FARM_NCP_MISSING` / `FARM_NCP_POSITIVE`
   — positive requires DSVSA decision flag `ncpPositiveSlaughterAuthorised` set by vet
   with document → then allowed with logistic slaughter note in FCI). Checked on
   `create`, `approve`, `dispatch` and re-checked when `scheduledDate` changes. **No
   role can bypass**; there is no override field in the API; ArchUnit test asserts
   `DeliveryPolicy` is called by every path that transitions to APPROVED/DISPATCHED.
9. Weight below target → eligibility `false` but order still **creatable** for THINNING
   type (thinning intentionally takes lighter birds) — only FINAL requires weight ≥
   target unless manager sets `acceptBelowTargetReason`.
10. Delivery lifecycle: `PLANNED` (anyone with `fa:delivery:write`) → `APPROVED`
    (FARM_MANAGER; publishes `delivery.planned` → slaughterhouse) → `CONFIRMED` (from
    slaughterhouse event with slot) → `FEED_WITHDRAWN` (feed-off time recorded; must be ≥
    `fa.feedWithdrawal.hoursBeforeCatching` before catching start else WARNING) →
    `DISPATCHED` (trucks, birds loaded; requires FCI `SENT` else `FA_FCI_NOT_SENT`;
    generates aviz PDF; `LotCountAdjustment DELIVERY`; publishes `delivery.dispatched`) →
    `RECEIVED` (slaughterhouse event with counts/weights → lot gets realised avg weight
    and `doaAtSlaughter`; discrepancy > 1 % birds → WARNING).
11. FCI: generated from data (`generate`), vet reviews/edits allowed fields, `sign`
    (FARM_VET), `send` (event `fci.issued` with document ref; email to slaughterhouse
    contact via notification); must be SENT ≥ `fa.fci.minHoursBeforeArrival` before
    `scheduledDate` → otherwise WARNING (block configurable `fa.fci.hardBlock=true`).
12. Lot close: allowed when `currentBirds == 0` or manual with reason (remaining birds
    recorded as CULL/CORRECTION); computes final KPIs; house → `CLEANING`; publishes
    `lot.closed`.
13. Thinning: after a THINNING delivery, lot status `THINNING`; density recalculated.
14. KPI standards: deviation beyond `fa.kpi.tolerancePct` → WARNING (`FA_KPI_DEVIATION`
    with metric); EPEF < 300 at close → INFO to MANAGER.
15. Sanitation requires Salmonella swab result NEGATIVE to set `READY` (`fa.requireSwabBeforePlacement=true`).

## 5. API (`/api/v1`)
Sites §A (`/farms`, `/farms/{id}/houses`, `/houses/{id}/silos`, `/houses/{id}/assignments`).
Planning/reception: `GET /incoming-announcements?farmId`, `GET/POST /lot-plans`,
`POST /chick-receptions` (from announcement or manual), `PUT /chick-receptions/{id}`,
`POST …/confirm` (creates Lot, house ACTIVE, emits reception event), `POST …/reject`.
Lots: `GET /lots?farmId&status&houseId`, `GET /lots/{id}` (header + today KPIs +
eligibility), `PATCH /lots/{id}/targets`, `POST /lots/{id}/count-adjustments`,
`POST /lots/{id}/close`, `GET /lots/{id}/timeline` (all events), `GET /lots/{id}/traceability`.
Daily: `GET /houses/{id}/daily-sheets?from&to`, `GET /daily-sheets/by-date?houseId&date`
(get-or-create DRAFT, pre-filled with yesterday's feed type, duty employees),
`PUT /daily-sheets/{id}` (sections: mortality[], feed[], water, climate, observations),
`POST …/submit`, `/close`, `/reopen`; `POST /feed-deliveries`; `POST /houses/{id}/climate-readings`
(sensor bulk); `GET /farms/{id}/today` (board: every house → sheet status, live birds,
age, alerts).
Weights/KPIs: `POST /lots/{id}/weight-samples` (manual or bulk from scale), `GET /lots/{id}/weekly`,
`GET /lots/{id}/kpi-history`, `GET /kpi/farm?farmId&from&to` (per lot comparison),
`GET /lots/{id}/standard-curve`.
Vet: `POST /vet-visits`, `GET/POST /lots/{id}/treatments`, `POST /treatments/{id}/cancel`,
`GET /lots/{id}/withdrawals` (active constraints), `POST /lots/{id}/vaccinations`,
`/disease-events`, `/lab-samplings` + `PATCH …/result`, `GET /farms/{id}/treatment-register?from&to`
(legal register, PDF/CSV).
Delivery: `GET /lots/{id}/eligibility`, `GET/POST /delivery-orders`, `GET /delivery-orders/{id}`,
`POST …/approve`, `/feed-withdrawal`, `/dispatch` `{trucks[], birdsDispatched, farmLiveWeightKg}`,
`/cancel`, `GET …/delivery-note.pdf`, FCI: `POST /delivery-orders/{id}/fci/generate`,
`PUT …/fci`, `POST …/fci/sign`, `/fci/send`, `GET …/fci.pdf`; `GET/POST /catching-crews`.
Sanitation: `GET/POST /houses/{id}/sanitation`, `POST /sanitation/{id}/ready`.
Inventory §C, costing §D (`GET /lots/{id}/cost` → cost/kg live), lookups §B, documents,
exports (`/lots/export`, `/daily-sheets/export`, `/treatment-register/export`).

## 6. Events
Published: `ppms.farm.chick-reception` (`received {orderId, countedQty, doaQty, weakQty,
lotCode}`, `rejected`), `ppms.farm.lot` (`lot.planned {houseId, acceptGradeC,
plannedDate, plannedBirds}`, `lot.created {lotCode, farmId, houseId, initialBirds,
breedCode, origin…}`, `lot.kpi.computed` (daily snapshot), `lot.thinned`, `lot.closed
{finalKpis}`), `ppms.farm.daily-ops` (`daily-sheet.submitted {…totals}`,
`treatment.recorded {lotId, product, withdrawalEndDate}`, `withdrawal.ending`,
`weight.recorded`), `ppms.farm.delivery` (`planned {orderNo, lotCode, destination,
scheduledDate, plannedBirds, plannedAvgWeight, type}`, `dispatched {trucks, birds,
farmLiveWeightKg, fciDocRef, movementDocNo, traceability{lotCode, hatcheryCode,
chickBatchCodes, flockCodes}}`, `cancelled`, `fci.issued`), `ppms.inventory`,
`ppms.alerts`, `ppms.audit`, `ppms.site.directory` (farm + houses), costing.
Consumed: `ppms.hatchery.chick-transfer.dispatched/cancelled` (announcements),
`ppms.slaughterhouse.reception` (`delivery.confirmed {orderId, slot}`, `received
{orderId, birds, doa, liveWeightKg}`, `rejected`), `ppms.slaughterhouse.processing`
(`session.closed` → lot realised yield, condemnation findings stored in
`LotSlaughterFeedback` for next FCI "previous findings"), `ppms.admin.*`,
`ppms.site.directory` (hatcheries, slaughterhouses).

## 7. Jobs
Daily 00:15: `LotKpiSnapshot` for active lots, `ageDays`, standards comparison, eligibility
recompute, withdrawal-ending alerts, expiring stock, NCP sampling due reminder (lot age ≥
targetAge − 21 and no sampling → WARNING to vet), auto-close sheets. Hourly: climate
sensor breach evaluation, water drop detection (if sensor meters). Monday: weekly
performance rows.

## 8. Migrations `VFA001…VFA014` (base, sites, planning/reception, lots, daily, climate,
weights/kpi, vet, delivery, fci, sanitation, inventory, costing, read models/indexes).

## 9. Tests
Unit: every derived KPI formula (table-driven with standard examples), `DeliveryPolicy`
exhaustive (active treatment / ends same day / ended yesterday / feed withdrawal / NCP
states), count adjustments, FEFO, state machines, FCI data assembly. IT: full lot
lifecycle with `TestClock` (placement → 42 days of sheets generated by test helper →
weights → treatment → delivery blocked (422 code) → after withdrawal → approve →
confirmed event → FCI sign/send → dispatch → received event → close → house CLEANING);
role matrix (worker edits other house → 403; weigher creates treatment → 403);
ArchUnit `DeliveryPolicy` coverage rule.

## 10. Task list
| # | Feature | Acceptance |
|---|---|---|
| F0 | Bootstrap, §A farms/houses/silos/assignments, §B read models, announcement consumer | directory event |
| F1 | LotPlan, ChickReception, Lot creation, count adjustments, traceability snapshot, events | IT reception→lot |
| F2 | DailySheet + entries, get-or-create, submit/close, derived KPIs, alerts (rules 4–5), feed deliveries → inventory, climate readings | worker flow IT |
| F3 | Weight samples, WeeklyPerformance, LotKpiSnapshot job, standards comparison alerts | formula unit tests |
| F4 | Vet: visits, treatments (FEFO + withdrawal), vaccinations, disease, lab samplings, treatment register PDF/CSV | vet-only IT |
| F5 | DeliveryPolicy + eligibility endpoint + ArchUnit guard | exhaustive unit tests |
| F6 | FarmDeliveryOrder lifecycle, catching crews, feed withdrawal, trucks, aviz PDF, events, slaughterhouse consumers | end-to-end IT |
| F7 | FCI generation/sign/send + PDF + email event | IT; PDF contains treatments |
| F8 | Lot close + final KPIs + house CLEANING; Sanitation + READY; downtime view | IT |
| F9 | Inventory §C (silo-located feed) | IT |
| F10 | Costing §D (cost/kg live weight) | IT |
| F11 | Farm today board, KPI comparison endpoints, exports, metrics (`ppms_farm_live_birds{farm,house}`) | — |

## Exit criteria
Complete broiler cycle demo via API with hard-block proven by test; frontend Farm module
(10-frontend §6) can be built against this API without gaps.
