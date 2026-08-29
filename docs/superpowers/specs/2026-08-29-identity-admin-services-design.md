# Identity Service + Admin (Authorization) Service — Design

**Status:** Approved for implementation
**Parent documents:** `docs/Specs.MD` §4 (Actors & Roles), §5.5 Feature X1 (User & Access
Management), `docs/parent-flock-architecture.md` §2 (Shared Kernel)
**Scope of this pass:** two new services (`identity-service`, `admin-service`), the
shared-kernel pieces they need, and their tests. Nothing else in the repo is touched.

---

## 1. Why two services, not one

Authentication ("who is this, prove it") and authorization ("what can they do, where")
are different bounded contexts with different change frequency and different owners:
Identity changes on every login/refresh (high frequency, low-stakes-per-write);
Admin changes rarely (a role/permission edit) but must stay the single source of truth
for "what does role X allow." Splitting them mirrors the pattern the rest of the repo
already uses (one bounded context = one service, cross-service refs by ID only).

## 2. What's explicitly out of scope for this pass

- **The 4 existing services** (`parent-flock`, `hatchery`, `farm`, `slaughterhouse-service`)
  — not touched. They keep whatever auth they have (or don't) today. Wiring them to
  validate tokens issued by Identity Service is a future pass.
- **DB consolidation** of the 4 existing services into shared schemas — not done. Their
  docker-compose entries are untouched.
- **OAuthConnection** (external SSO, service-to-service OAuth2 clients) — YAGNI, no
  concrete need identified yet.
- **MFA enforcement** — schema field reserved (`Credential.mfaEnabled`), no TOTP/WebAuthn
  flow implemented.
- **Audience-claim (`aud`) scoping** — deferred; JWT is valid for any service that trusts
  the signing key.
- **Password reset via email** — no email infrastructure decided yet; deferred.

## 3. Topology

Two new Maven modules under the existing reactor, following the same shape as the 4
existing services:

```
identity-service/     Spring Boot app, port 8084
admin-service/         Spring Boot app, port 8085
```

Both depend on `shared-kernel`. Both get **one new** Postgres container
(`postgres-identity-admin`, port 5436) with two schemas: `identity`, `admin` — the 4
existing Postgres containers in `docker-compose.yml` are untouched; this is a new,
additive service block.

```
docker-compose.yml (additive)
  postgres-identity-admin  (postgres:16, port 5436, DB "ppms_iam")
    ├── schema: identity   ← owned by identity-service's Flyway migrations
    └── schema: admin      ← owned by admin-service's Flyway migrations
```

Each service's `application.yml` sets `spring.jpa.properties.hibernate.default_schema`
and Flyway's `schemas` to its own schema name, and connects with a least-privilege DB
role granted only on that schema.

## 4. shared-kernel — additions needed

`shared-kernel/src` is currently empty on `main`. This pass adds only what identity- and
admin-service need to compile and run; nothing from the wider shared-kernel spec
(`AuditLog`, `Alert`, `Notification`, `Employee`, the `@Audited` AOP interceptor) is
added — out of scope until a feature needs it.

- `persistence.BaseEntity` — `id` (UUID), `createdAt`, `updatedAt`.
- `exception.DomainException`, `EntityNotFoundException`,
  `BusinessRuleViolationException`, `ValidationException`.
- `web.ApiErrorResponse`, `FieldValidationError`, `PageResponse<T>`.
- `domain.enums.ScopeType` — `GLOBAL`, `FARM_SITE`, `HATCHERY`, `FARM`,
  `SLAUGHTERHOUSE` — shared between Admin's `UserRoleAssignment` and the JWT `roles`
  claim so both sides agree on the same vocabulary.
- `security.JwtKeyProvider` — loads/holds the RSA key pair (signing key private,
  verification key public) from configured PEM values.
- `security.JwtTokenProvider` — issues (identity-service only) and validates (both
  services) RS256 JWTs; exposes the public key as a JWK for the `/.well-known/jwks.json`
  endpoint.
- `security.JwtAuthenticationFilter` — `OncePerRequestFilter`; extracts Bearer token,
  validates via `JwtTokenProvider`, builds `UserPrincipal` from claims only (no DB
  lookup), populates `SecurityContextHolder`.
- `security.UserPrincipal` — `userId`, `List<RoleClaim>` (`role`, `scopeType`,
  `scopeId`), implements `UserDetails` mapping each `RoleClaim.role` to a
  `SimpleGrantedAuthority("ROLE_" + role)` so `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`
  works unchanged.

This is purely additive — no code outside these two new services references it, so it
cannot break anything on `main`.

## 5. Identity Service

### 5.1 Entities (`identity` schema)

**`User`** — `id` (from `BaseEntity`), `email` (unique), `username` (unique), `status`
(`ACTIVE` / `LOCKED` / `DISABLED`), `failedLoginAttempts`, `lastLoginAt`.

**`Credential`** — `userId` (unique, one-to-one with `User`), `passwordHash` (bcrypt,
cost 12), `passwordUpdatedAt`, `mustChangePassword` (boolean), `mfaEnabled` (boolean,
reserved/unused this pass).

**`RefreshToken`** — `userId`, `tokenHash` (SHA-256 of the opaque token, never store
raw), `familyId` (UUID, shared by a token and everything it rotates into),
`issuedAt`, `expiresAt`, `revokedAt` (nullable), `replacedByTokenHash` (nullable),
`deviceInfo` (nullable, from `User-Agent` header).

**`LoginAuditEvent`** — `userId` (nullable — a login attempt against an unknown email
still gets logged), `email` (as submitted), `success` (boolean), `reason` (nullable —
`BAD_PASSWORD` / `LOCKED` / `UNKNOWN_EMAIL`), `ipAddress`, `occurredAt`. Append-only:
repository exposes `save()` and `findBy*()` only, no update/delete, same convention as
the system-wide `AuditLog`.

### 5.2 Business rules

- **Lockout**: 5 consecutive failed attempts (per `X1-FR-02`/`X1-FR-03` in `Specs.MD`)
  → `status = LOCKED`. Only an authenticated `SYSTEM_ADMIN` (via a dedicated unlock
  endpoint) can clear it. Every attempt — success or failure — writes one
  `LoginAuditEvent`.
- **Password policy**: minimum 8 characters, ≥1 uppercase, ≥1 digit, ≥1 special
  character (per `X1-FR-02`), enforced in the request DTO validator, bcrypt cost 12
  (per `NFR-S-02`).
- **Access token**: RS256, 15-minute TTL, claims: `sub` (userId), `roles` (array of
  `{role, scopeType, scopeId}` fetched from Admin service at mint time), `iat`,
  `exp`, `jti`.
- **Refresh token**: opaque random value, 30-day TTL, **rotated on every use** — the
  old token is marked `revokedAt` + `replacedByTokenHash` pointing at the new one.
  **Reuse detection**: if a refresh token whose `tokenHash` no longer matches the
  current head of its `familyId` (i.e. it was already rotated) is presented again,
  the entire family is revoked and the caller must re-authenticate. This is the
  standard theft-detection mechanism (same idea as Keycloak/Auth0 refresh rotation).
- **Bootstrap admin**: both services seed independently, tied together only by a
  fixed, documented UUID constant (no cross-schema access, consistent with
  `NFR-M-02`). identity-service's Flyway seed inserts one `User`
  (`admin@ppms.local`, id = the documented constant) + `Credential` with a
  documented initial password and `mustChangePassword = true`. admin-service's
  Flyway seed independently inserts the `SYSTEM_ADMIN` / `GLOBAL`
  `UserRoleAssignment` row with `userId` = that same constant. Login succeeds
  normally; any authenticated call other than "change my password" is blocked while
  `mustChangePassword = true`.

### 5.3 Endpoints

```
POST   /api/v1/auth/login              { email, password } → { accessToken, refreshToken }
POST   /api/v1/auth/refresh            { refreshToken } → { accessToken, refreshToken }
POST   /api/v1/auth/logout             { refreshToken } → revokes that token's family
POST   /api/v1/auth/logout-all         (authenticated) → revokes every RefreshToken for the caller
POST   /api/v1/auth/change-password    (authenticated) { currentPassword, newPassword }
POST   /api/v1/admin/users             (SYSTEM_ADMIN only) { email, username, temporaryPassword? }
                                        → creates User + Credential, mustChangePassword=true,
                                        generates a random temporary password if none supplied
                                        (returned once in the response, never logged)
POST   /api/v1/admin/users/{id}/unlock (SYSTEM_ADMIN only) → clears LOCKED status
GET    /.well-known/jwks.json          (public) → RSA public key as a JWK set
```

`POST /api/v1/admin/users` is the endpoint requested: `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`
at the service layer (not just the controller, per `NFR-S-04`). It does not assign any
role — that's a separate call to Admin Service's assignment endpoint, keeping "create an
account" and "grant access" as two distinct, auditable actions.

### 5.4 Integration with Admin Service

At login and at refresh, `AuthService` calls
`GET {admin-service}/internal/users/{userId}/effective-roles` (blocking `RestTemplate`,
same pattern as the documented `HatcheryIntegrationClient`) to fetch the current role
assignments to embed as JWT claims. This endpoint is internal-only (not exposed through
any gateway/public route) and is the *only* synchronous coupling between the two
services — it happens at token-mint time, never on a per-request hot path.

## 6. Admin Service

### 6.1 Entities (`admin` schema)

**`Role`** — `roleName` (unique, e.g. `SYSTEM_ADMIN`, `FARM_VET`), `description`,
`isSystemDefined` (boolean — the roles already catalogued in `Specs.MD` §4 are seeded
as system-defined; new roles created via the API are not).

**`Permission`** — `permissionKey` (unique, e.g. `identity:user:create`), `description`.

**`RolePermission`** — join table, `roleId`, `permissionId`.

**`UserRoleAssignment`** — `userId` (plain UUID, references Identity's `User` by ID
only, per `NFR-M-02`), `roleId`, `scopeType` (`ScopeType` from shared-kernel),
`scopeId` (nullable — null when `scopeType = GLOBAL`), `assignedBy`, `assignedAt`,
`expiresAt` (nullable).

### 6.2 Business rules

- A `UserRoleAssignment` with `scopeType != GLOBAL` requires a non-null `scopeId`;
  `GLOBAL` requires a null `scopeId` — validated at the service layer.
- Role/Permission mutations (`create`, `update`, `delete`) require `SYSTEM_ADMIN`.
- Deleting a `Role` that has active `UserRoleAssignment` rows is rejected
  (`BusinessRuleViolationException`) — assignments must be revoked first.
- Seed data (Flyway) creates the roles already catalogued in `Specs.MD` §4
  (`SYSTEM_ADMIN`, `MANAGER`, `FLOCK_SUPERVISOR`, `FLOCK_WORKER`, `FLOCK_VET`,
  `HATCHERY_SUPERVISOR`, ... all 15) as `isSystemDefined = true`, plus the one
  `SYSTEM_ADMIN`/`GLOBAL` assignment for the bootstrap admin user from §5.2.

### 6.3 Endpoints

```
GET/POST         /api/v1/roles                       (SYSTEM_ADMIN)
GET/PUT/DELETE    /api/v1/roles/{id}                  (SYSTEM_ADMIN)
GET/POST          /api/v1/permissions                 (SYSTEM_ADMIN)
POST/DELETE       /api/v1/roles/{id}/permissions/{permId}   (SYSTEM_ADMIN)
GET/POST          /api/v1/user-role-assignments        (SYSTEM_ADMIN)
DELETE            /api/v1/user-role-assignments/{id}    (SYSTEM_ADMIN)
GET               /api/v1/users/{userId}/role-assignments (SYSTEM_ADMIN)
GET               /internal/users/{userId}/effective-roles  (internal — called only by identity-service)
```

## 7. Testing strategy

Per `NFR-M-04` (≥70% service-layer coverage, Testcontainers for real Postgres):

- **Unit tests** (JUnit 5 + Mockito): password policy validation, lockout threshold,
  refresh-token rotation/reuse-detection logic, scope validation
  (`GLOBAL` ⇒ null `scopeId`), role-deletion-blocked-while-assigned rule.
- **Integration tests** (Testcontainers Postgres + `@SpringBootTest`):
  - Identity: login success/bad-password/unknown-email/locked-account, full
    refresh-rotation cycle, reuse of a rotated token revokes the family, `POST
    /api/v1/admin/users` returns 403 for a non-admin token and 201 for an admin
    token, JWKS endpoint returns a well-formed key set.
  - Admin: Role/Permission/UserRoleAssignment CRUD, scope validation, internal
    effective-roles endpoint returns the right claim shape for a multi-role user.
- **Cross-service test**: a `@SpringBootTest` in identity-service with a mocked
  Admin client verifying the claim shape assembled into the JWT matches §5.2/§6.3.

## 8. Roadmap slice (M/S/C)

**Must (this pass):** everything in §5 and §6 above, including refresh-token
rotation + reuse detection and the admin-only user-creation endpoint.
**Should (next pass):** password reset via email, `RoleAuditLog` (who granted what to
whom), wiring the 4 existing services to validate identity-service's JWKS.
**Could (future):** MFA enforcement, `OAuthConnection`, `aud`-claim scoping, DB
consolidation of the 4 existing services into the shared Postgres server.
