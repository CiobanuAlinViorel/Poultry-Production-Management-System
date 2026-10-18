# 04 — admin-service (authorisation, workforce, configuration)

Port 8085 · DB `ppms_admin` · schema `admin` · package `com.ppms.admin` (service is an
empty stub today, so the DDD package layout applies) · migrations prefix `VAD`.

---

## 1. Responsibilities
1. **Authorisation model**: roles, permissions, scoped user-role assignments; the
   `effective-access` contract for identity-service.
2. **Site directory**: read model of every operational unit registered by the domain
   services (so scoped assignments can show names, and the SPA can list sites).
3. **Workforce (HR-light)**: employees, positions, teams, shift templates, rosters,
   duty assignments. Domain services reference `employeeId`.
4. **System settings**: typed key/value configuration, global or per site, with history.
5. **Feature flags** and **reference catalogues** shared company-wide (breeds/genetics,
   feed phases vocabulary, units, suppliers, medication catalogue with withdrawal days,
   vaccine catalogue). Domain services keep local read models fed by events.

## 2. Roles
`SYSTEM_ADMIN` (everything), `HR_ADMIN` (employees, rosters, no roles/permissions),
`MANAGER` (read-only everywhere), site supervisors/managers (`FARM_MANAGER`,
`HATCHERY_SUPERVISOR`, `FLOCK_SUPERVISOR`, `SLAUGHTER_MANAGER`) may **view** their site's
assignments and **edit rosters** for their own site.

## 3. Domain model (schema `admin`)

### 3.1 Authorisation
| Entity | Fields | Notes |
|---|---|---|
| `Role` | code (unique, e.g. `FARM_VET`), nameRo, nameEn, description, module (`SYSTEM/PARENT_FLOCK/HATCHERY/FARM/SLAUGHTERHOUSE`), allowedScopeTypes (set), isSystemDefined, active | seeded list in 00 §3.4 |
| `Permission` | key (unique `farm:treatment:write`), module, description, isSystemDefined | seeded per service file |
| `RolePermission` | roleId, permissionId | join |
| `UserRoleAssignment` | userId, roleId, scopeType, scopeId?, assignedBy, assignedAt, validFrom?, validTo?, revokedAt?, revokedBy?, reason | `GLOBAL` ⇒ scopeId null; else required and must exist in SiteDirectory with matching type |
| `RoleAuditLog` | via shared `@Audited` | who granted what |

### 3.2 Site directory (read model, fed by `ppms.site.directory`)
| `SiteDirectoryEntry` | siteId (PK), siteType (`BREEDER_SITE/HATCHERY/FARM/SLAUGHTERHOUSE`), code, name, address, county, active, sourceService, lastEventAt |

Plus `SiteSubUnit` (houseId/incubatorId/lineId, siteId, type, code, name) for roster
assignment to a house.

### 3.3 Workforce
| Entity | Fields |
|---|---|
| `Employee` | employeeNo (unique), firstName, lastName, cnpHash (SHA-256 of CNP, never raw), phone, email, hireDate, terminationDate?, status `ACTIVE/ON_LEAVE/TERMINATED`, positionId, homeSiteId, homeSiteType, userId? (link to identity), photoUrl |
| `Position` | code, nameRo, nameEn, module, defaultRoleIds |
| `Team` | siteId, name, leaderEmployeeId |
| `TeamMember` | teamId, employeeId, from, to |
| `ShiftTemplate` | siteId?, code (`DAY/NIGHT/MORNING`), startTime, endTime, breakMinutes, color |
| `Roster` | siteId, weekStart (Monday), status `DRAFT/PUBLISHED`, publishedBy/At |
| `DutyAssignment` | rosterId, employeeId, date, shiftTemplateId, subUnitId? (house), role hint, note |
| `Absence` | employeeId, from, to, type `VACATION/SICK/UNPAID/OTHER`, approvedBy |
| `Certification` | employeeId, type (`VET_LICENCE`, `WELFARE_CERT_1099_2009`, `FORKLIFT`, `HACCP`), number, issuedAt, expiresAt, documentRef |

### 3.4 Settings & catalogues
| Entity | Fields |
|---|---|
| `SettingDefinition` | key (unique), module, type (`INT/DECIMAL/BOOL/STRING/ENUM/JSON`), unit, defaultValue, min/max, allowedValues, descriptionRo/En, scopeable (bool) |
| `SettingValue` | definitionKey, scopeType, scopeId?, value (text), effectiveFrom, changedBy, changedAt | history kept (new row per change; current = latest effectiveFrom ≤ now) |
| `FeatureFlag` | key, enabled, scopeType?, scopeId?, rollout note |
| `Breed` | code (`ROSS_308`, `COBB_500`, `ROSS_308_PS`), name, kind `BROILER/PARENT_STOCK`, active |
| `PerformanceStandard` | breedId, kind, ageDays (or ageWeeks for PS), sex `AS_HATCHED/MALE/FEMALE`, bodyWeightG, dailyGainG, cumFeedG, fcr, mortalityCumPct?, layingRatePct?, hatchingEggPct?, settableEggPct? | uploaded from breed company tables (CSV import) |
| `Supplier` | code, name, vatNo, address, contact, categories (`FEED/MEDICATION/VACCINE/LITTER/CLEANING/EGGS/CHICKS/PACKAGING/TRANSPORT`), active |
| `MedicationCatalogItem` | code, name, activeSubstance, form, unit, defaultDosePerKg, route, **withdrawalDaysMeat**, withdrawalDaysEggs, prescriptionRequired, atcVetCode |
| `VaccineCatalogItem` | code, name, disease, route (`DRINKING_WATER/SPRAY/EYE_DROP/SC/IM/IN_OVO`), manufacturer |
| `FeedTypeCatalogItem` | code, name, phase (`PRE_STARTER/STARTER/GROWER/FINISHER/WITHDRAWAL/BREEDER_REARING/BREEDER_LAYER_1/…`), form, meKcalKg, cpPct, medicated (coccidiostat?), withdrawalDays |
| `UnitOfMeasure` | code, name, kind |
| `Customer`? — **no**, customers belong to slaughterhouse-service. |

## 4. Business rules
- Assignment validity: `GLOBAL` only for roles whose `allowedScopeTypes` contains GLOBAL
  (`SYSTEM_ADMIN`, `MANAGER`, `HR_ADMIN`, `ACCOUNTANT`, `SALES`); site roles require a
  `scopeId` that exists in `SiteDirectoryEntry` with the matching `siteType`.
- Cannot delete a role with active assignments; cannot delete system-defined roles or
  permissions (deactivate only).
- Effective access = union of non-revoked, in-validity assignments; permissions = union of
  role permissions. `MANAGER` yields read permissions (`*:read`) for all scopes;
  `SYSTEM_ADMIN` yields admin permissions but **not** operational write permissions
  (keeps NFR "no operational write access"; an admin wanting to write must hold the
  operational role explicitly).
- Assignments expire via `validTo` (job marks expired → event); last `SYSTEM_ADMIN`
  GLOBAL assignment cannot be revoked.
- Employee: `cnp` accepted on create, validated (13 digits + checksum), stored only as
  hash + last 4 chars for display; terminated employee → `employee.deactivated` event
  (identity disables user; rosters future rows removed).
- Roster: one `DutyAssignment` per employee per date per shift; publishing a roster
  emits `roster.published` (notification to employees with accounts); absence overlapping
  a duty → warning, not block.
- Certification expiring in 30 days → alert `HR_CERTIFICATION_EXPIRING` (WARNING) to
  HR_ADMIN and site manager; a `FARM_VET` assignment requires a valid `VET_LICENCE`
  certification on the linked employee (enforced at assignment time, configurable flag
  `admin.requireVetLicence`).
- Settings: value validated against definition (type/min/max/enum); write emits
  `setting.changed` (shared-kernel caches invalidate); `MANAGER` cannot write.
- Performance standards import: CSV with header validation; a breed must have a complete
  day 0–56 (broiler) or week 0–65 (PS) table to be `active`.

## 5. API (`/api/v1`)

Authorisation: `GET/POST /roles`, `GET/PUT/DELETE /roles/{id}`, `GET /permissions`,
`PUT /roles/{id}/permissions` (full set replace), `GET /user-role-assignments?userId|scopeType|scopeId|role`,
`POST /user-role-assignments` (single or bulk array), `DELETE /user-role-assignments/{id}`
(`{reason}`), `GET /users/{userId}/access` (assignments + effective perms, for the admin UI),
`GET /sites?type&active&q` (directory), `GET /sites/{id}/sub-units`.
Internal: `GET /internal/users/{userId}/effective-access → {roles:[…], perms:[…]}`,
`GET /internal/sites/{id}`.

Workforce: `GET/POST /employees`, `GET/PATCH /employees/{id}`, `POST /employees/{id}/terminate`,
`POST /employees/{id}/link-user`, `GET /employees/{id}/certifications` + CRUD,
`GET /employees/{id}/absences` + CRUD, `GET/POST /positions`, `GET/POST /teams`,
`GET/POST /shift-templates`, `GET /rosters?siteId&weekStart`, `POST /rosters`,
`PUT /rosters/{id}/assignments` (bulk upsert), `POST /rosters/{id}/publish`,
`POST /rosters/{id}/copy-from-previous-week`, `GET /duty?siteId&date` (who is on duty —
used by domain services' UI to pre-fill "recorded by/with"), `GET /employees/export?format`.

Settings & catalogues: `GET /settings/definitions?module`, `GET /settings?scopeType&scopeId`
(merged effective view), `PUT /settings/{key}` `{scopeType, scopeId?, value, reason}`,
`GET /settings/{key}/history`, `GET/PUT /feature-flags`, CRUD `/breeds`,
`/breeds/{id}/standards` + `POST /breeds/{id}/standards/import` (CSV), CRUD `/suppliers`,
`/catalog/medications`, `/catalog/vaccines`, `/catalog/feed-types`, `/units`.
All lists: paged, filterable, `/export`.

## 6. Events
Published: `ppms.admin.role-assignment` (`assigned/revoked/expired`),
`ppms.admin.employee` (`employee.created/updated/deactivated`, compacted, payload =
directory projection: id, employeeNo, name, position, homeSiteId, status),
`ppms.admin.settings` (`setting.changed {key, scopeType, scopeId}`),
`ppms.admin.catalog` (new topic: `breed.updated`, `standards.imported`,
`medication.updated`, `vaccine.updated`, `feed-type.updated`, `supplier.updated` —
compacted, key = item id) — domain services maintain local `*_catalog` read-model tables,
`ppms.admin.roster` (`roster.published`, `duty.changed`).
Consumed: `ppms.site.directory` (upsert directory), `ppms.identity.user`
(`user.created` with employeeId → link; `user.disabled`).

## 7. Settings catalogue (definitions seeded by this service; defaults used by others)

| Key | Type | Default | Scope |
|---|---|---|---|
| `identity.password.minLength` | INT | 10 | GLOBAL |
| `identity.lockout.threshold` / `autoUnlockMinutes` | INT | 5 / 30 | GLOBAL |
| `identity.mfa.requiredRoles` | JSON | `["SYSTEM_ADMIN","HR_ADMIN"]` | GLOBAL |
| `pf.flock.endOfLayWeeks` | INT | 64 | BREEDER_SITE |
| `pf.flock.endOfLayWarningWeeks` | INT | 60 | BREEDER_SITE |
| `pf.layingRate.minPct` | DECIMAL | 75 | BREEDER_SITE |
| `pf.egg.maxStorageDays` | INT | 7 (warn), 10 hard | BREEDER_SITE |
| `pf.egg.maxCrackPct` | DECIMAL | 3 | BREEDER_SITE |
| `pf.egg.minHatchingWeightG` | DECIMAL | 50 | BREEDER_SITE |
| `pf.eggStore.tempMin/Max` · `rhMin/Max` | DECIMAL | 15/18 · 75/80 | BREEDER_SITE |
| `pf.vaccination.dueWarningDays` | INT | 7 | BREEDER_SITE |
| `ha.setter.tempMin/Max` · `rhMin/Max` · `co2MaxPpm` | DECIMAL | 37.5/37.9 · 50/60 · 4000 | HATCHERY |
| `ha.hatcher.tempMin/Max` · `rhMin/Max` | DECIMAL | 36.7/37.3 · 60/75 | HATCHERY |
| `ha.candling.firstDay` / `transferDay` / `hatchDay` | INT | 10 / 18 / 21 | HATCHERY |
| `ha.hatchability.minPct` | DECIMAL | 80 | HATCHERY |
| `ha.fertility.minPct` | DECIMAL | 88 | HATCHERY |
| `ha.chick.maxAgeHoursAtDispatch` | INT | 24 | HATCHERY |
| `ha.chick.allowGradeC` | BOOL | false | HATCHERY |
| `fa.stocking.maxKgPerM2` | DECIMAL | 33 (39 w/ derogation flag) | FARM |
| `fa.downtime.minDays` | INT | 10 | FARM |
| `fa.water.feedRatioMin` / `Max` | DECIMAL | 1.6 / 2.2 | FARM |
| `fa.mortality.dailyAlertPct` | DECIMAL | 0.5 | FARM |
| `fa.kpi.tolerancePct` | DECIMAL | 10 | FARM |
| `fa.target.liveWeightKg` | DECIMAL | 2.4 | FARM (per lot override) |
| `fa.withdrawal.warningHours` | INT | 48 | FARM |
| `fa.fci.minHoursBeforeArrival` | INT | 24 | FARM |
| `fa.feedWithdrawal.hoursBeforeCatching` | INT | 8 | FARM |
| `fa.weighing.minSampleSize` | INT | 50 | FARM |
| `sl.lairage.minMinutes` / `maxMinutes` | INT | 60 / 720 | SLAUGHTERHOUSE |
| `sl.transport.maxHours` | INT | 12 | SLAUGHTERHOUSE |
| `sl.yield.minPct` | DECIMAL | 72 | SLAUGHTERHOUSE |
| `sl.doa.alertPct` | DECIMAL | 0.3 | SLAUGHTERHOUSE |
| `sl.chill.maxCoreTempC` | DECIMAL | 4 | SLAUGHTERHOUSE |
| `sl.coldStore.tempMin/Max` (fresh) · freezer | DECIMAL | -1/4 · -18 | SLAUGHTERHOUSE (per chamber override) |
| `sl.shelfLifeDays.<productCategory>` | JSON | `{FRESH_WHOLE:8, FRESH_CUTS:7, FROZEN:365, OFFAL:3}` | SLAUGHTERHOUSE |
| `sl.vat.ratePct` | DECIMAL | configurable (RO food rate) | GLOBAL |
| `sl.invoice.seriesPrefix` | STRING | `PPMS` | SLAUGHTERHOUSE |
| `inv.expiry.warningDays` | INT | 30 | all |
| `alerts.critical.emailEnabled` | BOOL | true | GLOBAL |
| `ui.defaultLocale` | ENUM | `ro` | GLOBAL |

## 8. Seeds (Flyway `R__seed_*.sql`)
Roles (00 §3.4) with allowed scope types; permissions (each service file lists its
own; seed all here); bootstrap `SYSTEM_ADMIN` GLOBAL assignment for the documented
admin UUID; setting definitions above; breeds `ROSS_308`, `COBB_500`, `ROSS_308_PS`
(standards tables imported via CSV in `resources/seed/standards/*.csv` — fill from
breed company objectives, mark as "indicative"); positions; shift templates
DAY 06:00–14:00, AFTERNOON 14:00–22:00, NIGHT 22:00–06:00; units.

## 9. Jobs
Assignment expiry (hourly), certification-expiry alerts (daily 06:00), roster reminder
(Thursday: next week's roster not published → alert site manager).

## 10. Tests
Unit: scope validation matrix, effective-access merge, CNP checksum, settings type
validation, standards completeness. IT: assignment CRUD → event; effective-access
shape for multi-role user; site-directory consumer upsert; employee terminate → event
→ (identity IT separately) ; roster publish; settings change → event; CSV import.

## 11. Task list

| # | Task | Acceptance |
|---|---|---|
| A0 | Bootstrap module: DDD packages, shared-kernel wiring, V001 (outbox/audit/processed), security, OpenAPI | app starts, health green |
| A1 | Role/Permission/RolePermission + seeds + API | IT |
| A2 | SiteDirectory consumer + `/sites` API | IT with published test event |
| A3 | UserRoleAssignment + rules + `/internal/users/{id}/effective-access` + events | IT; identity-service login gets real claims |
| A4 | SettingDefinition/Value + API + events + seeds | shared-kernel `SettingsClient` IT against it |
| A5 | Catalogues: Breed/PerformanceStandard (+CSV import), Supplier, Medication, Vaccine, FeedType, Units + events | IT; import rejects incomplete tables |
| A6 | Employees + Positions + certifications + absences + CNP handling + events | IT |
| A7 | Teams, ShiftTemplates, Rosters, DutyAssignments, publish, `/duty` | IT |
| A8 | Feature flags; MANAGER read-only enforcement; exports | IT |
| A9 | Jobs + alerts | unit with `TestClock` |

## Exit criteria
identity-service issues tokens with real roles/perms; SPA admin screens (users, roles,
assignments, employees, rosters, settings, catalogues) have all endpoints; domain
services can consume catalogue/employee/settings events.
