# Identity Service (Authentication) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a working, tested `identity-service` covering authentication (login, JWT issuance, refresh-token rotation with reuse detection, logout, password change, admin-only user creation/unlock, JWKS publication) backed by the minimal `shared-kernel` pieces it needs.

**Architecture:** Rich domain model — entities own their invariants via behavior methods (`User.recordFailedLogin()`, `RefreshToken.markRotated(...)`), not anemic getter/setter bags. Services orchestrate: `AuthService` for the login/refresh/logout/change-password flows, `UserAdminService` for admin-only account management. Layering: `domain.model`/`domain.enums` (entity), `domain.repository` (repository), `application.service` (service), `application.dto` (application), `api.controller`/`api.exception`/`config`/`infrastructure.security`/`infrastructure.integration` to make it a runnable, secured REST service. JWT signing/validation and the security-context principal live in `shared-kernel` because `admin-service` (a future, separate plan) validates the same tokens.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security 6 (JWT RS256, stateless), PostgreSQL 16, Flyway, Lombok, jjwt 0.12.5, JUnit 5, Mockito, AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-08-29-identity-admin-services-design.md` (this plan's scope decisions — §5 and §7 specifically), `docs/Specs.MD` §4 (Actors & Roles), §5.5 Feature X1, §6.3 (Security NFRs), §6.5 (API error envelope).

## Global Constraints

- Java 21 / Spring Boot 3.3.4 — already pinned in the root `pom.xml`; do not change.
- Every entity extends `com.ppms.shared.persistence.BaseEntity` (UUID id, `createdAt`, `updatedAt`) — id is never redeclared on subclasses.
- Passwords hashed with bcrypt, cost factor exactly 12 (NFR-S-02).
- `@PreAuthorize` checks happen at the service layer, not only the controller (NFR-S-04).
- Cross-service references are by stored UUID only. `identity-service` never has a JPA relation into `admin` schema; the only coupling is `AdminRoleClient` returning plain `RoleClaim` DTOs.
- Refresh tokens: only the SHA-256 hex hash (64 lowercase hex chars) is ever persisted in `token_hash`/`replaced_by_token_hash`. The raw opaque token is returned to the caller once and never stored.
- All error responses use the envelope `{ timestamp, status, error, message, path }` (NFR-U-01), implemented via shared `ApiErrorResponse`.
- **Bootstrap admin constant** (must match exactly, byte-for-byte, whatever `admin-service`'s future plan seeds for the corresponding `UserRoleAssignment`): UUID `11111111-1111-1111-1111-111111111111`, email `admin@ppms.local`, username `admin`, dev-only password `ChangeMe123!`, pre-computed bcrypt (cost 12) hash `$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS` (same hash already used by the PF1 plan's seed data for the same password — reused here for consistency, not shared storage). Seeded with `mustChangePassword = true`.
- **Dev-only RSA keypair** used in `application.yml` (generated for this plan, embedded verbatim in Task 6) is for local dev/test only. Production key material comes from a real secrets manager — out of scope for this pass, per the design spec §2.
- **Naming note:** the PF1 plan (`docs/superpowers/plans/2026-08-27-parent-flock-pf1-foundation.md`) separately defines `com.ppms.shared.domain.model.User` as part of an earlier, now-superseded-for-auth shared-user pattern. This plan's `User` lives entirely inside `identity-service` at `com.ppms.identity.domain.model.User` — a different class, different package, different module. No code collision; don't confuse the two when reading both plans.
- **Scope:** this plan covers `identity-service` (authentication) only. `admin-service` (authorization: `Role`/`Permission`/`UserRoleAssignment`) is explicitly out of scope and needs its own separate plan, per the design spec's two-service split. Until `admin-service` exists, `AdminRoleClient` points at a URL nothing serves; every test in this plan mocks `AdminRoleClient` — no test here depends on a live `admin-service`.

---

## Task 1: Root Maven wrapper + verify reactor build

**Files:**
- Create: `mvnw` (copy of `parent-flock-service/mvnw`)
- Create: `mvnw.cmd` (copy of `parent-flock-service/mvnw.cmd`)
- Create: `.mvn/wrapper/maven-wrapper.properties` (copy of `parent-flock-service/.mvn/wrapper/maven-wrapper.properties`)

**Interfaces:** None — pure tooling.

- [ ] **Step 1: Copy the wrapper files to the repo root (skip if they already exist — another plan may have added them first)**

```bash
if [ ! -f ./mvnw ]; then
  cp parent-flock-service/mvnw ./mvnw
  cp parent-flock-service/mvnw.cmd ./mvnw.cmd
  mkdir -p .mvn/wrapper
  cp parent-flock-service/.mvn/wrapper/maven-wrapper.properties ./.mvn/wrapper/maven-wrapper.properties
  chmod +x ./mvnw
fi
```

- [ ] **Step 2: Verify the reactor build runs from root**

Run: `./mvnw -q validate`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit (skip if nothing changed)**

```bash
git add mvnw mvnw.cmd .mvn/wrapper/maven-wrapper.properties
git commit -m "build: add root Maven wrapper for reactor builds" || true
```

---

## Task 2: shared-kernel — BaseEntity, exception hierarchy, web response types

**Files:**
- Create: `shared-kernel/src/main/java/com/ppms/shared/persistence/BaseEntity.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/DomainException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/EntityNotFoundException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/BusinessRuleViolationException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/ValidationException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/FieldValidationError.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/ApiErrorResponse.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/PageResponse.java`

**Interfaces:**
- Produces: `BaseEntity` (`UUID id`, `Instant createdAt`, `Instant updatedAt`, `@MappedSuperclass`). `EntityNotFoundException.of(String entityType, Object id)` static factory. `ApiErrorResponse.of(int status, String error, String message, String path)` and an overload with `List<FieldValidationError>`. `FieldValidationError(String field, String message)` record.

If `shared-kernel/src/main/java/com/ppms/shared/persistence/BaseEntity.java` already exists (e.g. the PF1 plan ran first), skip straight to Task 3 after confirming the file matches the shape below — do not duplicate it.

Pure data/exception types with no business logic — no test step, consistent with the PF1 plan's convention for this same kind of file.

- [ ] **Step 1: `BaseEntity`**

```java
package com.ppms.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
```

- [ ] **Step 2: Exception hierarchy**

```java
package com.ppms.shared.exception;

public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
```

```java
package com.ppms.shared.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException of(String entityType, Object id) {
        return new EntityNotFoundException(entityType + " not found: " + id);
    }
}
```

```java
package com.ppms.shared.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
```

```java
package com.ppms.shared.exception;

public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Web response types**

```java
package com.ppms.shared.web;

public record FieldValidationError(String field, String message) {}
```

```java
package com.ppms.shared.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiErrorResponse of(int status, String error, String message, String path,
                                       List<FieldValidationError> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
```

```java
package com.ppms.shared.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
```

- [ ] **Step 4: Compile shared-kernel**

Run: `./mvnw -q -pl shared-kernel -am compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add shared-kernel/src/main/java/com/ppms/shared/persistence shared-kernel/src/main/java/com/ppms/shared/exception shared-kernel/src/main/java/com/ppms/shared/web
git commit -m "feat(shared-kernel): add BaseEntity, exception hierarchy, and API error envelope"
```

---
