# Parent Flock Service — PF1 Foundation Implementation Slice

**Status:** Approved for implementation
**Parent documents:** `docs/parent-flock-architecture.md` (full implementation guide), `docs/parent-flock-service.doc.md` (full functional spec), `docs/Specs.MD` (system-wide spec)
**Scope of this slice:** shared-kernel (minimal) + parent-flock-service PF1 (Farm Site, House, Flock lifecycle) + JWT/role security + tests

This document does not restate what the parent documents already specify in
full detail (entity fields, business rules, endpoint list). It records only
the decisions specific to *this implementation pass*: what's in scope, what's
deliberately deferred, and the mapping choices needed to start writing code.

---

## 1. Why a slice, not the whole spec

The full parent-flock-service spec covers ~40 entities and 15 service
classes (PF1–PF9). The repository currently has **no implementation** at
all — `shared-kernel` is an empty Maven module and `parent-flock-service` is
an unmodified Spring Initializr skeleton. Building everything in one pass
isn't practical. PF1 (Flock & House Management) is the foundation every
other feature (`flockId` is a required field almost everywhere else in the
spec) depends on, so it goes first, built completely (entity → repository →
service → DTO/mapper → controller → tests) rather than partially.

## 2. Layer mapping

| Requested layer | Package | Contents |
|---|---|---|
| Entity | `domain.model`, `domain.enums` | `@Entity` classes with calculated fields as methods (e.g. age-in-weeks); enums |
| Repository | `domain.repository` | `JpaRepository` interfaces, Spring Data derived queries + one custom `@Query` |
| Service | `application.service` | `@Transactional` business logic, one class per aggregate root, `@PreAuthorize` |
| Application | `application.dto.{request,response}`, `application.mapper` | DTOs + MapStruct mappers — the HTTP↔domain boundary |

Supporting packages needed for a runnable app: `api.controller` (thin),
`api.exception` (`@ControllerAdvice`), `config` (Security/JPA/OpenAPI),
`infrastructure.persistence` (`FlockSpecification` for the filterable list
query on `GET /flocks`).

## 3. shared-kernel — in scope for this pass

Per `docs/parent-flock-architecture.md` §2.3, minus anything not needed to
compile/run PF1 + auth:

- `persistence.BaseEntity`
- `exception.DomainException`, `EntityNotFoundException`,
  `BusinessRuleViolationException`, `ValidationException`,
  `WithdrawalPeriodActiveException` (unused by PF1 today, included because
  it's part of the documented exception hierarchy and cheap to add)
- `web.ApiErrorResponse`, `FieldValidationError`, `PageResponse<T>`
- `domain.enums.ServiceModule`, `domain.enums.RoleScope`
- `domain.model.User`, `Role`, `UserRoleAssignment`
- `security.JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtProperties`,
  `UserPrincipal`

**Explicitly out of scope for this pass:** `Employee`, `AuditLog`, `Alert`,
`Notification`, `AlertAcknowledgement`, the `@Audited` AOP interceptor.
None of these are required for PF1 or for auth to function. Adding them now
would be scope creep — they belong to a later pass once a feature actually
needs them (e.g. `AuditLog` when write-auditing is implemented,
`Alert`/`Notification` when PF1-C-FR-02/04 style alerting is built).

### Auth, since there's no auth-server in the architecture

The system-wide spec assumes JWTs exist but assigns no service the job of
issuing them. Since PF1 needs to be actually callable end-to-end, this pass
adds a minimal `AuthController` (`POST /api/v1/auth/login`) + `AuthService`
in parent-flock-service, backed by the `User`/`Role`/`UserRoleAssignment`
tables, plus a Flyway seed migration creating one dev user per PF1-relevant
role (`FLOCK_SUPERVISOR`, `FLOCK_WORKER`, `FLOCK_VET`, `MANAGER`).

`@PreAuthorize` checks role only (`hasRole('FLOCK_SUPERVISOR')`), matching
the role-only examples already given in the architecture doc's service
inventory. `UserRoleAssignment.scopeEntityId` (per-farm-site/per-house
scoping) is modeled in the entity but **not enforced** — deferred until a
feature needs it.

## 4. parent-flock-service — PF1 domain scope

**Entities** (`domain.model`): `BreederFarmSite`, `ParentFlockHouse`,
`HouseSection`, `ParentFlock`, `FlockBirdCountEvent`, `RearingWeightSample`,
`RearingToProductionTransfer`, `TransferDestination`, `FlockCullingRecord`.

**Enums** (`domain.enums`): `HouseType`, `HouseStatus`, `HouseSectionType`,
`VentilationType` (`NATURAL`/`TUNNEL`/`FORCED` — not enumerated in the
architecture doc's table but referenced as a field type; using the values
already established for the analogous Farm Service `PoultryHouse` entity in
`docs/Specs.MD` §7.3 for consistency), `FarmSiteStatus`, `FlockPhaseStatus`,
`FlockHealthStatus`, `FlockSourceType`, `BirdType`, `MortalityCause`,
`CullingReason`, `DisposalMethod`.

**Repositories**: one per aggregate, per the method signatures already
listed in `docs/parent-flock-architecture.md` §3.2 (repositories section) —
`BreederFarmSiteRepository`, `ParentFlockHouseRepository`,
`HouseSectionRepository`, `ParentFlockRepository`, `FlockBirdCountEventRepository`,
`RearingWeightSampleRepository`, `RearingToProductionTransferRepository`,
`FlockCullingRecordRepository`. The documented `findFlockWithActiveWithdrawal`
custom query on `ParentFlockRepository` joins against `HealthIntervention`,
which is a PF3 entity not built in this pass — that one method is **deferred
to the PF3 pass** along with the entity it depends on.

**Services** (`application.service`), method inventories exactly as
documented in §3.3:

- `FarmSiteService` — createFarmSite, updateFarmSiteStatus, getFarmSite, listFarmSites
- `HouseService` — registerHouse, addSection, changeHouseStatus, getHouseWithSections, listHouses
- `FlockService` — registerFlock, recordBirdCountEvent, recordRearingWeightSample, recordPhaseTransfer, recordCulling, getFlock, listFlocks

Business rules enforced at the service layer (from §9): BR-01 (house must be
`EMPTY` to accept a flock), BR-10 (house type must match flock phase),
plus the PF1-specific rules from `docs/parent-flock-service.doc.md` §7.1
(valid house status transitions; hen/cock counts never go negative; phase
transfer validates destination houses are `PRODUCTION`+`EMPTY` and
transferred counts ≤ current counts; culling validates culled counts ≤
current counts and auto-closes the flock + house when counts reach zero).

**Endpoints** (`api.controller`), from §10 of the functional spec, PF1
subset only:

```
GET/POST   /api/v1/farm-sites
PATCH      /api/v1/farm-sites/{id}/status
GET/POST   /api/v1/farm-sites/{farmSiteId}/houses
PATCH      /api/v1/houses/{id}/status
POST       /api/v1/houses/{houseId}/sections
GET/POST   /api/v1/flocks
GET        /api/v1/flocks/{id}
POST       /api/v1/flocks/{id}/phase-transfer
POST       /api/v1/flocks/{id}/culling
POST       /api/v1/flocks/{id}/rearing-weight-samples
POST       /api/v1/auth/login
```

**Flyway migrations**: `parent-flock-service/src/main/resources/db/migration`
for the PF1 tables above; `shared-kernel/src/main/resources/db/shared-migration`
for `users`, `roles`, `user_role_assignments` + seed rows (packaged into the
shared-kernel jar; already wired into `application.yml`'s
`flyway.locations`, which references this path even though it doesn't exist
yet).

## 5. Testing

- Unit tests (JUnit 5 + Mockito), one class per service: `FarmSiteServiceTest`,
  `HouseServiceTest`, `FlockServiceTest` — cover the business-rule branches
  and exception paths listed in §4 above.
- `ParentFlockRepositoryIT` — Testcontainers-backed (ephemeral Postgres, not
  the docker-compose one), proving the unique constraint on `flockCode` and
  the derived-query methods hit real SQL correctly.
- `JwtTokenProviderTest` (shared-kernel, unit) — token issuance, validation,
  expiry, tampering rejected.
- `FlockControllerTest` (`@WebMvcTest`) — proves `@PreAuthorize` + `@Valid`
  wiring at the HTTP layer for one representative controller.

Target: every service-layer business rule branch has a test; this is a
stricter bar than the doc's blanket "≥70% coverage" (NFR-M-04) for the
slice actually built.

## 6. Explicitly deferred (not built in this pass)

- PF2–PF9 (daily operations, health, egg classification/storage/dispatch,
  inventory, reporting) and their entities/services/controllers.
- `AuditLog` + `@Audited` AOP interceptor.
- `Alert`/`Notification`/`AlertAcknowledgement` and all `@Scheduled` alert
  jobs (§3.4 of the architecture doc) — PF1's own alerting requirements
  (PF1-C-FR-02, PF1-C-FR-04, PF1-E-FR-01/02) are **not implemented**, only
  the data they'd read (bird counts, weight samples) exists.
- `UserRoleAssignment` scope enforcement (role checks are global, not
  per-farm-site/per-house).
- Cross-service integration (`HatcheryIntegrationClient`) — not reachable
  from PF1.
- User self-registration / password reset / account lockout enforcement
  (X1-FR-02/03) — the seed migration creates working dev accounts; a real
  user-management flow is out of scope.

---

*This is an addendum to, not a replacement for,
`docs/parent-flock-architecture.md` and `docs/parent-flock-service.doc.md`,
which remain the source of truth for entity fields and business rules not
repeated here.*
