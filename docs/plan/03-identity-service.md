# 03 — identity-service (authentication)

Port 8084 · DB `ppms_identity` · schema `identity` · package `com.example.identity_service`
(existing, **kept**) · migrations prefix `VID`.

Existing code to keep: `User`, `Credential`, `RefreshToken`, `LoginAuditEvent`,
`AuthService` (login/refresh/logout/logoutAll/changePassword with rotation + reuse
detection), `TokenService`, `PasswordPolicyService`, `LoginAuditService`, `AdminClient` +
`MockAdminClient`, `AuthController`, `SecurityConfig`, `V1__create_identity_schema.sql`.
Step 0 of this file is an **audit & alignment pass**, then features are added.

---

## 1. Responsibilities
Who is this (authentication), session lifecycle, credentials, password policy, lockout,
MFA (TOTP), password reset, user account administration, JWKS. **Not** authorisation
(roles/permissions are admin-service) and **not** people/HR data (employees are
admin-service; identity holds a `employeeId` link only).

## 2. Roles touching this service
`SYSTEM_ADMIN` (create/disable/unlock/reset users), `HR_ADMIN` (create accounts for
employees, reset passwords, no role grants), any authenticated user (self-service).

## 3. Domain model (schema `identity`)

| Entity | Fields (beyond BaseEntity) | Notes |
|---|---|---|
| `User` | email (unique, citext), username (unique), displayName, phone, locale (`ro`/`en`), status `ACTIVE/LOCKED/DISABLED/PENDING_ACTIVATION`, failedLoginAttempts, lastLoginAt, employeeId (UUID, nullable), avatarUrl | existing + new columns |
| `Credential` | userId (PK/FK), passwordHash (bcrypt 12), passwordUpdatedAt, mustChangePassword, passwordHistory (jsonb last 5 hashes), mfaEnabled, mfaSecretEnc (AES-GCM), mfaRecoveryCodes (jsonb hashed) | existing + MFA |
| `RefreshToken` | tokenHash PK, userId, familyId, issuedAt, expiresAt, revokedAt, replacedByTokenHash, deviceInfo, ipAddress, lastUsedAt | existing + 2 cols |
| `LoginAuditEvent` | userId?, email, success, reason `BAD_PASSWORD/LOCKED/UNKNOWN_EMAIL/DISABLED/MFA_REQUIRED/MFA_FAILED/PASSWORD_EXPIRED`, ipAddress, userAgent, occurredAt | append-only |
| `PasswordResetToken` | tokenHash PK, userId, expiresAt (30 min), usedAt, requestedIp | new |
| `ActivationToken` | tokenHash PK, userId, expiresAt (7 days), usedAt | new (first login via e-mail link) |
| `ApiClient` | clientId, secretHash, name, allowedScopes, status | new, for service accounts / integrations (e.g. truck-scale bridge); client-credentials grant |

## 4. Business rules
- Password policy: ≥ 10 chars (raise from 8; configurable `identity.password.minLength`),
  upper, lower, digit, special, not in last 5, not equal to username/email local part,
  checked against a small common-password list; expiry `identity.password.maxAgeDays`
  (default 180 → `PASSWORD_EXPIRED` forces change).
- Lockout after 5 consecutive failures (`identity.lockout.threshold`); auto-unlock after
  `identity.lockout.autoUnlockMinutes` (default 30) **or** SYSTEM_ADMIN/HR_ADMIN unlock.
  Every attempt writes a `LoginAuditEvent`.
- Access token RS256, TTL `identity.jwt.accessTtlMinutes` default 15 (frontend silently
  refreshes); refresh token opaque 32 bytes, TTL 12 h for shared devices
  (`deviceType=KIOSK/SHARED`), 30 days for personal devices; **rotation on every use,
  reuse → revoke family** (existing). Max 10 active families per user.
- Claims: `sub`, `iss=ppms-identity`, `jti`, `iat`, `exp`, `username`, `name`, `locale`,
  `roles:[{role,scopeType,scopeId}]`, `perms:[...]`, `mcp` (mustChangePassword),
  `mfa` (bool), `kid` header. Roles/perms are fetched from admin-service
  `GET /internal/users/{id}/effective-access` at login and refresh (replace
  `MockAdminClient` with `RestClient`-based `HttpAdminClient`, 2 s timeout, fail-closed
  → `503 ADMIN_UNAVAILABLE` on login; on refresh reuse previous claims if admin down,
  max 1 time).
- `mustChangePassword=true` → every endpoint except `change-password`, `logout`, `me`
  returns `403 PASSWORD_CHANGE_REQUIRED` (filter in shared-kernel reads `mcp` claim).
- MFA (TOTP RFC 6238): optional per user, **required** for `SYSTEM_ADMIN`, `HR_ADMIN`,
  `SLAUGHTER_MANAGER`, `FARM_MANAGER` when setting `identity.mfa.requiredRoles` says so.
  Login returns `{mfaRequired:true, mfaToken}` (5 min) → `POST /auth/mfa/verify`.
- PIN login for kiosks: users may set a 6-digit PIN usable **only** from devices
  registered as `KIOSK` (device token issued by SYSTEM_ADMIN, bound to siteId); PIN login
  yields a short session (2 h) with the same claims. Lockout after 3 PIN failures.
- Account disable revokes all refresh families immediately and publishes `user.disabled`.
- Bootstrap admin from Flyway seed (`admin@ppms.local`, documented UUID,
  `mustChangePassword=true`) — keep as is; add env override `PPMS_BOOTSTRAP_ADMIN_EMAIL`.

## 5. API (`/api/v1`)

| Method & path | Auth | Purpose |
|---|---|---|
| POST `/auth/login` | public | `{emailOrUsername, password, deviceType?}` → tokens or `mfaRequired` |
| POST `/auth/mfa/verify` | mfaToken | `{mfaToken, code}` → tokens |
| POST `/auth/pin-login` | kiosk device token header `X-Device-Token` | `{username, pin}` → tokens |
| POST `/auth/refresh` | public | rotate |
| POST `/auth/logout` · `/auth/logout-all` | public / bearer | revoke family / all |
| GET `/auth/me` | bearer | profile + roles + perms + settings for UI (locale, must change) |
| PATCH `/auth/me` | bearer | displayName, phone, locale, avatar |
| POST `/auth/change-password` | bearer | existing |
| POST `/auth/forgot-password` | public, rate-limited | always 202; e-mails reset link (event to notification-service) |
| POST `/auth/reset-password` | public | `{token, newPassword}` |
| POST `/auth/activate` | public | `{token, newPassword}` first-time activation |
| GET/POST `/auth/mfa/setup` · POST `/auth/mfa/enable` · POST `/auth/mfa/disable` | bearer (+password re-auth) | TOTP QR provisioning (otpauth URI), verify, disable |
| POST `/auth/pin` · DELETE `/auth/pin` | bearer | set/remove PIN |
| GET `/auth/sessions` · DELETE `/auth/sessions/{familyId}` | bearer | list/revoke own devices |
| GET `/users` (filters: status, q, employeeId) | SYSTEM_ADMIN, HR_ADMIN | paged |
| POST `/users` | SYSTEM_ADMIN, HR_ADMIN | `{email, username, displayName, employeeId?, locale, sendActivationEmail}` → creates PENDING_ACTIVATION (link) or ACTIVE with temp password |
| GET `/users/{id}` · PATCH `/users/{id}` | SYSTEM_ADMIN, HR_ADMIN | profile fields |
| POST `/users/{id}/unlock` · `/disable` · `/enable` · `/reset-password` · `/mfa/reset` · `/force-password-change` | SYSTEM_ADMIN, HR_ADMIN | admin actions, all `@Audited` |
| GET `/users/{id}/login-events` | SYSTEM_ADMIN | paged audit |
| GET/POST `/kiosk-devices` · DELETE `/kiosk-devices/{id}` | SYSTEM_ADMIN | register tablet as kiosk for site → device token shown once |
| GET/POST `/api-clients` · POST `/oauth/token` (client_credentials) | SYSTEM_ADMIN / client | service accounts |
| GET `/.well-known/jwks.json` | public | key set (current + previous kid) |
| GET `/internal/users/{id}/summary` | internal | displayName/email for admin-service directory (sync fallback) |

Rate limits (gateway + local): login 10/min/IP, forgot-password 3/h/email.

## 6. Events published (`ppms.identity.user`)
`user.created {userId,email,username,displayName,employeeId,locale}`, `user.updated`,
`user.locked`, `user.unlocked`, `user.disabled`, `user.enabled`,
`user.password-reset-requested {userId,email,resetUrl,expiresAt}` (notification-service
sends mail), `user.activation-requested`, `user.login-succeeded {userId, ip, device}`
(reporting: last activity), `user.mfa-enabled`.
Consumed: `ppms.admin.role-assignment` → nothing stored, but invalidates
`effective-access` cache if one is introduced; `ppms.admin.employee.deactivated` → auto
`DISABLED` user linked to that employeeId.

## 7. Scheduled jobs
Purge expired refresh/reset/activation tokens nightly; auto-unlock scanner every minute;
password-expiry warning event 14 days before (`user.password-expiring`).

## 8. Migrations
`VID002__alter_users_profile_fields.sql`, `VID003__credential_history_mfa.sql`,
`VID004__reset_activation_tokens.sql`, `VID005__kiosk_devices_api_clients.sql`,
`VID006__outbox_processed_audit.sql` (shared snippets), `VID007__indexes.sql`.
Rename existing `V1__…` only if Flyway history is empty in all envs (it is dev-only now:
acceptable to reset the DB — document in DECISIONS-LOG).

## 9. Tests
Unit: policy validator table-driven, lockout counter, rotation/reuse, TOTP window
(±1 step), PIN lock, claim assembly. IT (Testcontainers): full login→refresh→logout,
reuse revokes family, admin create-user → activation mail event in Kafka, forgot/reset
flow, JWKS shape, `mcp` gating, kiosk PIN login rejected without device token,
`/users` 403 for non-admin.

## 10. Task list (in order)

| # | Task | Acceptance |
|---|---|---|
| I0 | Alignment pass: move to shared-kernel `SecurityDefaults`/`GlobalExceptionHandler`/`BaseEntity`; add outbox/audit/processed tables; replace `MockAdminClient` with `HttpAdminClient` behind the same `AdminClient` interface (mock kept for tests); fix test package name (`com.example.parent_flock_service` → identity) | existing tests green; `/auth/login` works against admin-service stub |
| I1 | User profile fields, `/auth/me`, `PATCH /auth/me`, locale | IT |
| I2 | Users admin API + activation flow + events | IT; mail visible in Mailpit via notification-service (or event asserted) |
| I3 | Password policy v2 (history, expiry, common list), force change | unit + IT |
| I4 | Lockout auto-unlock + admin unlock + `user.locked` event | IT |
| I5 | Forgot/reset password | IT |
| I6 | Sessions list/revoke, device types & TTLs | IT |
| I7 | MFA TOTP (setup/enable/verify/disable/reset, required roles) | IT with generated codes (`TestClock`) |
| I8 | Kiosk devices + PIN login | IT |
| I9 | API clients + client_credentials token | IT |
| I10 | JWKS with `kid` rotation; shared-kernel validator honours multiple keys | IT |
| I11 | Scheduled purges, metrics (`ppms_login_failures_total`), OpenAPI polish | — |

## Exit criteria
Frontend can: log in (incl. MFA/PIN), stay logged in via refresh, see `/auth/me`,
change/reset password; admins manage users; all writes audited; coverage ≥ 70 %.
