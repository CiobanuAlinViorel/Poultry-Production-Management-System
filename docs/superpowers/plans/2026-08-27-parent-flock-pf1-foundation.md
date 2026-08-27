# Parent Flock Service — PF1 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a working, tested `parent-flock-service` covering PF1 (farm sites, houses, flock lifecycle) with JWT-secured endpoints, backed by the minimal `shared-kernel` pieces it needs.

**Architecture:** Rich domain model (DDD tactical style) — entities own their invariants via behavior methods (e.g. `ParentFlock.applyCulling(...)`, `HouseStatus.canTransitionTo(...)`), not anemic getter/setter bags. Services orchestrate cross-aggregate rules (does the house exist, is it the right type) and call entity methods for single-aggregate invariants. Layering: `domain.model`/`domain.enums` (entity), `domain.repository` (repository), `application.service` (service), `application.dto`/`application.mapper` (application) — plus `api.controller`/`api.exception`/`config`/`infrastructure.persistence` to make it a runnable, secured REST service.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security 6 (JWT, stateless), PostgreSQL 16, Flyway, MapStruct 1.5.5, Lombok, jjwt 0.12.5, JUnit 5, Mockito, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-08-27-parent-flock-pf1-foundation-design.md` (this plan's scope decisions), `docs/parent-flock-architecture.md` (entity fields, package layout), `docs/parent-flock-service.doc.md` (business rules, functional requirements).

## Global Constraints

- Java 21 / Spring Boot 3.3.4 — already pinned in the root `pom.xml`; do not change.
- Cross-service references are by stored UUID only — not applicable within this plan (single service), but do not add `@ManyToOne` from `ParentFlock`/`ParentFlockHouse` to anything outside this schema.
- All service-layer methods are `@Transactional` (per `docs/parent-flock-architecture.md` §3.3).
- Role checks (`@PreAuthorize`) happen at the service layer, not the controller (per NFR-S-04 in `docs/Specs.MD`).
- Passwords hashed with bcrypt cost ≥ 12 (per `docs/parent-flock-architecture.md` §2.4 `User`).
- Every entity extends `BaseEntity` (UUID id, `createdAt`, `updatedAt`) — id is never redeclared on subclasses.
- No JPA entity in `parent-flock-service` may reference a table outside `parent_flock_db` — `farmSiteId`, `houseId`, etc. on cross-aggregate-but-same-schema entities are stored as plain UUID fields, not `@ManyToOne`, **except** `TransferDestination → RearingToProductionTransfer`, which is a true same-transaction parent/child relationship and uses a real JPA `@ManyToOne`/`@OneToMany`.
- Dev/test bcrypt hash for password `ChangeMe123!` at cost 12: `$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS` (verified round-trip). Use this exact string in seed migrations — do not regenerate it.

---

## Task 1: Root Maven wrapper + verify reactor build

**Files:**
- Create: `mvnw` (copy of `parent-flock-service/mvnw`)
- Create: `mvnw.cmd` (copy of `parent-flock-service/mvnw.cmd`)
- Create: `.mvn/wrapper/maven-wrapper.properties` (copy of `parent-flock-service/.mvn/wrapper/maven-wrapper.properties`)

**Interfaces:** None — pure tooling, no code dependencies.

- [ ] **Step 1: Copy the wrapper files to the repo root**

```bash
cp parent-flock-service/mvnw ./mvnw
cp parent-flock-service/mvnw.cmd ./mvnw.cmd
mkdir -p .mvn/wrapper
cp parent-flock-service/.mvn/wrapper/maven-wrapper.properties ./.mvn/wrapper/maven-wrapper.properties
chmod +x ./mvnw
```

- [ ] **Step 2: Verify the reactor build runs from root**

Run: `./mvnw -q validate`
Expected: `BUILD SUCCESS` (validates all 5 modules resolve — shared-kernel, parent-flock-service, hatchery-service, farm-service, slaughterhouse-service). No compilation happens yet since there's no source beyond the existing skeletons.

- [ ] **Step 3: Commit**

```bash
git add mvnw mvnw.cmd .mvn/wrapper/maven-wrapper.properties
git commit -m "build: add root Maven wrapper for reactor builds"
```

---

## Task 2: shared-kernel — BaseEntity, exception hierarchy, web response types

**Files:**
- Create: `shared-kernel/src/main/java/com/ppms/shared/persistence/BaseEntity.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/DomainException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/EntityNotFoundException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/BusinessRuleViolationException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/ValidationException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/exception/WithdrawalPeriodActiveException.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/FieldValidationError.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/ApiErrorResponse.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/web/PageResponse.java`

**Interfaces:**
- Produces: `BaseEntity` (fields: `UUID id`, `Instant createdAt`, `Instant updatedAt` — all `@MappedSuperclass`, `id` generated). `DomainException(String message)` and subclasses, each with a `(String message)` constructor. `ApiErrorResponse.of(HttpStatus status, String message, String path)` static factory. `PageResponse.from(Page<T> page)` static factory returning `PageResponse<T>` with the same element type.

These are plain data/exception types with no business logic to unit-test — no test step for this task (TDD applies to logic-bearing code; pure data carriers are covered indirectly by every later test that uses them).

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

```java
package com.ppms.shared.exception;

public class WithdrawalPeriodActiveException extends DomainException {
    public WithdrawalPeriodActiveException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Web response types**

```java
package com.ppms.shared.web;

public record FieldValidationError(String field, Object rejectedValue, String message) {
}
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

    public static ApiErrorResponse ofValidation(int status, String error, String message, String path,
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
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
```

- [ ] **Step 4: Compile check**

Run: `./mvnw -q -pl shared-kernel -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add shared-kernel/src/main/java/com/ppms/shared/persistence shared-kernel/src/main/java/com/ppms/shared/exception shared-kernel/src/main/java/com/ppms/shared/web
git commit -m "feat(shared-kernel): base entity, exception hierarchy, web response types"
```

---

## Task 3: shared-kernel — User/Role/UserRoleAssignment entities + Flyway migrations

**Files:**
- Create: `shared-kernel/src/main/java/com/ppms/shared/domain/enums/ServiceModule.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/domain/enums/RoleScope.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/domain/model/User.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/domain/model/Role.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/domain/model/UserRoleAssignment.java`
- Create: `shared-kernel/src/main/resources/db/shared-migration/V1__create_shared_tables.sql`
- Create: `shared-kernel/src/main/resources/db/shared-migration/V2__seed_dev_users.sql`
- Test: `shared-kernel/src/test/java/com/ppms/shared/domain/model/UserTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2).
- Produces: `User` with `getUsername()`, `getPasswordHash()`, `getEmail()`, `isActive()`, `isLocked()`, `getFailedLoginAttempts()`, `getLastLoginAt()`, and behavior methods `registerFailedLogin()` (increments `failedLoginAttempts`, locks at 5) and `registerSuccessfulLogin()` (resets `failedLoginAttempts` to 0, sets `lastLoginAt`), plus static factory `User.create(username, passwordHash, email)`. `Role` with `getRoleName()`, `getModule()`, static factory `Role.create(roleName, description, module)`. `UserRoleAssignment` with `getUserId()`, `getRoleId()`, static factory `UserRoleAssignment.assign(userId, roleId, scopeEntityType, scopeEntityId, assignedBy)`.

- [ ] **Step 1: Write the failing test for `User` login-attempt behavior**

```java
package com.ppms.shared.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void locksAccountAfterFiveFailedLogins() {
        User user = User.create("jdoe", "hashed", "jdoe@example.com");

        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin();
            assertThat(user.isLocked()).isFalse();
        }
        user.registerFailedLogin();

        assertThat(user.isLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    void successfulLoginResetsFailedAttemptsAndSetsLastLoginAt() {
        User user = User.create("jdoe", "hashed", "jdoe@example.com");
        user.registerFailedLogin();
        user.registerFailedLogin();

        user.registerSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLastLoginAt()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl shared-kernel -am test -Dtest=UserTest`
Expected: FAIL — compile error, `User` class does not exist yet.

- [ ] **Step 3: Write the enums**

```java
package com.ppms.shared.domain.enums;

public enum ServiceModule {
    PARENT_FLOCK, HATCHERY, FARM, SLAUGHTERHOUSE
}
```

```java
package com.ppms.shared.domain.enums;

public enum RoleScope {
    FLOCK_HOUSE, FARM_SITE, HATCHERY, SLAUGHTERHOUSE
}
```

- [ ] **Step 4: Write `User`, `Role`, `UserRoleAssignment`**

```java
package com.ppms.shared.domain.model;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant passwordExpiresAt;

    private Instant lastLoginAt;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public static User create(String username, String passwordHash, String email) {
        return new User(username, passwordHash, email);
    }

    public void registerFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.locked = true;
        }
    }

    public void registerSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = Instant.now();
    }
}
```

```java
package com.ppms.shared.domain.model;

import com.ppms.shared.domain.enums.ServiceModule;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String roleName;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceModule module;

    private Role(String roleName, String description, ServiceModule module) {
        this.roleName = roleName;
        this.description = description;
        this.module = module;
    }

    public static Role create(String roleName, String description, ServiceModule module) {
        return new Role(roleName, description, module);
    }
}
```

```java
package com.ppms.shared.domain.model;

import com.ppms.shared.domain.enums.RoleScope;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_role_assignments")
@Getter
@NoArgsConstructor
public class UserRoleAssignment extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    private RoleScope scopeEntityType;

    private UUID scopeEntityId;

    private UUID assignedBy;

    @Column(nullable = false)
    private Instant assignedAt;

    private Instant expiresAt;

    private UserRoleAssignment(UUID userId, UUID roleId, RoleScope scopeEntityType,
                                UUID scopeEntityId, UUID assignedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.scopeEntityType = scopeEntityType;
        this.scopeEntityId = scopeEntityId;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public static UserRoleAssignment assign(UUID userId, UUID roleId, RoleScope scopeEntityType,
                                             UUID scopeEntityId, UUID assignedBy) {
        return new UserRoleAssignment(userId, roleId, scopeEntityType, scopeEntityId, assignedBy);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q -pl shared-kernel -am test -Dtest=UserTest`
Expected: PASS (2 tests)

- [ ] **Step 6: Write the Flyway migrations**

```sql
-- shared-kernel/src/main/resources/db/shared-migration/V1__create_shared_tables.sql
CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    username               VARCHAR(100) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    email                  VARCHAR(255) NOT NULL UNIQUE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    locked                 BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    password_expires_at    TIMESTAMP,
    last_login_at          TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id           UUID PRIMARY KEY,
    role_name    VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(255),
    module       VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_role_assignments (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,
    role_id            UUID NOT NULL,
    scope_entity_type  VARCHAR(50),
    scope_entity_id    UUID,
    assigned_by        UUID,
    assigned_at        TIMESTAMP NOT NULL DEFAULT now(),
    expires_at         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_role_assignments_user_id ON user_role_assignments(user_id);
```

```sql
-- shared-kernel/src/main/resources/db/shared-migration/V2__seed_dev_users.sql
-- Dev-only seed data. Password for every seeded user is "ChangeMe123!"
-- (bcrypt cost 12: $2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS)

INSERT INTO roles (id, role_name, description, module, created_at, updated_at) VALUES
    ('6a56e430-d110-473f-a7cb-0ddc084f5b07', 'FLOCK_SUPERVISOR', 'Full CRUD over parent flock records; approves transfers, sales, culling', 'PARENT_FLOCK', now(), now()),
    ('93f2734f-7c61-4e1f-aa4c-98f006edb241', 'FLOCK_WORKER', 'Records daily operations for assigned houses', 'PARENT_FLOCK', now(), now()),
    ('1d64a808-5a58-49f7-a0b9-344e5221e13a', 'FLOCK_VET', 'Creates health, vaccination, and biosecurity records', 'PARENT_FLOCK', now(), now()),
    ('365ba0b5-965a-4f15-bd78-6246c4ba819f', 'MANAGER', 'Read access across all records', 'PARENT_FLOCK', now(), now());

INSERT INTO users (id, username, password_hash, email, active, locked, failed_login_attempts, created_at, updated_at) VALUES
    ('954abcd9-d0f2-46dd-b72d-5c6ac2b75e97', 'supervisor1', '$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS', 'supervisor1@ppms.local', TRUE, FALSE, 0, now(), now()),
    ('c06f142d-781e-4022-8d0a-c634ec0a38a8', 'worker1', '$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS', 'worker1@ppms.local', TRUE, FALSE, 0, now(), now()),
    ('8bff3562-6c5c-4a14-9bf7-a69e2cac1a91', 'vet1', '$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS', 'vet1@ppms.local', TRUE, FALSE, 0, now(), now()),
    ('107b0bff-84ee-4943-98d9-d79a32629798', 'manager1', '$2b$12$BaWl1fmagUdB5IgnTXvpPuNAYdtx1HQ8gQYx/AsW7du6kQjzxW.lS', 'manager1@ppms.local', TRUE, FALSE, 0, now(), now());

INSERT INTO user_role_assignments (id, user_id, role_id, assigned_at, created_at, updated_at) VALUES
    ('e51bc1b8-ab3a-4d67-8dab-5662befd6dd5', '954abcd9-d0f2-46dd-b72d-5c6ac2b75e97', '6a56e430-d110-473f-a7cb-0ddc084f5b07', now(), now(), now()),
    ('a15dead9-2c26-4818-b9cf-857732a83905', 'c06f142d-781e-4022-8d0a-c634ec0a38a8', '93f2734f-7c61-4e1f-aa4c-98f006edb241', now(), now(), now()),
    ('258b61aa-69cc-4933-a070-8373f57a17a3', '8bff3562-6c5c-4a14-9bf7-a69e2cac1a91', '1d64a808-5a58-49f7-a0b9-344e5221e13a', now(), now(), now()),
    ('ff73bc99-c2a9-4181-82c6-862c125e1ec3', '107b0bff-84ee-4943-98d9-d79a32629798', '365ba0b5-965a-4f15-bd78-6246c4ba819f', now(), now(), now());
```

- [ ] **Step 7: Commit**

```bash
git add shared-kernel/src/main/java/com/ppms/shared/domain shared-kernel/src/main/resources/db shared-kernel/src/test
git commit -m "feat(shared-kernel): User/Role/UserRoleAssignment entities, shared migrations, seed data"
```

---

## Task 4: shared-kernel — JWT token provider

**Files:**
- Create: `shared-kernel/src/main/java/com/ppms/shared/security/JwtProperties.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/security/UserPrincipal.java`
- Create: `shared-kernel/src/main/java/com/ppms/shared/security/JwtTokenProvider.java`
- Test: `shared-kernel/src/test/java/com/ppms/shared/security/JwtTokenProviderTest.java`

**Interfaces:**
- Produces: `JwtTokenProvider.generateToken(String username, List<String> roles)` → `String`; `validateToken(String token)` → `boolean`; `getUsername(String token)` → `String`; `getRoles(String token)` → `List<String>`; `getExpirationMillis()` → `long`. `JwtProperties` is `@ConfigurationProperties(prefix = "ppms.jwt")` with `String secret` and `long expirationMs`.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dev-only-secret-key-must-be-at-least-32-bytes-long-for-hs256");
        properties.setExpirationMs(3_600_000L);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    void generatesTokenThatValidatesAndRoundTripsClaims() {
        String token = provider.generateToken("jdoe", List.of("FLOCK_SUPERVISOR", "MANAGER"));

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("jdoe");
        assertThat(provider.getRoles(token)).containsExactlyInAnyOrder("FLOCK_SUPERVISOR", "MANAGER");
    }

    @Test
    void rejectsTamperedToken() {
        String token = provider.generateToken("jdoe", List.of("FLOCK_SUPERVISOR"));
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("dev-only-secret-key-must-be-at-least-32-bytes-long-for-hs256");
        expiredProps.setExpirationMs(-1_000L);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

        String token = expiredProvider.generateToken("jdoe", List.of("FLOCK_SUPERVISOR"));

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl shared-kernel -am test -Dtest=JwtTokenProviderTest`
Expected: FAIL — compile error, `JwtProperties`/`JwtTokenProvider` don't exist yet.

- [ ] **Step 3: Write `JwtProperties`**

```java
package com.ppms.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ppms.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expirationMs = 28_800_000L; // 8 hours, matches X1-FR-04 default
}
```

- [ ] **Step 4: Write `UserPrincipal`**

```java
package com.ppms.shared.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class UserPrincipal implements UserDetails {

    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 5: Write `JwtTokenProvider`**

```java
package com.ppms.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) parseClaims(token).get("roles", List.class);
    }

    public long getExpirationMillis() {
        return properties.getExpirationMs();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./mvnw -q -pl shared-kernel -am test -Dtest=JwtTokenProviderTest`
Expected: PASS (4 tests)

- [ ] **Step 7: Commit**

```bash
git add shared-kernel/src/main/java/com/ppms/shared/security shared-kernel/src/test/java/com/ppms/shared/security
git commit -m "feat(shared-kernel): JWT token provider with issue/validate/parse"
```

---

## Task 5: shared-kernel — JWT authentication filter

**Files:**
- Create: `shared-kernel/src/main/java/com/ppms/shared/security/JwtAuthenticationFilter.java`

**Interfaces:**
- Consumes: `JwtTokenProvider` (Task 4) — `validateToken`, `getUsername`, `getRoles`. `UserPrincipal` (Task 4) constructor `(String username, Collection<? extends GrantedAuthority> authorities)`.
- Produces: `JwtAuthenticationFilter(JwtTokenProvider provider)` — a plain `OncePerRequestFilter`, not a managed bean (instantiated explicitly in each service's `SecurityConfig` so filter-chain ordering stays visible at the call site).

No dedicated unit test for this task: `OncePerRequestFilter` logic against real `HttpServletRequest`/`FilterChain` needs either heavy mocking or a full Spring context, and its correctness is proven end-to-end by the `@WebMvcTest` in Task 15 (unauthenticated request → 401, valid token → 200) plus the `AuthServiceTest` in Task 8 for the token content itself. Wiring it here now and exercising it later avoids a throwaway mock-only test.

- [ ] **Step 1: Write `JwtAuthenticationFilter`**

```java
package com.ppms.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            List<GrantedAuthority> authorities = jwtTokenProvider.getRoles(token).stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            UserPrincipal principal = new UserPrincipal(username, authorities);
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./mvnw -q -pl shared-kernel -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add shared-kernel/src/main/java/com/ppms/shared/security/JwtAuthenticationFilter.java
git commit -m "feat(shared-kernel): JWT authentication filter"
```

---

## Task 6: parent-flock-service — package rename, application entrypoint, cross-module scanning

**Files:**
- Delete: `parent-flock-service/src/main/java/com/example/parent_flock_service/ParentFlockServiceApplication.java`
- Delete: `parent-flock-service/src/test/java/com/example/parent_flock_service/ParentFlockServiceApplicationTests.java`
- Delete: `parent-flock-service/src/main/resources/application.properties`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/ParentFlockServiceApplication.java`
- Create: `parent-flock-service/src/test/java/com/ppms/parentflock/ParentFlockServiceApplicationTests.java`
- Modify: `parent-flock-service/src/main/resources/application.yml`

**Interfaces:**
- Produces: `ParentFlockServiceApplication` — the app's component-scan and entity-scan root. Every later task's `@Component`/`@Service`/`@RestController`/`@Entity`/`@Repository` in `com.ppms.parentflock.*` and every `@Component`/`@Entity` in `com.ppms.shared.*` depends on this task's scan configuration to be picked up.

Why the explicit scanning config matters: `shared-kernel` entities/beans live under `com.ppms.shared`, outside `@SpringBootApplication`'s default scan root (`com.ppms.parentflock`). Without `scanBasePackages`/`@EntityScan` covering both, `User`/`Role`/`JwtTokenProvider` etc. silently never get picked up and the app fails at runtime with "no bean of type" or "not a managed type" errors that are painful to debug later — so it's fixed once, here, before anything depends on it.

- [ ] **Step 1: Delete the skeleton files**

```bash
git rm parent-flock-service/src/main/java/com/example/parent_flock_service/ParentFlockServiceApplication.java
git rm parent-flock-service/src/test/java/com/example/parent_flock_service/ParentFlockServiceApplicationTests.java
git rm parent-flock-service/src/main/resources/application.properties
```

(if `git rm` fails because the files aren't tracked yet, use plain `rm` instead)

- [ ] **Step 2: Write the new application entrypoint**

```java
package com.ppms.parentflock;

import com.ppms.shared.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.ppms.parentflock", "com.ppms.shared"})
@EntityScan(basePackages = {"com.ppms.parentflock.domain.model", "com.ppms.shared.domain.model"})
@EnableJpaRepositories(basePackages = "com.ppms.parentflock.domain.repository")
@EnableConfigurationProperties(JwtProperties.class)
public class ParentFlockServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParentFlockServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Update `application.yml`**

```yaml
# parent-flock-service
spring:
  application:
    name: parent-flock-service
  datasource:
    url: jdbc:postgresql://localhost:5432/parent_flock_db
    username: ppms
    password: ppms_dev
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    locations:
      - classpath:db/shared-migration
      - classpath:db/migration

server:
  port: 8080

ppms:
  jwt:
    secret: ${PPMS_JWT_SECRET:dev-only-secret-key-must-be-at-least-32-bytes-long-for-hs256}
    expiration-ms: 28800000

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

Note the Flyway `locations` order: `db/shared-migration` (V1, V2 — users/roles tables) must run before `db/migration` (V3+ — parent flock tables), since nothing in `db/migration` references the shared tables by FK, but logically the shared schema should exist first. Flyway actually orders strictly by version number across all locations combined, not by list order, so this ordering is for readability — the version numbers (Task 3 used V1/V2, Task 7 onward uses V3+) are what actually guarantees the order.

- [ ] **Step 4: Write the smoke test**

```java
package com.ppms.parentflock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ParentFlockServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

This test requires a reachable Postgres at `localhost:5432` (the docker-compose `postgres-parent-flock` service) to pass, since Flyway migrations run on context startup — it is not runnable until Task 3's migrations exist and the database is up. Leave it unverified for now; it gets exercised for real once the full slice is built (see the final verification task).

- [ ] **Step 5: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock parent-flock-service/src/test/java/com/ppms/parentflock parent-flock-service/src/main/resources/application.yml
git commit -m "refactor(parent-flock-service): rename package to com.ppms.parentflock, configure cross-module scanning"
```

---

## Task 7: parent-flock-service — domain enums (incl. `HouseStatus` transition rules)

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/HouseType.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/HouseStatus.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/HouseSectionType.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/VentilationType.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/FarmSiteStatus.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/FlockPhaseStatus.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/FlockHealthStatus.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/FlockSourceType.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/BirdType.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/MortalityCause.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/CullingReason.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums/DisposalMethod.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/enums/HouseStatusTest.java`

**Interfaces:**
- Produces: `HouseStatus.canTransitionTo(HouseStatus target)` → `boolean` — the state machine `EMPTY→OCCUPIED→CLEANING→EMPTY`, with `MAINTENANCE` reachable from any state and resolving back to `EMPTY`. `HouseService` (Task 12) and `ParentFlockHouse.changeStatus` (Task 9) depend on this exact method name/signature.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.parentflock.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HouseStatusTest {

    @Test
    void emptyCanOnlyTransitionToOccupiedOrMaintenance() {
        assertThat(HouseStatus.EMPTY.canTransitionTo(HouseStatus.OCCUPIED)).isTrue();
        assertThat(HouseStatus.EMPTY.canTransitionTo(HouseStatus.MAINTENANCE)).isTrue();
        assertThat(HouseStatus.EMPTY.canTransitionTo(HouseStatus.CLEANING)).isFalse();
        assertThat(HouseStatus.EMPTY.canTransitionTo(HouseStatus.EMPTY)).isFalse();
    }

    @Test
    void occupiedCanOnlyTransitionToCleaningOrMaintenance() {
        assertThat(HouseStatus.OCCUPIED.canTransitionTo(HouseStatus.CLEANING)).isTrue();
        assertThat(HouseStatus.OCCUPIED.canTransitionTo(HouseStatus.MAINTENANCE)).isTrue();
        assertThat(HouseStatus.OCCUPIED.canTransitionTo(HouseStatus.EMPTY)).isFalse();
    }

    @Test
    void cleaningCanOnlyTransitionToEmptyOrMaintenance() {
        assertThat(HouseStatus.CLEANING.canTransitionTo(HouseStatus.EMPTY)).isTrue();
        assertThat(HouseStatus.CLEANING.canTransitionTo(HouseStatus.MAINTENANCE)).isTrue();
        assertThat(HouseStatus.CLEANING.canTransitionTo(HouseStatus.OCCUPIED)).isFalse();
    }

    @Test
    void maintenanceCanOnlyResolveToEmpty() {
        assertThat(HouseStatus.MAINTENANCE.canTransitionTo(HouseStatus.EMPTY)).isTrue();
        assertThat(HouseStatus.MAINTENANCE.canTransitionTo(HouseStatus.OCCUPIED)).isFalse();
        assertThat(HouseStatus.MAINTENANCE.canTransitionTo(HouseStatus.CLEANING)).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=HouseStatusTest`
Expected: FAIL — compile error, `HouseStatus` doesn't exist yet.

- [ ] **Step 3: Write the simple enums**

```java
package com.ppms.parentflock.domain.enums;

public enum HouseType { REARING, PRODUCTION }
```

```java
package com.ppms.parentflock.domain.enums;

public enum HouseSectionType { SLAT, LITTER, NEST_ROW, SCRATCH, PEN }
```

```java
package com.ppms.parentflock.domain.enums;

public enum VentilationType { NATURAL, TUNNEL, FORCED }
```

```java
package com.ppms.parentflock.domain.enums;

public enum FarmSiteStatus { ACTIVE, INACTIVE }
```

```java
package com.ppms.parentflock.domain.enums;

public enum FlockPhaseStatus { REARING, PRODUCTION, CULLED }
```

```java
package com.ppms.parentflock.domain.enums;

public enum FlockHealthStatus { HEALTHY, QUARANTINE, TREATMENT, CULLED }
```

```java
package com.ppms.parentflock.domain.enums;

public enum FlockSourceType { OWN_STOCK, PURCHASED }
```

```java
package com.ppms.parentflock.domain.enums;

public enum BirdType { HEN, COCK }
```

```java
package com.ppms.parentflock.domain.enums;

public enum MortalityCause { DISEASE, INJURY, SUFFOCATION, CULLED, UNKNOWN }
```

```java
package com.ppms.parentflock.domain.enums;

public enum CullingReason { AGE, LOW_PERFORMANCE, DISEASE, ECONOMIC }
```

```java
package com.ppms.parentflock.domain.enums;

public enum DisposalMethod { INCINERATION, COMPOSTING, RENDERING }
```

- [ ] **Step 4: Write `HouseStatus` with the transition rule**

```java
package com.ppms.parentflock.domain.enums;

public enum HouseStatus {
    EMPTY, OCCUPIED, CLEANING, MAINTENANCE;

    /**
     * PF1-A-FR-02: EMPTY -> OCCUPIED -> CLEANING -> EMPTY, and MAINTENANCE
     * is reachable from any state and resolves back to EMPTY.
     */
    public boolean canTransitionTo(HouseStatus target) {
        if (this == target) {
            return false;
        }
        if (target == MAINTENANCE) {
            return true;
        }
        return switch (this) {
            case EMPTY -> target == OCCUPIED;
            case OCCUPIED -> target == CLEANING;
            case CLEANING -> target == EMPTY;
            case MAINTENANCE -> target == EMPTY;
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=HouseStatusTest`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/enums parent-flock-service/src/test/java/com/ppms/parentflock/domain/enums
git commit -m "feat(parent-flock-service): PF1 domain enums with HouseStatus transition rules"
```

---

## Task 8: `BreederFarmSite` entity, repository, migration

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/BreederFarmSite.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/BreederFarmSiteRepository.java`
- Create: `parent-flock-service/src/main/resources/db/migration/V3__create_farm_site_table.sql`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/BreederFarmSiteTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2), `FarmSiteStatus` (Task 7).
- Produces: `BreederFarmSite.register(siteCode, name, address, licenceNumber, biosecurityZone, managerName)` static factory (status defaults `ACTIVE`); `getSiteCode()`, `getName()`, `getStatus()`, etc.; `changeStatus(FarmSiteStatus newStatus)`. `BreederFarmSiteRepository extends JpaRepository<BreederFarmSite, UUID>` with `findBySiteCode(String)` → `Optional<BreederFarmSite>`, `findByStatus(FarmSiteStatus)` → `List<BreederFarmSite>`.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BreederFarmSiteTest {

    @Test
    void registerCreatesActiveSiteWithGivenFields() {
        BreederFarmSite site = BreederFarmSite.register(
                "BFS-001", "North Site", "123 Farm Rd", "LIC-99", "Zone A", "Jane Manager");

        assertThat(site.getSiteCode()).isEqualTo("BFS-001");
        assertThat(site.getName()).isEqualTo("North Site");
        assertThat(site.getStatus()).isEqualTo(FarmSiteStatus.ACTIVE);
    }

    @Test
    void changeStatusUpdatesStatus() {
        BreederFarmSite site = BreederFarmSite.register(
                "BFS-002", "South Site", "456 Farm Rd", "LIC-100", "Zone B", "John Manager");

        site.changeStatus(FarmSiteStatus.INACTIVE);

        assertThat(site.getStatus()).isEqualTo(FarmSiteStatus.INACTIVE);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=BreederFarmSiteTest`
Expected: FAIL — compile error, `BreederFarmSite` doesn't exist yet.

- [ ] **Step 3: Write the entity**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "breeder_farm_sites")
@Getter
@NoArgsConstructor
public class BreederFarmSite extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String siteCode;

    @Column(nullable = false)
    private String name;

    private String address;
    private String licenceNumber;
    private String biosecurityZone;
    private String managerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmSiteStatus status;

    private BreederFarmSite(String siteCode, String name, String address, String licenceNumber,
                             String biosecurityZone, String managerName) {
        this.siteCode = siteCode;
        this.name = name;
        this.address = address;
        this.licenceNumber = licenceNumber;
        this.biosecurityZone = biosecurityZone;
        this.managerName = managerName;
        this.status = FarmSiteStatus.ACTIVE;
    }

    public static BreederFarmSite register(String siteCode, String name, String address,
                                            String licenceNumber, String biosecurityZone, String managerName) {
        return new BreederFarmSite(siteCode, name, address, licenceNumber, biosecurityZone, managerName);
    }

    public void changeStatus(FarmSiteStatus newStatus) {
        this.status = newStatus;
    }
}
```

- [ ] **Step 4: Write the repository**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.parentflock.domain.model.BreederFarmSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreederFarmSiteRepository extends JpaRepository<BreederFarmSite, UUID> {
    Optional<BreederFarmSite> findBySiteCode(String siteCode);

    List<BreederFarmSite> findByStatus(FarmSiteStatus status);
}
```

- [ ] **Step 5: Write the migration**

```sql
-- parent-flock-service/src/main/resources/db/migration/V3__create_farm_site_table.sql
CREATE TABLE breeder_farm_sites (
    id                UUID PRIMARY KEY,
    site_code         VARCHAR(50) NOT NULL UNIQUE,
    name              VARCHAR(200) NOT NULL,
    address           VARCHAR(500),
    licence_number    VARCHAR(100),
    biosecurity_zone  VARCHAR(100),
    manager_name      VARCHAR(200),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=BreederFarmSiteTest`
Expected: PASS (2 tests)

- [ ] **Step 7: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/BreederFarmSite.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/BreederFarmSiteRepository.java parent-flock-service/src/main/resources/db/migration/V3__create_farm_site_table.sql parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/BreederFarmSiteTest.java
git commit -m "feat(parent-flock-service): BreederFarmSite entity, repository, migration"
```

---

## Task 9: `ParentFlockHouse` + `HouseSection` entities, repositories, migration

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/ParentFlockHouse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/HouseSection.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockHouseRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/HouseSectionRepository.java`
- Create: `parent-flock-service/src/main/resources/db/migration/V4__create_house_tables.sql`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/ParentFlockHouseTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2), `HouseType`/`HouseStatus`/`HouseSectionType`/`VentilationType` (Task 7), `BusinessRuleViolationException` (Task 2).
- Produces: `ParentFlockHouse.register(houseCode, farmSiteId, houseType, henCapacity, cockCapacity, lengthM, widthM, ventilationType)` static factory (status defaults `EMPTY`); `changeStatus(HouseStatus target)` — throws `BusinessRuleViolationException` if `!status.canTransitionTo(target)`. `HouseSection.create(sectionCode, houseId, sectionType, birdCapacity, notes)` static factory. `ParentFlockHouseRepository` with `findByFarmSiteIdAndStatus`, `findByFarmSiteIdAndHouseType`, `findByHouseCode` → `Optional`. `HouseSectionRepository` with `findByHouseId`.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParentFlockHouseTest {

    @Test
    void registerCreatesEmptyHouse() {
        ParentFlockHouse house = ParentFlockHouse.register(
                "H-001", UUID.randomUUID(), HouseType.REARING, 5000, 500, 100f, 20f, VentilationType.TUNNEL);

        assertThat(house.getStatus()).isEqualTo(HouseStatus.EMPTY);
        assertThat(house.getHouseType()).isEqualTo(HouseType.REARING);
    }

    @Test
    void changeStatusFollowsValidTransition() {
        ParentFlockHouse house = ParentFlockHouse.register(
                "H-002", UUID.randomUUID(), HouseType.PRODUCTION, 5000, 500, 100f, 20f, VentilationType.TUNNEL);

        house.changeStatus(HouseStatus.OCCUPIED);

        assertThat(house.getStatus()).isEqualTo(HouseStatus.OCCUPIED);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        ParentFlockHouse house = ParentFlockHouse.register(
                "H-003", UUID.randomUUID(), HouseType.PRODUCTION, 5000, 500, 100f, 20f, VentilationType.TUNNEL);

        assertThatThrownBy(() -> house.changeStatus(HouseStatus.CLEANING))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("H-003");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=ParentFlockHouseTest`
Expected: FAIL — compile error, `ParentFlockHouse` doesn't exist yet.

- [ ] **Step 3: Write `ParentFlockHouse`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "parent_flock_houses")
@Getter
@NoArgsConstructor
public class ParentFlockHouse extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String houseCode;

    @Column(nullable = false)
    private UUID farmSiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseType houseType;

    @Column(nullable = false)
    private int henCapacity;

    private int cockCapacity;
    private Float lengthM;
    private Float widthM;

    @Enumerated(EnumType.STRING)
    private VentilationType ventilationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseStatus status;

    private ParentFlockHouse(String houseCode, UUID farmSiteId, HouseType houseType, int henCapacity,
                              int cockCapacity, Float lengthM, Float widthM, VentilationType ventilationType) {
        this.houseCode = houseCode;
        this.farmSiteId = farmSiteId;
        this.houseType = houseType;
        this.henCapacity = henCapacity;
        this.cockCapacity = cockCapacity;
        this.lengthM = lengthM;
        this.widthM = widthM;
        this.ventilationType = ventilationType;
        this.status = HouseStatus.EMPTY;
    }

    public static ParentFlockHouse register(String houseCode, UUID farmSiteId, HouseType houseType,
                                             int henCapacity, int cockCapacity, Float lengthM, Float widthM,
                                             VentilationType ventilationType) {
        return new ParentFlockHouse(houseCode, farmSiteId, houseType, henCapacity, cockCapacity,
                lengthM, widthM, ventilationType);
    }

    public void changeStatus(HouseStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(
                    "House " + houseCode + " cannot transition from " + status + " to " + target);
        }
        this.status = target;
    }
}
```

- [ ] **Step 4: Write `HouseSection`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.HouseSectionType;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "house_sections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"house_id", "section_code"}))
@Getter
@NoArgsConstructor
public class HouseSection extends BaseEntity {

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    @Column(nullable = false)
    private UUID houseId;

    @Enumerated(EnumType.STRING)
    private HouseSectionType sectionType;

    private int birdCapacity;
    private String notes;

    private HouseSection(String sectionCode, UUID houseId, HouseSectionType sectionType,
                          int birdCapacity, String notes) {
        this.sectionCode = sectionCode;
        this.houseId = houseId;
        this.sectionType = sectionType;
        this.birdCapacity = birdCapacity;
        this.notes = notes;
    }

    public static HouseSection create(String sectionCode, UUID houseId, HouseSectionType sectionType,
                                       int birdCapacity, String notes) {
        return new HouseSection(sectionCode, houseId, sectionType, birdCapacity, notes);
    }
}
```

- [ ] **Step 5: Write the repositories**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentFlockHouseRepository extends JpaRepository<ParentFlockHouse, UUID> {
    List<ParentFlockHouse> findByFarmSiteIdAndStatus(UUID farmSiteId, HouseStatus status);

    List<ParentFlockHouse> findByFarmSiteIdAndHouseType(UUID farmSiteId, HouseType houseType);

    List<ParentFlockHouse> findByFarmSiteId(UUID farmSiteId);

    Optional<ParentFlockHouse> findByHouseCode(String houseCode);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.model.HouseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HouseSectionRepository extends JpaRepository<HouseSection, UUID> {
    List<HouseSection> findByHouseId(UUID houseId);
}
```

- [ ] **Step 6: Write the migration**

```sql
-- parent-flock-service/src/main/resources/db/migration/V4__create_house_tables.sql
CREATE TABLE parent_flock_houses (
    id                 UUID PRIMARY KEY,
    house_code         VARCHAR(50) NOT NULL UNIQUE,
    farm_site_id       UUID NOT NULL,
    house_type         VARCHAR(20) NOT NULL,
    hen_capacity       INT NOT NULL,
    cock_capacity      INT NOT NULL DEFAULT 0,
    length_m           REAL,
    width_m            REAL,
    ventilation_type   VARCHAR(20),
    status             VARCHAR(20) NOT NULL DEFAULT 'EMPTY',
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_parent_flock_houses_farm_site_id ON parent_flock_houses(farm_site_id);

CREATE TABLE house_sections (
    id             UUID PRIMARY KEY,
    section_code   VARCHAR(50) NOT NULL,
    house_id       UUID NOT NULL,
    section_type   VARCHAR(20),
    bird_capacity  INT NOT NULL DEFAULT 0,
    notes          VARCHAR(500),
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_house_section_code UNIQUE (house_id, section_code)
);
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=ParentFlockHouseTest`
Expected: PASS (3 tests)

- [ ] **Step 8: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/ParentFlockHouse.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/HouseSection.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockHouseRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/HouseSectionRepository.java parent-flock-service/src/main/resources/db/migration/V4__create_house_tables.sql parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/ParentFlockHouseTest.java
git commit -m "feat(parent-flock-service): ParentFlockHouse/HouseSection entities, repositories, migration"
```

---

## Task 10: `ParentFlock` + `FlockBirdCountEvent` entities, repositories, migration

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/ParentFlock.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/FlockBirdCountEvent.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/FlockBirdCountEventRepository.java`
- Create: `parent-flock-service/src/main/resources/db/migration/V5__create_flock_tables.sql`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/ParentFlockTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2), `FlockPhaseStatus`/`FlockHealthStatus`/`FlockSourceType` (Task 7), `BusinessRuleViolationException` (Task 2).
- Produces: `ParentFlock.register(flockCode, farmSiteId, geneticLine, breed, entryDate, initialHenCount, initialCockCount, rearingHouseId, sourceType, supplierRef)` static factory (`phaseStatus=REARING`, `healthStatus=HEALTHY`, `currentHenCount=initialHenCount`, `currentCockCount=initialCockCount`); `ageInWeeks()` → `long`; `applyBirdCountChange(int henChange, int cockChange)` — throws `BusinessRuleViolationException` if either resulting count would go negative; `assertInRearingPhase()` — throws `BusinessRuleViolationException` if `phaseStatus != REARING`; `transferToProduction()` — throws `BusinessRuleViolationException` if not in `REARING`, else sets `phaseStatus = PRODUCTION`; `applyCulling(int hensCulled, int cocksCulled)` → `boolean` (`true` when both counts reach zero, and it also sets `phaseStatus`/`healthStatus` to `CULLED` in that case) — throws `BusinessRuleViolationException` if either culled count exceeds the current count. These five methods are exactly what `FlockService` (Task 15) calls — same names, same exception type, same "over-cull throws" / "full-cull returns true" contract.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.FlockHealthStatus;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParentFlockTest {

    private ParentFlock newFlock() {
        return ParentFlock.register("PF-001", UUID.randomUUID(), "Ross 308", "Broiler Breeder",
                LocalDate.now().minusWeeks(10), 5000, 500, UUID.randomUUID(),
                FlockSourceType.PURCHASED, "SUP-REF-1");
    }

    @Test
    void registerSetsRearingPhaseAndCurrentCountsFromInitial() {
        ParentFlock flock = newFlock();

        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.REARING);
        assertThat(flock.getHealthStatus()).isEqualTo(FlockHealthStatus.HEALTHY);
        assertThat(flock.getCurrentHenCount()).isEqualTo(5000);
        assertThat(flock.getCurrentCockCount()).isEqualTo(500);
    }

    @Test
    void ageInWeeksReflectsEntryDate() {
        ParentFlock flock = newFlock();

        assertThat(flock.ageInWeeks()).isEqualTo(10L);
    }

    @Test
    void applyBirdCountChangeUpdatesCounts() {
        ParentFlock flock = newFlock();

        flock.applyBirdCountChange(-10, -2);

        assertThat(flock.getCurrentHenCount()).isEqualTo(4990);
        assertThat(flock.getCurrentCockCount()).isEqualTo(498);
    }

    @Test
    void applyBirdCountChangeRejectsGoingNegative() {
        ParentFlock flock = newFlock();

        assertThatThrownBy(() -> flock.applyBirdCountChange(-5001, 0))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void transferToProductionRequiresRearingPhase() {
        ParentFlock flock = newFlock();
        flock.transferToProduction();

        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.PRODUCTION);
        assertThatThrownBy(flock::transferToProduction)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void applyCullingReducesCountsAndClosesFlockWhenZero() {
        ParentFlock flock = newFlock();

        boolean partiallyClosed = flock.applyCulling(4000, 400);
        assertThat(partiallyClosed).isFalse();
        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.REARING);

        boolean fullyClosed = flock.applyCulling(1000, 100);
        assertThat(fullyClosed).isTrue();
        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.CULLED);
        assertThat(flock.getHealthStatus()).isEqualTo(FlockHealthStatus.CULLED);
        assertThat(flock.getCurrentHenCount()).isZero();
        assertThat(flock.getCurrentCockCount()).isZero();
    }

    @Test
    void applyCullingRejectsCullingMoreThanCurrentCount() {
        ParentFlock flock = newFlock();

        assertThatThrownBy(() -> flock.applyCulling(5001, 0))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void assertInRearingPhaseThrowsOncePhaseIsProduction() {
        ParentFlock flock = newFlock();
        flock.transferToProduction();

        assertThatThrownBy(flock::assertInRearingPhase)
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=ParentFlockTest`
Expected: FAIL — compile error, `ParentFlock` doesn't exist yet.

- [ ] **Step 3: Write `ParentFlock`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.FlockHealthStatus;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "parent_flocks")
@Getter
@NoArgsConstructor
public class ParentFlock extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String flockCode;

    @Column(nullable = false)
    private UUID farmSiteId;

    private String geneticLine;
    private String breed;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlockPhaseStatus phaseStatus;

    @Column(nullable = false)
    private int currentHenCount;

    private int currentCockCount;

    @Column(nullable = false)
    private int initialHenCount;

    private int initialCockCount;

    @Column(nullable = false)
    private UUID rearingHouseId;

    @Enumerated(EnumType.STRING)
    private FlockHealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    private FlockSourceType sourceType;

    private String supplierRef;

    private ParentFlock(String flockCode, UUID farmSiteId, String geneticLine, String breed,
                         LocalDate entryDate, int initialHenCount, int initialCockCount,
                         UUID rearingHouseId, FlockSourceType sourceType, String supplierRef) {
        this.flockCode = flockCode;
        this.farmSiteId = farmSiteId;
        this.geneticLine = geneticLine;
        this.breed = breed;
        this.entryDate = entryDate;
        this.initialHenCount = initialHenCount;
        this.initialCockCount = initialCockCount;
        this.currentHenCount = initialHenCount;
        this.currentCockCount = initialCockCount;
        this.rearingHouseId = rearingHouseId;
        this.sourceType = sourceType;
        this.supplierRef = supplierRef;
        this.phaseStatus = FlockPhaseStatus.REARING;
        this.healthStatus = FlockHealthStatus.HEALTHY;
    }

    public static ParentFlock register(String flockCode, UUID farmSiteId, String geneticLine, String breed,
                                        LocalDate entryDate, int initialHenCount, int initialCockCount,
                                        UUID rearingHouseId, FlockSourceType sourceType, String supplierRef) {
        return new ParentFlock(flockCode, farmSiteId, geneticLine, breed, entryDate, initialHenCount,
                initialCockCount, rearingHouseId, sourceType, supplierRef);
    }

    public long ageInWeeks() {
        return ChronoUnit.WEEKS.between(entryDate, LocalDate.now());
    }

    public void assertInRearingPhase() {
        if (phaseStatus != FlockPhaseStatus.REARING) {
            throw new BusinessRuleViolationException(
                    "Flock " + flockCode + " is not in REARING phase (current: " + phaseStatus + ")");
        }
    }

    public void applyBirdCountChange(int henChange, int cockChange) {
        int newHenCount = currentHenCount + henChange;
        int newCockCount = currentCockCount + cockChange;
        if (newHenCount < 0 || newCockCount < 0) {
            throw new BusinessRuleViolationException(
                    "Bird count change would make counts negative for flock " + flockCode);
        }
        this.currentHenCount = newHenCount;
        this.currentCockCount = newCockCount;
    }

    public void transferToProduction() {
        assertInRearingPhase();
        this.phaseStatus = FlockPhaseStatus.PRODUCTION;
    }

    /**
     * @return true if this culling reduced both counts to zero, closing the flock.
     */
    public boolean applyCulling(int hensCulled, int cocksCulled) {
        if (hensCulled > currentHenCount || cocksCulled > currentCockCount) {
            throw new BusinessRuleViolationException(
                    "Cannot cull more birds than currently present in flock " + flockCode);
        }
        this.currentHenCount -= hensCulled;
        this.currentCockCount -= cocksCulled;
        if (currentHenCount == 0 && currentCockCount == 0) {
            this.phaseStatus = FlockPhaseStatus.CULLED;
            this.healthStatus = FlockHealthStatus.CULLED;
            return true;
        }
        return false;
    }
}
```

- [ ] **Step 4: Write `FlockBirdCountEvent`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.MortalityCause;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "flock_bird_count_events")
@Getter
@NoArgsConstructor
public class FlockBirdCountEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID flockId;

    @Column(nullable = false)
    private LocalDate eventDate;

    private int henChange;
    private int cockChange;

    @Enumerated(EnumType.STRING)
    private MortalityCause cause;

    private String causeNotes;
    private UUID recordedBy;

    private FlockBirdCountEvent(UUID flockId, LocalDate eventDate, int henChange, int cockChange,
                                 MortalityCause cause, String causeNotes, UUID recordedBy) {
        this.flockId = flockId;
        this.eventDate = eventDate;
        this.henChange = henChange;
        this.cockChange = cockChange;
        this.cause = cause;
        this.causeNotes = causeNotes;
        this.recordedBy = recordedBy;
    }

    public static FlockBirdCountEvent record(UUID flockId, LocalDate eventDate, int henChange, int cockChange,
                                              MortalityCause cause, String causeNotes, UUID recordedBy) {
        return new FlockBirdCountEvent(flockId, eventDate, henChange, cockChange, cause, causeNotes, recordedBy);
    }
}
```

- [ ] **Step 5: Write the repositories**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.model.ParentFlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentFlockRepository extends JpaRepository<ParentFlock, UUID>,
        JpaSpecificationExecutor<ParentFlock> {
    List<ParentFlock> findByFarmSiteIdAndPhaseStatus(UUID farmSiteId, FlockPhaseStatus phaseStatus);

    List<ParentFlock> findByRearingHouseId(UUID rearingHouseId);

    Optional<ParentFlock> findByFlockCode(String flockCode);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.model.FlockBirdCountEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlockBirdCountEventRepository extends JpaRepository<FlockBirdCountEvent, UUID> {
    List<FlockBirdCountEvent> findByFlockIdOrderByEventDateDesc(UUID flockId);
}
```

- [ ] **Step 6: Write the migration**

```sql
-- parent-flock-service/src/main/resources/db/migration/V5__create_flock_tables.sql
CREATE TABLE parent_flocks (
    id                 UUID PRIMARY KEY,
    flock_code         VARCHAR(50) NOT NULL UNIQUE,
    farm_site_id       UUID NOT NULL,
    genetic_line       VARCHAR(100),
    breed              VARCHAR(100),
    entry_date         DATE NOT NULL,
    phase_status       VARCHAR(20) NOT NULL DEFAULT 'REARING',
    current_hen_count  INT NOT NULL,
    current_cock_count INT NOT NULL DEFAULT 0,
    initial_hen_count  INT NOT NULL,
    initial_cock_count INT NOT NULL DEFAULT 0,
    rearing_house_id   UUID NOT NULL,
    health_status      VARCHAR(20) DEFAULT 'HEALTHY',
    source_type        VARCHAR(20),
    supplier_ref       VARCHAR(200),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_parent_flocks_farm_site_id ON parent_flocks(farm_site_id);
CREATE INDEX idx_parent_flocks_rearing_house_id ON parent_flocks(rearing_house_id);

CREATE TABLE flock_bird_count_events (
    id           UUID PRIMARY KEY,
    flock_id     UUID NOT NULL,
    event_date   DATE NOT NULL,
    hen_change   INT NOT NULL DEFAULT 0,
    cock_change  INT NOT NULL DEFAULT 0,
    cause        VARCHAR(20),
    cause_notes  VARCHAR(500),
    recorded_by  UUID,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_flock_bird_count_events_flock_id ON flock_bird_count_events(flock_id);
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=ParentFlockTest`
Expected: PASS (8 tests)

- [ ] **Step 8: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/ParentFlock.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/FlockBirdCountEvent.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/FlockBirdCountEventRepository.java parent-flock-service/src/main/resources/db/migration/V5__create_flock_tables.sql parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/ParentFlockTest.java
git commit -m "feat(parent-flock-service): ParentFlock/FlockBirdCountEvent entities with lifecycle behavior, repositories, migration"
```

---

## Task 11: `RearingWeightSample`, `RearingToProductionTransfer`/`TransferDestination`, `FlockCullingRecord` — entities, repositories, migration

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/RearingWeightSample.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/RearingToProductionTransfer.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/TransferDestination.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/FlockCullingRecord.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RearingWeightSampleRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RearingToProductionTransferRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/FlockCullingRecordRepository.java`
- Create: `parent-flock-service/src/main/resources/db/migration/V6__create_transfer_and_weight_tables.sql`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/RearingWeightSampleTest.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/RearingToProductionTransferTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2).
- Produces: `RearingWeightSample.record(flockId, weekOfAge, sampleDate, sampleSize, totalWeightG, targetWeightG, uniformityPercent, recordedBy)` — computes and stores `averageWeightG = totalWeightG / sampleSize`. `RearingToProductionTransfer.create(flockId, transferDate, rearingHouseId, bodyWeightAtTransferG, authorisedBy)`, instance method `addDestination(UUID productionHouseId, int henCount, int cockCount)` (appends a `TransferDestination`, returns `this` for chaining), `getDestinations()` → `List<TransferDestination>`, `totalTransferredHens()`/`totalTransferredCocks()` → `int`. `TransferDestination.getProductionHouseId()`/`getTransferredHenCount()`/`getTransferredCockCount()`. `FlockCullingRecord.record(flockId, cullingDate, hensCulled, cocksCulled, reason, disposalMethod, authorisedBy, notes)`. `FlockService` (Task 15) depends on all of these exact names.

- [ ] **Step 1: Write the failing tests**

```java
package com.ppms.parentflock.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RearingWeightSampleTest {

    @Test
    void recordComputesAverageWeightFromTotalAndSampleSize() {
        RearingWeightSample sample = RearingWeightSample.record(
                UUID.randomUUID(), 10, LocalDate.now(), 50, 25_000f, 500f, 92.5f, UUID.randomUUID());

        assertThat(sample.getAverageWeightG()).isEqualTo(500f);
        assertThat(sample.getSampleSize()).isEqualTo(50);
    }
}
```

```java
package com.ppms.parentflock.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RearingToProductionTransferTest {

    @Test
    void addDestinationAccumulatesTotals() {
        RearingToProductionTransfer transfer = RearingToProductionTransfer.create(
                UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), 1750f, UUID.randomUUID());

        transfer.addDestination(UUID.randomUUID(), 2500, 250);
        transfer.addDestination(UUID.randomUUID(), 2500, 250);

        assertThat(transfer.getDestinations()).hasSize(2);
        assertThat(transfer.totalTransferredHens()).isEqualTo(5000);
        assertThat(transfer.totalTransferredCocks()).isEqualTo(500);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=RearingWeightSampleTest,RearingToProductionTransferTest`
Expected: FAIL — compile error, classes don't exist yet.

- [ ] **Step 3: Write `RearingWeightSample`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rearing_weight_samples")
@Getter
@NoArgsConstructor
public class RearingWeightSample extends BaseEntity {

    @Column(nullable = false)
    private UUID flockId;

    @Column(nullable = false)
    private int weekOfAge;

    @Column(nullable = false)
    private LocalDate sampleDate;

    private int sampleSize;
    private float totalWeightG;
    private float averageWeightG;
    private float targetWeightG;
    private float uniformityPercent;
    private UUID recordedBy;

    private RearingWeightSample(UUID flockId, int weekOfAge, LocalDate sampleDate, int sampleSize,
                                 float totalWeightG, float targetWeightG, float uniformityPercent,
                                 UUID recordedBy) {
        this.flockId = flockId;
        this.weekOfAge = weekOfAge;
        this.sampleDate = sampleDate;
        this.sampleSize = sampleSize;
        this.totalWeightG = totalWeightG;
        this.averageWeightG = sampleSize == 0 ? 0f : totalWeightG / sampleSize;
        this.targetWeightG = targetWeightG;
        this.uniformityPercent = uniformityPercent;
        this.recordedBy = recordedBy;
    }

    public static RearingWeightSample record(UUID flockId, int weekOfAge, LocalDate sampleDate, int sampleSize,
                                              float totalWeightG, float targetWeightG, float uniformityPercent,
                                              UUID recordedBy) {
        return new RearingWeightSample(flockId, weekOfAge, sampleDate, sampleSize, totalWeightG,
                targetWeightG, uniformityPercent, recordedBy);
    }
}
```

- [ ] **Step 4: Write `RearingToProductionTransfer` and `TransferDestination`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rearing_to_production_transfers")
@Getter
@NoArgsConstructor
public class RearingToProductionTransfer extends BaseEntity {

    @Column(nullable = false)
    private UUID flockId;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false)
    private UUID rearingHouseId;

    private Float bodyWeightAtTransferG;
    private UUID authorisedBy;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferDestination> destinations = new ArrayList<>();

    private RearingToProductionTransfer(UUID flockId, LocalDate transferDate, UUID rearingHouseId,
                                         Float bodyWeightAtTransferG, UUID authorisedBy) {
        this.flockId = flockId;
        this.transferDate = transferDate;
        this.rearingHouseId = rearingHouseId;
        this.bodyWeightAtTransferG = bodyWeightAtTransferG;
        this.authorisedBy = authorisedBy;
    }

    public static RearingToProductionTransfer create(UUID flockId, LocalDate transferDate, UUID rearingHouseId,
                                                       Float bodyWeightAtTransferG, UUID authorisedBy) {
        return new RearingToProductionTransfer(flockId, transferDate, rearingHouseId,
                bodyWeightAtTransferG, authorisedBy);
    }

    public RearingToProductionTransfer addDestination(UUID productionHouseId, int henCount, int cockCount) {
        destinations.add(TransferDestination.of(this, productionHouseId, henCount, cockCount));
        return this;
    }

    public int totalTransferredHens() {
        return destinations.stream().mapToInt(TransferDestination::getTransferredHenCount).sum();
    }

    public int totalTransferredCocks() {
        return destinations.stream().mapToInt(TransferDestination::getTransferredCockCount).sum();
    }
}
```

```java
package com.ppms.parentflock.domain.model;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "transfer_destinations")
@Getter
@NoArgsConstructor
public class TransferDestination extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private RearingToProductionTransfer transfer;

    @Column(nullable = false)
    private UUID productionHouseId;

    private int transferredHenCount;
    private int transferredCockCount;

    private TransferDestination(RearingToProductionTransfer transfer, UUID productionHouseId,
                                 int transferredHenCount, int transferredCockCount) {
        this.transfer = transfer;
        this.productionHouseId = productionHouseId;
        this.transferredHenCount = transferredHenCount;
        this.transferredCockCount = transferredCockCount;
    }

    static TransferDestination of(RearingToProductionTransfer transfer, UUID productionHouseId,
                                   int henCount, int cockCount) {
        return new TransferDestination(transfer, productionHouseId, henCount, cockCount);
    }
}
```

- [ ] **Step 5: Write `FlockCullingRecord`**

```java
package com.ppms.parentflock.domain.model;

import com.ppms.parentflock.domain.enums.CullingReason;
import com.ppms.parentflock.domain.enums.DisposalMethod;
import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "flock_culling_records")
@Getter
@NoArgsConstructor
public class FlockCullingRecord extends BaseEntity {

    @Column(nullable = false)
    private UUID flockId;

    @Column(nullable = false)
    private LocalDate cullingDate;

    private int hensCulled;
    private int cocksCulled;

    @Enumerated(EnumType.STRING)
    private CullingReason reason;

    @Enumerated(EnumType.STRING)
    private DisposalMethod disposalMethod;

    private UUID authorisedBy;
    private String notes;

    private FlockCullingRecord(UUID flockId, LocalDate cullingDate, int hensCulled, int cocksCulled,
                                CullingReason reason, DisposalMethod disposalMethod, UUID authorisedBy,
                                String notes) {
        this.flockId = flockId;
        this.cullingDate = cullingDate;
        this.hensCulled = hensCulled;
        this.cocksCulled = cocksCulled;
        this.reason = reason;
        this.disposalMethod = disposalMethod;
        this.authorisedBy = authorisedBy;
        this.notes = notes;
    }

    public static FlockCullingRecord record(UUID flockId, LocalDate cullingDate, int hensCulled, int cocksCulled,
                                             CullingReason reason, DisposalMethod disposalMethod,
                                             UUID authorisedBy, String notes) {
        return new FlockCullingRecord(flockId, cullingDate, hensCulled, cocksCulled, reason,
                disposalMethod, authorisedBy, notes);
    }
}
```

- [ ] **Step 6: Write the repositories**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.model.RearingWeightSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RearingWeightSampleRepository extends JpaRepository<RearingWeightSample, UUID> {
    List<RearingWeightSample> findByFlockIdOrderByWeekOfAge(UUID flockId);

    Optional<RearingWeightSample> findByFlockIdAndWeekOfAge(UUID flockId, int weekOfAge);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.model.RearingToProductionTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RearingToProductionTransferRepository extends JpaRepository<RearingToProductionTransfer, UUID> {
    List<RearingToProductionTransfer> findByFlockId(UUID flockId);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.model.FlockCullingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlockCullingRecordRepository extends JpaRepository<FlockCullingRecord, UUID> {
    List<FlockCullingRecord> findByFlockId(UUID flockId);
}
```

- [ ] **Step 7: Write the migration**

```sql
-- parent-flock-service/src/main/resources/db/migration/V6__create_transfer_and_weight_tables.sql
CREATE TABLE rearing_weight_samples (
    id                  UUID PRIMARY KEY,
    flock_id            UUID NOT NULL,
    week_of_age         INT NOT NULL,
    sample_date         DATE NOT NULL,
    sample_size         INT NOT NULL DEFAULT 0,
    total_weight_g      REAL NOT NULL DEFAULT 0,
    average_weight_g    REAL NOT NULL DEFAULT 0,
    target_weight_g     REAL,
    uniformity_percent  REAL,
    recorded_by         UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rearing_weight_samples_flock_id ON rearing_weight_samples(flock_id);

CREATE TABLE rearing_to_production_transfers (
    id                        UUID PRIMARY KEY,
    flock_id                  UUID NOT NULL,
    transfer_date             DATE NOT NULL,
    rearing_house_id          UUID NOT NULL,
    body_weight_at_transfer_g REAL,
    authorised_by             UUID,
    created_at                TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rearing_transfers_flock_id ON rearing_to_production_transfers(flock_id);

CREATE TABLE transfer_destinations (
    id                     UUID PRIMARY KEY,
    transfer_id            UUID NOT NULL REFERENCES rearing_to_production_transfers(id),
    production_house_id    UUID NOT NULL,
    transferred_hen_count  INT NOT NULL DEFAULT 0,
    transferred_cock_count INT NOT NULL DEFAULT 0,
    created_at             TIMESTAMP NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transfer_destinations_transfer_id ON transfer_destinations(transfer_id);

CREATE TABLE flock_culling_records (
    id               UUID PRIMARY KEY,
    flock_id         UUID NOT NULL,
    culling_date     DATE NOT NULL,
    hens_culled      INT NOT NULL DEFAULT 0,
    cocks_culled     INT NOT NULL DEFAULT 0,
    reason           VARCHAR(20),
    disposal_method  VARCHAR(20),
    authorised_by    UUID,
    notes            VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_flock_culling_records_flock_id ON flock_culling_records(flock_id);
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=RearingWeightSampleTest,RearingToProductionTransferTest`
Expected: PASS (2 tests)

- [ ] **Step 9: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/RearingWeightSample.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/RearingToProductionTransfer.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/TransferDestination.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/model/FlockCullingRecord.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RearingWeightSampleRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RearingToProductionTransferRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/FlockCullingRecordRepository.java parent-flock-service/src/main/resources/db/migration/V6__create_transfer_and_weight_tables.sql parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/RearingWeightSampleTest.java parent-flock-service/src/test/java/com/ppms/parentflock/domain/model/RearingToProductionTransferTest.java
git commit -m "feat(parent-flock-service): RearingWeightSample/RearingToProductionTransfer/FlockCullingRecord entities, repositories, migration"
```

---

## Task 12: Farm Site — DTOs, mapper, service (TDD), controller

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/CreateFarmSiteRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/UpdateFarmSiteStatusRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/FarmSiteResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/FarmSiteMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/service/FarmSiteService.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/FarmSiteController.java`
- Modify: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/BreederFarmSiteRepository.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/application/service/FarmSiteServiceTest.java`

**Interfaces:**
- Consumes: `BreederFarmSite`, `BreederFarmSiteRepository` (Task 8), `FarmSiteStatus` (Task 7), `EntityNotFoundException`/`BusinessRuleViolationException`/`PageResponse` (Task 2).
- Produces: `FarmSiteService.createFarmSite(CreateFarmSiteRequest)` → `FarmSiteResponse`; `updateFarmSiteStatus(UUID id, FarmSiteStatus status)` → `FarmSiteResponse`; `getFarmSite(UUID id)` → `FarmSiteResponse`; `listFarmSites(FarmSiteStatus statusFilter, Pageable pageable)` → `PageResponse<FarmSiteResponse>`. Endpoints under `/api/v1/farm-sites`.

- [ ] **Step 1: Add the pageable status-filter query to the repository**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.parentflock.domain.model.BreederFarmSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreederFarmSiteRepository extends JpaRepository<BreederFarmSite, UUID> {
    Optional<BreederFarmSite> findBySiteCode(String siteCode);

    List<BreederFarmSite> findByStatus(FarmSiteStatus status);

    Page<BreederFarmSite> findByStatus(FarmSiteStatus status, Pageable pageable);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateFarmSiteRequest;
import com.ppms.parentflock.application.dto.response.FarmSiteResponse;
import com.ppms.parentflock.application.mapper.FarmSiteMapper;
import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.parentflock.domain.model.BreederFarmSite;
import com.ppms.parentflock.domain.repository.BreederFarmSiteRepository;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FarmSiteServiceTest {

    @Mock
    private BreederFarmSiteRepository repository;

    @InjectMocks
    private FarmSiteService service;

    private final FarmSiteMapper mapper = new FarmSiteMapperImpl();

    @Test
    void createFarmSiteSavesWhenSiteCodeIsUnique() {
        service = new FarmSiteService(repository, mapper);
        CreateFarmSiteRequest request = new CreateFarmSiteRequest(
                "BFS-010", "East Site", "1 Road", "LIC-1", "Zone C", "Manager X");
        when(repository.findBySiteCode("BFS-010")).thenReturn(Optional.empty());
        when(repository.save(any(BreederFarmSite.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmSiteResponse response = service.createFarmSite(request);

        assertThat(response.siteCode()).isEqualTo("BFS-010");
        assertThat(response.status()).isEqualTo(FarmSiteStatus.ACTIVE);
        verify(repository).save(any(BreederFarmSite.class));
    }

    @Test
    void createFarmSiteRejectsDuplicateSiteCode() {
        service = new FarmSiteService(repository, mapper);
        CreateFarmSiteRequest request = new CreateFarmSiteRequest(
                "BFS-010", "East Site", "1 Road", "LIC-1", "Zone C", "Manager X");
        when(repository.findBySiteCode("BFS-010"))
                .thenReturn(Optional.of(BreederFarmSite.register("BFS-010", "x", null, null, null, null)));

        assertThatThrownBy(() -> service.createFarmSite(request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void updateFarmSiteStatusThrowsWhenNotFound() {
        service = new FarmSiteService(repository, mapper);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFarmSiteStatus(id, FarmSiteStatus.INACTIVE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateFarmSiteStatusUpdatesAndSaves() {
        service = new FarmSiteService(repository, mapper);
        UUID id = UUID.randomUUID();
        BreederFarmSite existing = BreederFarmSite.register("BFS-020", "Y", null, null, null, null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(BreederFarmSite.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmSiteResponse response = service.updateFarmSiteStatus(id, FarmSiteStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(FarmSiteStatus.INACTIVE);
    }

    @Test
    void getFarmSiteThrowsWhenNotFound() {
        service = new FarmSiteService(repository, mapper);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFarmSite(id)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listFarmSitesUsesStatusFilterWhenProvided() {
        service = new FarmSiteService(repository, mapper);
        var pageable = PageRequest.of(0, 20);
        when(repository.findByStatus(FarmSiteStatus.ACTIVE, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.listFarmSites(FarmSiteStatus.ACTIVE, pageable);

        verify(repository).findByStatus(FarmSiteStatus.ACTIVE, pageable);
    }
}
```

This test references `FarmSiteMapperImpl` (the MapStruct-generated implementation) directly rather than mocking the mapper — mapping a small entity to a record is cheap, real behavior worth exercising for real, and avoids a brittle mock of generated code. It won't compile until Steps 3–4 exist.

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=FarmSiteServiceTest`
Expected: FAIL — compile error, none of the DTOs/mapper/service exist yet.

- [ ] **Step 4: Write the DTOs**

```java
package com.ppms.parentflock.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateFarmSiteRequest(
        @NotBlank String siteCode,
        @NotBlank String name,
        String address,
        String licenceNumber,
        String biosecurityZone,
        String managerName
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFarmSiteStatusRequest(@NotNull FarmSiteStatus status) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.FarmSiteStatus;

import java.util.UUID;

public record FarmSiteResponse(
        UUID id,
        String siteCode,
        String name,
        String address,
        String licenceNumber,
        String biosecurityZone,
        String managerName,
        FarmSiteStatus status
) {
}
```

- [ ] **Step 5: Write the mapper**

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.FarmSiteResponse;
import com.ppms.parentflock.domain.model.BreederFarmSite;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FarmSiteMapper {
    FarmSiteResponse toResponse(BreederFarmSite entity);
}
```

- [ ] **Step 6: Write the service**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateFarmSiteRequest;
import com.ppms.parentflock.application.dto.response.FarmSiteResponse;
import com.ppms.parentflock.application.mapper.FarmSiteMapper;
import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.parentflock.domain.model.BreederFarmSite;
import com.ppms.parentflock.domain.repository.BreederFarmSiteRepository;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FarmSiteService {

    private final BreederFarmSiteRepository repository;
    private final FarmSiteMapper mapper;

    public FarmSiteResponse createFarmSite(CreateFarmSiteRequest request) {
        repository.findBySiteCode(request.siteCode()).ifPresent(existing -> {
            throw new BusinessRuleViolationException("Farm site code already in use: " + request.siteCode());
        });
        BreederFarmSite site = BreederFarmSite.register(request.siteCode(), request.name(), request.address(),
                request.licenceNumber(), request.biosecurityZone(), request.managerName());
        return mapper.toResponse(repository.save(site));
    }

    public FarmSiteResponse updateFarmSiteStatus(UUID id, FarmSiteStatus status) {
        BreederFarmSite site = getOrThrow(id);
        site.changeStatus(status);
        return mapper.toResponse(repository.save(site));
    }

    @Transactional(readOnly = true)
    public FarmSiteResponse getFarmSite(UUID id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<FarmSiteResponse> listFarmSites(FarmSiteStatus statusFilter, Pageable pageable) {
        Page<BreederFarmSite> page = statusFilter != null
                ? repository.findByStatus(statusFilter, pageable)
                : repository.findAll(pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    private BreederFarmSite getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> EntityNotFoundException.of("BreederFarmSite", id));
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=FarmSiteServiceTest`
Expected: PASS (6 tests). MapStruct generates `FarmSiteMapperImpl` at compile time via the annotation processor already wired in the root `pom.xml`'s `pluginManagement` — no manual step needed, but if this fails with "cannot find symbol: class FarmSiteMapperImpl", run `./mvnw -q -pl parent-flock-service -am clean compile` first to force annotation processing to re-run.

- [ ] **Step 8: Write the controller**

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.CreateFarmSiteRequest;
import com.ppms.parentflock.application.dto.request.UpdateFarmSiteStatusRequest;
import com.ppms.parentflock.application.dto.response.FarmSiteResponse;
import com.ppms.parentflock.application.service.FarmSiteService;
import com.ppms.parentflock.domain.enums.FarmSiteStatus;
import com.ppms.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farm-sites")
@RequiredArgsConstructor
public class FarmSiteController {

    private final FarmSiteService farmSiteService;

    @PostMapping
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public ResponseEntity<FarmSiteResponse> create(@Valid @RequestBody CreateFarmSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farmSiteService.createFarmSite(request));
    }

    @GetMapping
    public PageResponse<FarmSiteResponse> list(
            @RequestParam(required = false) FarmSiteStatus status, Pageable pageable) {
        return farmSiteService.listFarmSites(status, pageable);
    }

    @GetMapping("/{id}")
    public FarmSiteResponse get(@PathVariable UUID id) {
        return farmSiteService.getFarmSite(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public FarmSiteResponse updateStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateFarmSiteStatusRequest request) {
        return farmSiteService.updateFarmSiteStatus(id, request.status());
    }
}
```

- [ ] **Step 9: Compile check**

Run: `./mvnw -q -pl parent-flock-service -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 10: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/application parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/FarmSiteController.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/BreederFarmSiteRepository.java parent-flock-service/src/test/java/com/ppms/parentflock/application/service/FarmSiteServiceTest.java
git commit -m "feat(parent-flock-service): Farm Site DTOs, mapper, service, controller"
```

---

## Task 13: House / House Section — DTOs, mapper, service (TDD), controllers

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/CreateHouseRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/ChangeHouseStatusRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/CreateHouseSectionRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/HouseResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/HouseSectionResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/HouseDetailResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/HouseMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/HouseSectionMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/service/HouseService.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/HouseController.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/HouseSectionController.java`
- Modify: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockHouseRepository.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/application/service/HouseServiceTest.java`

**Interfaces:**
- Consumes: `ParentFlockHouse`, `HouseSection`, `ParentFlockHouseRepository`, `HouseSectionRepository` (Task 9), `BreederFarmSiteRepository` (Task 8), `HouseType`/`HouseStatus`/`HouseSectionType`/`VentilationType` (Task 7).
- Produces: `HouseService.registerHouse(UUID farmSiteId, CreateHouseRequest)` → `HouseResponse`; `addSection(UUID houseId, CreateHouseSectionRequest)` → `HouseSectionResponse`; `changeHouseStatus(UUID houseId, HouseStatus newStatus, String reason)` → `HouseResponse`; `getHouseWithSections(UUID houseId)` → `HouseDetailResponse`; `listHouses(UUID farmSiteId, HouseStatus status, HouseType type, Pageable pageable)` → `PageResponse<HouseResponse>`.

- [ ] **Step 1: Add `JpaSpecificationExecutor` to the house repository**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentFlockHouseRepository extends JpaRepository<ParentFlockHouse, UUID>,
        JpaSpecificationExecutor<ParentFlockHouse> {
    List<ParentFlockHouse> findByFarmSiteIdAndStatus(UUID farmSiteId, HouseStatus status);

    List<ParentFlockHouse> findByFarmSiteIdAndHouseType(UUID farmSiteId, HouseType houseType);

    List<ParentFlockHouse> findByFarmSiteId(UUID farmSiteId);

    Optional<ParentFlockHouse> findByHouseCode(String houseCode);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateHouseRequest;
import com.ppms.parentflock.application.dto.request.CreateHouseSectionRequest;
import com.ppms.parentflock.application.dto.response.HouseDetailResponse;
import com.ppms.parentflock.application.dto.response.HouseResponse;
import com.ppms.parentflock.application.dto.response.HouseSectionResponse;
import com.ppms.parentflock.application.mapper.HouseMapperImpl;
import com.ppms.parentflock.application.mapper.HouseSectionMapperImpl;
import com.ppms.parentflock.domain.enums.HouseSectionType;
import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;
import com.ppms.parentflock.domain.model.HouseSection;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import com.ppms.parentflock.domain.repository.BreederFarmSiteRepository;
import com.ppms.parentflock.domain.repository.HouseSectionRepository;
import com.ppms.parentflock.domain.repository.ParentFlockHouseRepository;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private ParentFlockHouseRepository houseRepository;

    @Mock
    private HouseSectionRepository sectionRepository;

    @Mock
    private BreederFarmSiteRepository farmSiteRepository;

    private HouseService service;

    private HouseService service() {
        return new HouseService(houseRepository, sectionRepository, farmSiteRepository,
                new HouseMapperImpl(), new HouseSectionMapperImpl());
    }

    private CreateHouseRequest sampleRequest() {
        return new CreateHouseRequest("H-100", HouseType.REARING, 5000, 500, 100f, 20f, VentilationType.TUNNEL);
    }

    @Test
    void registerHouseThrowsWhenFarmSiteNotFound() {
        service = service();
        UUID farmSiteId = UUID.randomUUID();
        when(farmSiteRepository.findById(farmSiteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerHouse(farmSiteId, sampleRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void registerHouseThrowsWhenHouseCodeAlreadyExists() {
        service = service();
        UUID farmSiteId = UUID.randomUUID();
        when(farmSiteRepository.findById(farmSiteId)).thenReturn(Optional.of(
                com.ppms.parentflock.domain.model.BreederFarmSite.register("BFS-1", "N", null, null, null, null)));
        when(houseRepository.findByHouseCode("H-100")).thenReturn(Optional.of(
                ParentFlockHouse.register("H-100", farmSiteId, HouseType.REARING, 1, 1, null, null, null)));

        assertThatThrownBy(() -> service.registerHouse(farmSiteId, sampleRequest()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void registerHouseSavesSuccessfully() {
        service = service();
        UUID farmSiteId = UUID.randomUUID();
        when(farmSiteRepository.findById(farmSiteId)).thenReturn(Optional.of(
                com.ppms.parentflock.domain.model.BreederFarmSite.register("BFS-1", "N", null, null, null, null)));
        when(houseRepository.findByHouseCode("H-100")).thenReturn(Optional.empty());
        when(houseRepository.save(any(ParentFlockHouse.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseResponse response = service.registerHouse(farmSiteId, sampleRequest());

        assertThat(response.houseCode()).isEqualTo("H-100");
        assertThat(response.status()).isEqualTo(HouseStatus.EMPTY);
    }

    @Test
    void addSectionThrowsWhenHouseNotFound() {
        service = service();
        UUID houseId = UUID.randomUUID();
        when(houseRepository.findById(houseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSection(houseId,
                new CreateHouseSectionRequest("S1", HouseSectionType.NEST_ROW, 100, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addSectionThrowsWhenSectionCodeAlreadyUsedInHouse() {
        service = service();
        UUID houseId = UUID.randomUUID();
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(
                ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING, 1, 1, null, null, null)));
        when(sectionRepository.findByHouseId(houseId)).thenReturn(
                List.of(HouseSection.create("S1", houseId, HouseSectionType.NEST_ROW, 50, null)));

        assertThatThrownBy(() -> service.addSection(houseId,
                new CreateHouseSectionRequest("S1", HouseSectionType.NEST_ROW, 100, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void addSectionSavesSuccessfully() {
        service = service();
        UUID houseId = UUID.randomUUID();
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(
                ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING, 1, 1, null, null, null)));
        when(sectionRepository.findByHouseId(houseId)).thenReturn(List.of());
        when(sectionRepository.save(any(HouseSection.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseSectionResponse response = service.addSection(houseId,
                new CreateHouseSectionRequest("S1", HouseSectionType.NEST_ROW, 100, "note"));

        assertThat(response.sectionCode()).isEqualTo("S1");
    }

    @Test
    void changeHouseStatusThrowsWhenHouseNotFound() {
        service = service();
        UUID houseId = UUID.randomUUID();
        when(houseRepository.findById(houseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeHouseStatus(houseId, HouseStatus.MAINTENANCE, "repair"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void changeHouseStatusAppliesValidTransition() {
        service = service();
        UUID houseId = UUID.randomUUID();
        ParentFlockHouse house = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                1, 1, null, null, null);
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
        when(houseRepository.save(any(ParentFlockHouse.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseResponse response = service.changeHouseStatus(houseId, HouseStatus.OCCUPIED, null);

        assertThat(response.status()).isEqualTo(HouseStatus.OCCUPIED);
    }

    @Test
    void getHouseWithSectionsReturnsHouseAndSections() {
        service = service();
        UUID houseId = UUID.randomUUID();
        ParentFlockHouse house = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                1, 1, null, null, null);
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
        when(sectionRepository.findByHouseId(houseId)).thenReturn(
                List.of(HouseSection.create("S1", houseId, HouseSectionType.NEST_ROW, 50, null)));

        HouseDetailResponse response = service.getHouseWithSections(houseId);

        assertThat(response.houseCode()).isEqualTo("H-1");
        assertThat(response.sections()).hasSize(1);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=HouseServiceTest`
Expected: FAIL — compile error, none of the DTOs/mapper/service exist yet.

- [ ] **Step 4: Write the DTOs**

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateHouseRequest(
        @NotBlank String houseCode,
        @NotNull HouseType houseType,
        @Positive int henCapacity,
        int cockCapacity,
        Float lengthM,
        Float widthM,
        VentilationType ventilationType
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.HouseStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeHouseStatusRequest(@NotNull HouseStatus status, String reason) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.HouseSectionType;
import jakarta.validation.constraints.NotBlank;

public record CreateHouseSectionRequest(
        @NotBlank String sectionCode,
        HouseSectionType sectionType,
        int birdCapacity,
        String notes
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;

import java.util.UUID;

public record HouseResponse(
        UUID id,
        String houseCode,
        UUID farmSiteId,
        HouseType houseType,
        int henCapacity,
        int cockCapacity,
        Float lengthM,
        Float widthM,
        VentilationType ventilationType,
        HouseStatus status
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.HouseSectionType;

import java.util.UUID;

public record HouseSectionResponse(
        UUID id,
        String sectionCode,
        UUID houseId,
        HouseSectionType sectionType,
        int birdCapacity,
        String notes
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.enums.VentilationType;

import java.util.List;
import java.util.UUID;

public record HouseDetailResponse(
        UUID id,
        String houseCode,
        UUID farmSiteId,
        HouseType houseType,
        int henCapacity,
        int cockCapacity,
        Float lengthM,
        Float widthM,
        VentilationType ventilationType,
        HouseStatus status,
        List<HouseSectionResponse> sections
) {
}
```

- [ ] **Step 5: Write the mappers**

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.HouseResponse;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HouseMapper {
    HouseResponse toResponse(ParentFlockHouse entity);
}
```

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.HouseSectionResponse;
import com.ppms.parentflock.domain.model.HouseSection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HouseSectionMapper {
    HouseSectionResponse toResponse(HouseSection entity);
}
```

- [ ] **Step 6: Write the service**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateHouseRequest;
import com.ppms.parentflock.application.dto.request.CreateHouseSectionRequest;
import com.ppms.parentflock.application.dto.response.HouseDetailResponse;
import com.ppms.parentflock.application.dto.response.HouseResponse;
import com.ppms.parentflock.application.dto.response.HouseSectionResponse;
import com.ppms.parentflock.application.mapper.HouseMapper;
import com.ppms.parentflock.application.mapper.HouseSectionMapper;
import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.model.HouseSection;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import com.ppms.parentflock.domain.repository.BreederFarmSiteRepository;
import com.ppms.parentflock.domain.repository.HouseSectionRepository;
import com.ppms.parentflock.domain.repository.ParentFlockHouseRepository;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HouseService {

    private final ParentFlockHouseRepository houseRepository;
    private final HouseSectionRepository sectionRepository;
    private final BreederFarmSiteRepository farmSiteRepository;
    private final HouseMapper houseMapper;
    private final HouseSectionMapper sectionMapper;

    public HouseResponse registerHouse(UUID farmSiteId, CreateHouseRequest request) {
        farmSiteRepository.findById(farmSiteId)
                .orElseThrow(() -> EntityNotFoundException.of("BreederFarmSite", farmSiteId));
        houseRepository.findByHouseCode(request.houseCode()).ifPresent(existing -> {
            throw new BusinessRuleViolationException("House code already in use: " + request.houseCode());
        });
        ParentFlockHouse house = ParentFlockHouse.register(request.houseCode(), farmSiteId, request.houseType(),
                request.henCapacity(), request.cockCapacity(), request.lengthM(), request.widthM(),
                request.ventilationType());
        return houseMapper.toResponse(houseRepository.save(house));
    }

    public HouseSectionResponse addSection(UUID houseId, CreateHouseSectionRequest request) {
        ParentFlockHouse house = getHouseOrThrow(houseId);
        boolean codeInUse = sectionRepository.findByHouseId(houseId).stream()
                .anyMatch(s -> s.getSectionCode().equalsIgnoreCase(request.sectionCode()));
        if (codeInUse) {
            throw new BusinessRuleViolationException(
                    "Section code " + request.sectionCode() + " already used in house " + house.getHouseCode());
        }
        HouseSection section = HouseSection.create(request.sectionCode(), houseId, request.sectionType(),
                request.birdCapacity(), request.notes());
        return sectionMapper.toResponse(sectionRepository.save(section));
    }

    public HouseResponse changeHouseStatus(UUID houseId, HouseStatus newStatus, String reason) {
        ParentFlockHouse house = getHouseOrThrow(houseId);
        house.changeStatus(newStatus);
        log.info("House {} status changed to {} (reason: {})", house.getHouseCode(), newStatus, reason);
        return houseMapper.toResponse(houseRepository.save(house));
    }

    @Transactional(readOnly = true)
    public HouseDetailResponse getHouseWithSections(UUID houseId) {
        ParentFlockHouse house = getHouseOrThrow(houseId);
        List<HouseSectionResponse> sections = sectionRepository.findByHouseId(houseId).stream()
                .map(sectionMapper::toResponse)
                .toList();
        return new HouseDetailResponse(house.getId(), house.getHouseCode(), house.getFarmSiteId(),
                house.getHouseType(), house.getHenCapacity(), house.getCockCapacity(), house.getLengthM(),
                house.getWidthM(), house.getVentilationType(), house.getStatus(), sections);
    }

    @Transactional(readOnly = true)
    public PageResponse<HouseResponse> listHouses(UUID farmSiteId, HouseStatus status, HouseType type,
                                                   Pageable pageable) {
        Specification<ParentFlockHouse> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and((root, query, cb) -> cb.equal(root.get("farmSiteId"), farmSiteId));
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("houseType"), type));
        }
        return PageResponse.from(houseRepository.findAll(spec, pageable).map(houseMapper::toResponse));
    }

    private ParentFlockHouse getHouseOrThrow(UUID houseId) {
        return houseRepository.findById(houseId)
                .orElseThrow(() -> EntityNotFoundException.of("ParentFlockHouse", houseId));
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=HouseServiceTest`
Expected: PASS (9 tests)

- [ ] **Step 8: Write the controllers**

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.ChangeHouseStatusRequest;
import com.ppms.parentflock.application.dto.request.CreateHouseRequest;
import com.ppms.parentflock.application.dto.response.HouseDetailResponse;
import com.ppms.parentflock.application.dto.response.HouseResponse;
import com.ppms.parentflock.application.service.HouseService;
import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @PostMapping("/api/v1/farm-sites/{farmSiteId}/houses")
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public ResponseEntity<HouseResponse> register(@PathVariable UUID farmSiteId,
                                                   @Valid @RequestBody CreateHouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.registerHouse(farmSiteId, request));
    }

    @GetMapping("/api/v1/farm-sites/{farmSiteId}/houses")
    public PageResponse<HouseResponse> list(@PathVariable UUID farmSiteId,
                                             @RequestParam(required = false) HouseStatus status,
                                             @RequestParam(required = false) HouseType type,
                                             Pageable pageable) {
        return houseService.listHouses(farmSiteId, status, type, pageable);
    }

    @GetMapping("/api/v1/houses/{houseId}")
    public HouseDetailResponse get(@PathVariable UUID houseId) {
        return houseService.getHouseWithSections(houseId);
    }

    @PatchMapping("/api/v1/houses/{houseId}/status")
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public HouseResponse changeStatus(@PathVariable UUID houseId,
                                       @Valid @RequestBody ChangeHouseStatusRequest request) {
        return houseService.changeHouseStatus(houseId, request.status(), request.reason());
    }
}
```

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.CreateHouseSectionRequest;
import com.ppms.parentflock.application.dto.response.HouseSectionResponse;
import com.ppms.parentflock.application.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/houses/{houseId}/sections")
@RequiredArgsConstructor
public class HouseSectionController {

    private final HouseService houseService;

    @PostMapping
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public ResponseEntity<HouseSectionResponse> addSection(@PathVariable UUID houseId,
                                                            @Valid @RequestBody CreateHouseSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.addSection(houseId, request));
    }
}
```

- [ ] **Step 9: Compile check**

Run: `./mvnw -q -pl parent-flock-service -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 10: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/application parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/HouseController.java parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/HouseSectionController.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/ParentFlockHouseRepository.java parent-flock-service/src/test/java/com/ppms/parentflock/application/service/HouseServiceTest.java
git commit -m "feat(parent-flock-service): House/HouseSection DTOs, mapper, service, controllers"
```

---

## Task 14: Auth (login), Security config, global exception handling, OpenAPI

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/UserRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RoleRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/UserRoleAssignmentRepository.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/LoginRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/LoginResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/service/AuthService.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/AuthController.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/exception/GlobalExceptionHandler.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/config/SecurityConfig.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/config/OpenApiConfig.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/application/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `User`/`Role`/`UserRoleAssignment` (Task 3), `JwtTokenProvider` (Task 4), `JwtAuthenticationFilter` (Task 5), all shared exception/web types (Task 2).
- Produces: `AuthService.login(LoginRequest)` → `LoginResponse`. `SecurityConfig` wires `JwtAuthenticationFilter` into the chain and exposes a `PasswordEncoder` bean every later task's tests that need to hash a password use. `GlobalExceptionHandler` maps every `DomainException` subtype to the HTTP status given in `docs/parent-flock-architecture.md` §2.5.

- [ ] **Step 1: Write the repositories**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.shared.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRoleName(String roleName);
}
```

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.shared.domain.model.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    List<UserRoleAssignment> findByUserId(UUID userId);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.LoginRequest;
import com.ppms.parentflock.application.dto.response.LoginResponse;
import com.ppms.parentflock.domain.repository.RoleRepository;
import com.ppms.parentflock.domain.repository.UserRepository;
import com.ppms.parentflock.domain.repository.UserRoleAssignmentRepository;
import com.ppms.shared.domain.model.Role;
import com.ppms.shared.domain.model.User;
import com.ppms.shared.domain.model.UserRoleAssignment;
import com.ppms.shared.domain.enums.RoleScope;
import com.ppms.shared.domain.enums.ServiceModule;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pw")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void loginThrowsWhenAccountLocked() {
        User user = User.create("locked1", "hash", "l@x.com");
        for (int i = 0; i < 5; i++) user.registerFailedLogin();
        when(userRepository.findByUsername("locked1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("locked1", "pw")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void loginRegistersFailedAttemptOnWrongPassword() {
        User user = User.create("jdoe", "hash", "j@x.com");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void loginReturnsTokenOnSuccessAndResetsFailedAttempts() {
        User user = User.create("jdoe", "hash", "j@x.com");
        user.registerFailedLogin();
        Role role = Role.create("FLOCK_SUPERVISOR", "desc", ServiceModule.PARENT_FLOCK);
        UserRoleAssignment assignment = UserRoleAssignment.assign(user.getId(), role.getId(), RoleScope.FARM_SITE,
                null, null);

        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right", "hash")).thenReturn(true);
        when(userRoleAssignmentRepository.findByUserId(user.getId())).thenReturn(List.of(assignment));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(jwtTokenProvider.generateToken(eq("jdoe"), any())).thenReturn("signed-token");
        when(jwtTokenProvider.getExpirationMillis()).thenReturn(28_800_000L);

        LoginResponse response = authService.login(new LoginRequest("jdoe", "right"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=AuthServiceTest`
Expected: FAIL — compile error, `AuthService`/DTOs don't exist yet.

- [ ] **Step 4: Write the DTOs**

```java
package com.ppms.parentflock.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

public record LoginResponse(String token, long expiresInMillis) {
}
```

- [ ] **Step 5: Write `AuthService`**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.LoginRequest;
import com.ppms.parentflock.application.dto.response.LoginResponse;
import com.ppms.parentflock.domain.repository.RoleRepository;
import com.ppms.parentflock.domain.repository.UserRepository;
import com.ppms.parentflock.domain.repository.UserRoleAssignmentRepository;
import com.ppms.shared.domain.model.Role;
import com.ppms.shared.domain.model.User;
import com.ppms.shared.domain.model.UserRoleAssignment;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid username or password"));
        if (user.isLocked()) {
            throw new BusinessRuleViolationException("Account is locked");
        }
        if (!user.isActive()) {
            throw new BusinessRuleViolationException("Account is inactive");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new BusinessRuleViolationException("Invalid username or password");
        }
        user.registerSuccessfulLogin();
        userRepository.save(user);

        var roles = userRoleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(UserRoleAssignment::getRoleId)
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new BusinessRuleViolationException("Assigned role no longer exists")))
                .map(Role::getRoleName)
                .distinct()
                .toList();

        String token = jwtTokenProvider.generateToken(user.getUsername(), roles);
        return new LoginResponse(token, jwtTokenProvider.getExpirationMillis());
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=AuthServiceTest`
Expected: PASS (4 tests)

- [ ] **Step 7: Write `SecurityConfig`**

```java
package com.ppms.parentflock.config;

import com.ppms.shared.security.JwtAuthenticationFilter;
import com.ppms.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 8: Write `GlobalExceptionHandler`**

```java
package com.ppms.parentflock.api.exception;

import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.DomainException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.exception.ValidationException;
import com.ppms.shared.exception.WithdrawalPeriodActiveException;
import com.ppms.shared.web.ApiErrorResponse;
import com.ppms.shared.web.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({BusinessRuleViolationException.class, WithdrawalPeriodActiveException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(DomainException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldValidationError(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.ofValidation(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Validation failed", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(DomainException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message,
                        request.getRequestURI()));
    }
}
```

- [ ] **Step 9: Write `OpenApiConfig`**

```java
package com.ppms.parentflock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI parentFlockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PPMS Parent Flock Service")
                        .version("1.0")
                        .description("Parent Flock bounded context — PF1 foundation (farm sites, houses, flock lifecycle)"))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

- [ ] **Step 10: Write `AuthController`**

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.LoginRequest;
import com.ppms.parentflock.application.dto.response.LoginResponse;
import com.ppms.parentflock.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

- [ ] **Step 11: Compile check**

Run: `./mvnw -q -pl parent-flock-service -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 12: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/UserRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/RoleRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/domain/repository/UserRoleAssignmentRepository.java parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/LoginRequest.java parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/LoginResponse.java parent-flock-service/src/main/java/com/ppms/parentflock/application/service/AuthService.java parent-flock-service/src/main/java/com/ppms/parentflock/api parent-flock-service/src/main/java/com/ppms/parentflock/config parent-flock-service/src/test/java/com/ppms/parentflock/application/service/AuthServiceTest.java
git commit -m "feat(parent-flock-service): auth login, JWT security config, global exception handling, OpenAPI"
```

---

## Task 15: Flock — DTOs, mapper, `FlockSpecification`, service (TDD), controllers

This is the largest task: it's the aggregate root everything else in PF1 hangs off of, so it carries the bulk of the business rules from `docs/parent-flock-service.doc.md` §7.1 and §9.

**Files:**
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/CreateFlockRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/RecordBirdCountRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/RearingWeightSampleRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/PhaseTransferRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/TransferDestinationRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/request/CullingRequest.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/FlockResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/FlockDetailResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/BirdCountEventResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/RearingWeightSampleResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/TransferResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/TransferDestinationResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/dto/response/CullingResponse.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/FlockMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/RearingWeightSampleMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/mapper/FlockBirdCountEventMapper.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/infrastructure/persistence/FlockSpecification.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/application/service/FlockService.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/FlockController.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/RearingController.java`
- Create: `parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/CullingController.java`
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/application/service/FlockServiceTest.java`

**Interfaces:**
- Consumes: `ParentFlock`, `FlockBirdCountEvent`, `RearingWeightSample`, `RearingToProductionTransfer`, `TransferDestination`, `FlockCullingRecord` and their repositories (Tasks 10–11); `ParentFlockHouse`/`ParentFlockHouseRepository` (Task 9); all PF1 enums (Task 7).
- Produces: `FlockService.registerFlock(CreateFlockRequest)` → `FlockResponse`; `recordBirdCountEvent(UUID, RecordBirdCountRequest)` → `FlockResponse`; `recordRearingWeightSample(UUID, RearingWeightSampleRequest)` → `RearingWeightSampleResponse`; `recordPhaseTransfer(UUID, PhaseTransferRequest)` → `TransferResponse`; `recordCulling(UUID, CullingRequest)` → `CullingResponse`; `getFlock(UUID)` → `FlockDetailResponse`; `listFlocks(UUID farmSiteId, FlockPhaseStatus phaseStatus, Pageable)` → `PageResponse<FlockResponse>`.

- [ ] **Step 1: Write the failing test**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateFlockRequest;
import com.ppms.parentflock.application.dto.request.CullingRequest;
import com.ppms.parentflock.application.dto.request.PhaseTransferRequest;
import com.ppms.parentflock.application.dto.request.RearingWeightSampleRequest;
import com.ppms.parentflock.application.dto.request.RecordBirdCountRequest;
import com.ppms.parentflock.application.dto.request.TransferDestinationRequest;
import com.ppms.parentflock.application.dto.response.CullingResponse;
import com.ppms.parentflock.application.dto.response.FlockDetailResponse;
import com.ppms.parentflock.application.dto.response.FlockResponse;
import com.ppms.parentflock.application.dto.response.RearingWeightSampleResponse;
import com.ppms.parentflock.application.dto.response.TransferResponse;
import com.ppms.parentflock.application.mapper.FlockBirdCountEventMapperImpl;
import com.ppms.parentflock.application.mapper.FlockMapperImpl;
import com.ppms.parentflock.application.mapper.RearingWeightSampleMapperImpl;
import com.ppms.parentflock.domain.enums.CullingReason;
import com.ppms.parentflock.domain.enums.DisposalMethod;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;
import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.model.FlockBirdCountEvent;
import com.ppms.parentflock.domain.model.ParentFlock;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import com.ppms.parentflock.domain.model.RearingToProductionTransfer;
import com.ppms.parentflock.domain.repository.FlockBirdCountEventRepository;
import com.ppms.parentflock.domain.repository.FlockCullingRecordRepository;
import com.ppms.parentflock.domain.repository.ParentFlockHouseRepository;
import com.ppms.parentflock.domain.repository.ParentFlockRepository;
import com.ppms.parentflock.domain.repository.RearingToProductionTransferRepository;
import com.ppms.parentflock.domain.repository.RearingWeightSampleRepository;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlockServiceTest {

    @Mock
    private ParentFlockRepository flockRepository;
    @Mock
    private ParentFlockHouseRepository houseRepository;
    @Mock
    private FlockBirdCountEventRepository birdCountEventRepository;
    @Mock
    private RearingWeightSampleRepository weightSampleRepository;
    @Mock
    private RearingToProductionTransferRepository transferRepository;
    @Mock
    private FlockCullingRecordRepository cullingRecordRepository;

    private FlockService service() {
        return new FlockService(flockRepository, houseRepository, birdCountEventRepository,
                weightSampleRepository, transferRepository, cullingRecordRepository,
                new FlockMapperImpl(), new RearingWeightSampleMapperImpl(), new FlockBirdCountEventMapperImpl());
    }

    private CreateFlockRequest sampleFlockRequest(UUID rearingHouseId) {
        return new CreateFlockRequest("PF-100", UUID.randomUUID(), "Ross 308", "Broiler Breeder",
                LocalDate.now().minusWeeks(1), 5000, 500, rearingHouseId, FlockSourceType.PURCHASED, "SUP-1");
    }

    @Test
    void registerFlockThrowsWhenHouseIsNotRearingType() {
        FlockService service = service();
        UUID houseId = UUID.randomUUID();
        when(flockRepository.findByFlockCode("PF-100")).thenReturn(Optional.empty());
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(
                ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.PRODUCTION, 1, 1, null, null, null)));

        assertThatThrownBy(() -> service.registerFlock(sampleFlockRequest(houseId)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void registerFlockThrowsWhenHouseIsNotEmpty() {
        FlockService service = service();
        UUID houseId = UUID.randomUUID();
        ParentFlockHouse house = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                1, 1, null, null, null);
        house.changeStatus(HouseStatus.OCCUPIED);
        when(flockRepository.findByFlockCode("PF-100")).thenReturn(Optional.empty());
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> service.registerFlock(sampleFlockRequest(houseId)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void registerFlockSavesFlockAndOccupiesHouse() {
        FlockService service = service();
        UUID houseId = UUID.randomUUID();
        ParentFlockHouse house = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                5000, 500, null, null, null);
        when(flockRepository.findByFlockCode("PF-100")).thenReturn(Optional.empty());
        when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
        when(houseRepository.save(any(ParentFlockHouse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flockRepository.save(any(ParentFlock.class))).thenAnswer(inv -> inv.getArgument(0));

        FlockResponse response = service.registerFlock(sampleFlockRequest(houseId));

        assertThat(response.flockCode()).isEqualTo("PF-100");
        assertThat(response.phaseStatus()).isEqualTo(FlockPhaseStatus.REARING);
        assertThat(house.getStatus()).isEqualTo(HouseStatus.OCCUPIED);
    }

    @Test
    void recordBirdCountEventUpdatesCountsAndPersistsEvent() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 100, 10, UUID.randomUUID(), FlockSourceType.OWN_STOCK, null);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(flockRepository.save(any(ParentFlock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(birdCountEventRepository.save(any(FlockBirdCountEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        FlockResponse response = service.recordBirdCountEvent(flockId,
                new RecordBirdCountRequest(LocalDate.now(), -5, -1,
                        com.ppms.parentflock.domain.enums.MortalityCause.DISEASE, "notes", UUID.randomUUID()));

        assertThat(response.currentHenCount()).isEqualTo(95);
        assertThat(response.currentCockCount()).isEqualTo(9);
    }

    @Test
    void recordRearingWeightSampleRejectsWhenFlockNotInRearingPhase() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 100, 10, UUID.randomUUID(), FlockSourceType.OWN_STOCK, null);
        flock.transferToProduction();
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));

        assertThatThrownBy(() -> service.recordRearingWeightSample(flockId,
                new RearingWeightSampleRequest(10, LocalDate.now(), 50, 25000f, 500f, 90f, UUID.randomUUID())))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void recordPhaseTransferRejectsDestinationHouseOfWrongType() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        UUID rearingHouseId = UUID.randomUUID();
        UUID destHouseId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 5000, 500, rearingHouseId, FlockSourceType.OWN_STOCK, null);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(houseRepository.findById(destHouseId)).thenReturn(Optional.of(
                ParentFlockHouse.register("H-2", UUID.randomUUID(), HouseType.REARING, 1, 1, null, null, null)));

        PhaseTransferRequest request = new PhaseTransferRequest(LocalDate.now(), 1750f, UUID.randomUUID(),
                List.of(new TransferDestinationRequest(destHouseId, 5000, 500)));

        assertThatThrownBy(() -> service.recordPhaseTransfer(flockId, request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void recordPhaseTransferRejectsWhenTransferredCountsExceedCurrentCounts() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        UUID rearingHouseId = UUID.randomUUID();
        UUID destHouseId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 5000, 500, rearingHouseId, FlockSourceType.OWN_STOCK, null);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(houseRepository.findById(destHouseId)).thenReturn(Optional.of(
                ParentFlockHouse.register("H-2", UUID.randomUUID(), HouseType.PRODUCTION, 6000, 600, null, null, null)));

        PhaseTransferRequest request = new PhaseTransferRequest(LocalDate.now(), 1750f, UUID.randomUUID(),
                List.of(new TransferDestinationRequest(destHouseId, 5001, 500)));

        assertThatThrownBy(() -> service.recordPhaseTransfer(flockId, request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void recordPhaseTransferMovesFlockAndHousesOnSuccess() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        UUID rearingHouseId = UUID.randomUUID();
        UUID destHouseId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 5000, 500, rearingHouseId, FlockSourceType.OWN_STOCK, null);
        ParentFlockHouse rearingHouse = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                5000, 500, null, null, null);
        rearingHouse.changeStatus(HouseStatus.OCCUPIED);
        ParentFlockHouse destHouse = ParentFlockHouse.register("H-2", UUID.randomUUID(), HouseType.PRODUCTION,
                6000, 600, null, null, null);

        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(houseRepository.findById(destHouseId)).thenReturn(Optional.of(destHouse));
        when(houseRepository.findById(rearingHouseId)).thenReturn(Optional.of(rearingHouse));
        lenient().when(houseRepository.save(any(ParentFlockHouse.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(houseRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(RearingToProductionTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flockRepository.save(any(ParentFlock.class))).thenAnswer(inv -> inv.getArgument(0));

        PhaseTransferRequest request = new PhaseTransferRequest(LocalDate.now(), 1750f, UUID.randomUUID(),
                List.of(new TransferDestinationRequest(destHouseId, 5000, 500)));

        TransferResponse response = service.recordPhaseTransfer(flockId, request);

        assertThat(response.totalTransferredHens()).isEqualTo(5000);
        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.PRODUCTION);
        assertThat(rearingHouse.getStatus()).isEqualTo(HouseStatus.CLEANING);
        assertThat(destHouse.getStatus()).isEqualTo(HouseStatus.OCCUPIED);
    }

    @Test
    void recordCullingRejectsOverCulling() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 100, 10, UUID.randomUUID(), FlockSourceType.OWN_STOCK, null);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));

        CullingRequest request = new CullingRequest(LocalDate.now(), 101, 0, CullingReason.DISEASE,
                DisposalMethod.INCINERATION, UUID.randomUUID(), null);

        assertThatThrownBy(() -> service.recordCulling(flockId, request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void recordCullingFullCullClosesFlockAndRearingHouse() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        UUID rearingHouseId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now(), 100, 10, rearingHouseId, FlockSourceType.OWN_STOCK, null);
        ParentFlockHouse rearingHouse = ParentFlockHouse.register("H-1", UUID.randomUUID(), HouseType.REARING,
                100, 10, null, null, null);
        rearingHouse.changeStatus(HouseStatus.OCCUPIED);

        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(flockRepository.save(any(ParentFlock.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(cullingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(houseRepository.findById(rearingHouseId)).thenReturn(Optional.of(rearingHouse));
        lenient().when(houseRepository.save(any(ParentFlockHouse.class))).thenAnswer(inv -> inv.getArgument(0));

        CullingResponse response = service.recordCulling(flockId,
                new CullingRequest(LocalDate.now(), 100, 10, CullingReason.AGE,
                        DisposalMethod.COMPOSTING, UUID.randomUUID(), "end of life"));

        assertThat(response.flockClosed()).isTrue();
        assertThat(flock.getPhaseStatus()).isEqualTo(FlockPhaseStatus.CULLED);
        assertThat(rearingHouse.getStatus()).isEqualTo(HouseStatus.CLEANING);
    }

    @Test
    void getFlockThrowsWhenNotFound() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        when(flockRepository.findById(flockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFlock(flockId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getFlockReturnsDetailWithAgeInWeeksAndRecentEvents() {
        FlockService service = service();
        UUID flockId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-1", UUID.randomUUID(), "L", "B",
                LocalDate.now().minusWeeks(3), 100, 10, UUID.randomUUID(), FlockSourceType.OWN_STOCK, null);
        when(flockRepository.findById(flockId)).thenReturn(Optional.of(flock));
        when(birdCountEventRepository.findByFlockIdOrderByEventDateDesc(flockId)).thenReturn(List.of(
                FlockBirdCountEvent.record(flockId, LocalDate.now(), -1, 0,
                        com.ppms.parentflock.domain.enums.MortalityCause.UNKNOWN, null, null)));

        FlockDetailResponse response = service.getFlock(flockId);

        assertThat(response.ageInWeeks()).isEqualTo(3L);
        assertThat(response.recentBirdCountEvents()).hasSize(1);
    }

    @Test
    void listFlocksDelegatesToRepositoryWithSpecification() {
        FlockService service = service();
        UUID farmSiteId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(flockRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.listFlocks(farmSiteId, FlockPhaseStatus.PRODUCTION, pageable);

        org.mockito.Mockito.verify(flockRepository)
                .findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=FlockServiceTest`
Expected: FAIL — compile error, none of the DTOs/mapper/service exist yet.

- [ ] **Step 3: Write the request DTOs**

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.FlockSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CreateFlockRequest(
        @NotBlank String flockCode,
        @NotNull UUID farmSiteId,
        String geneticLine,
        String breed,
        @NotNull LocalDate entryDate,
        @PositiveOrZero int initialHenCount,
        @PositiveOrZero int initialCockCount,
        @NotNull UUID rearingHouseId,
        FlockSourceType sourceType,
        String supplierRef
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.MortalityCause;

import java.time.LocalDate;
import java.util.UUID;

public record RecordBirdCountRequest(
        LocalDate eventDate,
        int henChange,
        int cockChange,
        MortalityCause cause,
        String causeNotes,
        UUID recordedBy
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record RearingWeightSampleRequest(
        @NotNull Integer weekOfAge,
        @NotNull LocalDate sampleDate,
        @Positive int sampleSize,
        @Positive float totalWeightG,
        Float targetWeightG,
        Float uniformityPercent,
        UUID recordedBy
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PhaseTransferRequest(
        @NotNull LocalDate transferDate,
        Float bodyWeightAtTransferG,
        UUID authorisedBy,
        @NotEmpty @Valid List<TransferDestinationRequest> destinations
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record TransferDestinationRequest(
        @NotNull UUID productionHouseId,
        @PositiveOrZero int transferredHenCount,
        @PositiveOrZero int transferredCockCount
) {
}
```

```java
package com.ppms.parentflock.application.dto.request;

import com.ppms.parentflock.domain.enums.CullingReason;
import com.ppms.parentflock.domain.enums.DisposalMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CullingRequest(
        @NotNull LocalDate cullingDate,
        @PositiveOrZero int hensCulled,
        @PositiveOrZero int cocksCulled,
        CullingReason reason,
        DisposalMethod disposalMethod,
        UUID authorisedBy,
        String notes
) {
}
```

- [ ] **Step 4: Write the response DTOs**

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.FlockHealthStatus;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;

import java.time.LocalDate;
import java.util.UUID;

public record FlockResponse(
        UUID id,
        String flockCode,
        UUID farmSiteId,
        String geneticLine,
        String breed,
        LocalDate entryDate,
        FlockPhaseStatus phaseStatus,
        int currentHenCount,
        int currentCockCount,
        int initialHenCount,
        int initialCockCount,
        UUID rearingHouseId,
        FlockHealthStatus healthStatus,
        FlockSourceType sourceType,
        String supplierRef,
        long ageInWeeks
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.FlockHealthStatus;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FlockDetailResponse(
        UUID id,
        String flockCode,
        UUID farmSiteId,
        String geneticLine,
        String breed,
        LocalDate entryDate,
        FlockPhaseStatus phaseStatus,
        int currentHenCount,
        int currentCockCount,
        int initialHenCount,
        int initialCockCount,
        UUID rearingHouseId,
        FlockHealthStatus healthStatus,
        FlockSourceType sourceType,
        String supplierRef,
        long ageInWeeks,
        List<BirdCountEventResponse> recentBirdCountEvents
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.MortalityCause;

import java.time.LocalDate;
import java.util.UUID;

public record BirdCountEventResponse(
        UUID id,
        LocalDate eventDate,
        int henChange,
        int cockChange,
        MortalityCause cause,
        String causeNotes,
        UUID recordedBy
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record RearingWeightSampleResponse(
        UUID id,
        UUID flockId,
        int weekOfAge,
        LocalDate sampleDate,
        int sampleSize,
        float totalWeightG,
        float averageWeightG,
        float targetWeightG,
        float uniformityPercent,
        UUID recordedBy
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID flockId,
        LocalDate transferDate,
        UUID rearingHouseId,
        Float bodyWeightAtTransferG,
        UUID authorisedBy,
        int totalTransferredHens,
        int totalTransferredCocks,
        List<TransferDestinationResponse> destinations
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import java.util.UUID;

public record TransferDestinationResponse(
        UUID productionHouseId,
        int transferredHenCount,
        int transferredCockCount
) {
}
```

```java
package com.ppms.parentflock.application.dto.response;

import com.ppms.parentflock.domain.enums.CullingReason;
import com.ppms.parentflock.domain.enums.DisposalMethod;

import java.time.LocalDate;
import java.util.UUID;

public record CullingResponse(
        UUID id,
        UUID flockId,
        LocalDate cullingDate,
        int hensCulled,
        int cocksCulled,
        CullingReason reason,
        DisposalMethod disposalMethod,
        UUID authorisedBy,
        String notes,
        boolean flockClosed
) {
}
```

- [ ] **Step 5: Write the mappers**

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.FlockResponse;
import com.ppms.parentflock.domain.model.ParentFlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlockMapper {
    @Mapping(target = "ageInWeeks", expression = "java(entity.ageInWeeks())")
    FlockResponse toResponse(ParentFlock entity);
}
```

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.RearingWeightSampleResponse;
import com.ppms.parentflock.domain.model.RearingWeightSample;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RearingWeightSampleMapper {
    RearingWeightSampleResponse toResponse(RearingWeightSample entity);
}
```

```java
package com.ppms.parentflock.application.mapper;

import com.ppms.parentflock.application.dto.response.BirdCountEventResponse;
import com.ppms.parentflock.domain.model.FlockBirdCountEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlockBirdCountEventMapper {
    BirdCountEventResponse toResponse(FlockBirdCountEvent entity);
}
```

- [ ] **Step 6: Write `FlockSpecification`**

```java
package com.ppms.parentflock.infrastructure.persistence;

import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.model.ParentFlock;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FlockSpecification {

    private FlockSpecification() {
    }

    public static Specification<ParentFlock> withFilters(UUID farmSiteId, FlockPhaseStatus phaseStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (farmSiteId != null) {
                predicates.add(cb.equal(root.get("farmSiteId"), farmSiteId));
            }
            if (phaseStatus != null) {
                predicates.add(cb.equal(root.get("phaseStatus"), phaseStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

- [ ] **Step 7: Write `FlockService`**

```java
package com.ppms.parentflock.application.service;

import com.ppms.parentflock.application.dto.request.CreateFlockRequest;
import com.ppms.parentflock.application.dto.request.CullingRequest;
import com.ppms.parentflock.application.dto.request.PhaseTransferRequest;
import com.ppms.parentflock.application.dto.request.RearingWeightSampleRequest;
import com.ppms.parentflock.application.dto.request.RecordBirdCountRequest;
import com.ppms.parentflock.application.dto.request.TransferDestinationRequest;
import com.ppms.parentflock.application.dto.response.CullingResponse;
import com.ppms.parentflock.application.dto.response.FlockDetailResponse;
import com.ppms.parentflock.application.dto.response.FlockResponse;
import com.ppms.parentflock.application.dto.response.RearingWeightSampleResponse;
import com.ppms.parentflock.application.dto.response.TransferDestinationResponse;
import com.ppms.parentflock.application.dto.response.TransferResponse;
import com.ppms.parentflock.application.mapper.FlockBirdCountEventMapper;
import com.ppms.parentflock.application.mapper.FlockMapper;
import com.ppms.parentflock.application.mapper.RearingWeightSampleMapper;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.HouseStatus;
import com.ppms.parentflock.domain.enums.HouseType;
import com.ppms.parentflock.domain.model.FlockBirdCountEvent;
import com.ppms.parentflock.domain.model.FlockCullingRecord;
import com.ppms.parentflock.domain.model.ParentFlock;
import com.ppms.parentflock.domain.model.ParentFlockHouse;
import com.ppms.parentflock.domain.model.RearingToProductionTransfer;
import com.ppms.parentflock.domain.model.RearingWeightSample;
import com.ppms.parentflock.domain.model.TransferDestination;
import com.ppms.parentflock.domain.repository.FlockBirdCountEventRepository;
import com.ppms.parentflock.domain.repository.FlockCullingRecordRepository;
import com.ppms.parentflock.domain.repository.ParentFlockHouseRepository;
import com.ppms.parentflock.domain.repository.ParentFlockRepository;
import com.ppms.parentflock.domain.repository.RearingToProductionTransferRepository;
import com.ppms.parentflock.domain.repository.RearingWeightSampleRepository;
import com.ppms.parentflock.infrastructure.persistence.FlockSpecification;
import com.ppms.shared.exception.BusinessRuleViolationException;
import com.ppms.shared.exception.EntityNotFoundException;
import com.ppms.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FlockService {

    private final ParentFlockRepository flockRepository;
    private final ParentFlockHouseRepository houseRepository;
    private final FlockBirdCountEventRepository birdCountEventRepository;
    private final RearingWeightSampleRepository weightSampleRepository;
    private final RearingToProductionTransferRepository transferRepository;
    private final FlockCullingRecordRepository cullingRecordRepository;
    private final FlockMapper flockMapper;
    private final RearingWeightSampleMapper weightSampleMapper;
    private final FlockBirdCountEventMapper eventMapper;

    public FlockResponse registerFlock(CreateFlockRequest request) {
        flockRepository.findByFlockCode(request.flockCode()).ifPresent(existing -> {
            throw new BusinessRuleViolationException("Flock code already in use: " + request.flockCode());
        });
        ParentFlockHouse house = houseRepository.findById(request.rearingHouseId())
                .orElseThrow(() -> EntityNotFoundException.of("ParentFlockHouse", request.rearingHouseId()));
        if (house.getHouseType() != HouseType.REARING) {
            throw new BusinessRuleViolationException(
                    "House " + house.getHouseCode() + " is not a REARING house");
        }
        house.changeStatus(HouseStatus.OCCUPIED);
        houseRepository.save(house);

        ParentFlock flock = ParentFlock.register(request.flockCode(), request.farmSiteId(), request.geneticLine(),
                request.breed(), request.entryDate(), request.initialHenCount(), request.initialCockCount(),
                request.rearingHouseId(), request.sourceType(), request.supplierRef());
        return flockMapper.toResponse(flockRepository.save(flock));
    }

    public FlockResponse recordBirdCountEvent(UUID flockId, RecordBirdCountRequest request) {
        ParentFlock flock = getFlockOrThrow(flockId);
        flock.applyBirdCountChange(request.henChange(), request.cockChange());
        FlockBirdCountEvent event = FlockBirdCountEvent.record(flockId, request.eventDate(), request.henChange(),
                request.cockChange(), request.cause(), request.causeNotes(), request.recordedBy());
        birdCountEventRepository.save(event);
        return flockMapper.toResponse(flockRepository.save(flock));
    }

    public RearingWeightSampleResponse recordRearingWeightSample(UUID flockId, RearingWeightSampleRequest request) {
        ParentFlock flock = getFlockOrThrow(flockId);
        flock.assertInRearingPhase();
        RearingWeightSample sample = RearingWeightSample.record(flockId, request.weekOfAge(), request.sampleDate(),
                request.sampleSize(), request.totalWeightG(), request.targetWeightG(), request.uniformityPercent(),
                request.recordedBy());
        return weightSampleMapper.toResponse(weightSampleRepository.save(sample));
    }

    public TransferResponse recordPhaseTransfer(UUID flockId, PhaseTransferRequest request) {
        ParentFlock flock = getFlockOrThrow(flockId);
        flock.assertInRearingPhase();

        List<ParentFlockHouse> destinationHouses = new ArrayList<>();
        int totalHens = 0;
        int totalCocks = 0;
        for (TransferDestinationRequest dest : request.destinations()) {
            ParentFlockHouse house = houseRepository.findById(dest.productionHouseId())
                    .orElseThrow(() -> EntityNotFoundException.of("ParentFlockHouse", dest.productionHouseId()));
            if (house.getHouseType() != HouseType.PRODUCTION) {
                throw new BusinessRuleViolationException(
                        "House " + house.getHouseCode() + " is not a PRODUCTION house");
            }
            if (house.getStatus() != HouseStatus.EMPTY) {
                throw new BusinessRuleViolationException("House " + house.getHouseCode() + " is not EMPTY");
            }
            destinationHouses.add(house);
            totalHens += dest.transferredHenCount();
            totalCocks += dest.transferredCockCount();
        }
        if (totalHens > flock.getCurrentHenCount() || totalCocks > flock.getCurrentCockCount()) {
            throw new BusinessRuleViolationException(
                    "Transferred bird counts exceed current counts for flock " + flock.getFlockCode());
        }

        RearingToProductionTransfer transfer = RearingToProductionTransfer.create(flockId, request.transferDate(),
                flock.getRearingHouseId(), request.bodyWeightAtTransferG(), request.authorisedBy());
        for (TransferDestinationRequest dest : request.destinations()) {
            transfer.addDestination(dest.productionHouseId(), dest.transferredHenCount(),
                    dest.transferredCockCount());
        }
        transferRepository.save(transfer);

        ParentFlockHouse rearingHouse = houseRepository.findById(flock.getRearingHouseId())
                .orElseThrow(() -> EntityNotFoundException.of("ParentFlockHouse", flock.getRearingHouseId()));
        rearingHouse.changeStatus(HouseStatus.CLEANING);
        houseRepository.save(rearingHouse);

        destinationHouses.forEach(house -> house.changeStatus(HouseStatus.OCCUPIED));
        houseRepository.saveAll(destinationHouses);

        flock.transferToProduction();
        flockRepository.save(flock);

        return toTransferResponse(transfer);
    }

    public CullingResponse recordCulling(UUID flockId, CullingRequest request) {
        ParentFlock flock = getFlockOrThrow(flockId);
        FlockPhaseStatus phaseBeforeCulling = flock.getPhaseStatus();
        boolean fullyClosed = flock.applyCulling(request.hensCulled(), request.cocksCulled());

        FlockCullingRecord record = FlockCullingRecord.record(flockId, request.cullingDate(), request.hensCulled(),
                request.cocksCulled(), request.reason(), request.disposalMethod(), request.authorisedBy(),
                request.notes());
        cullingRecordRepository.save(record);
        flockRepository.save(flock);

        if (fullyClosed) {
            closeHousesForClosedFlock(flock, phaseBeforeCulling);
        }

        return new CullingResponse(record.getId(), flockId, record.getCullingDate(), record.getHensCulled(),
                record.getCocksCulled(), record.getReason(), record.getDisposalMethod(),
                record.getAuthorisedBy(), record.getNotes(), fullyClosed);
    }

    @Transactional(readOnly = true)
    public FlockDetailResponse getFlock(UUID flockId) {
        ParentFlock flock = getFlockOrThrow(flockId);
        var recentEvents = birdCountEventRepository.findByFlockIdOrderByEventDateDesc(flockId).stream()
                .limit(5)
                .map(eventMapper::toResponse)
                .toList();
        return new FlockDetailResponse(flock.getId(), flock.getFlockCode(), flock.getFarmSiteId(),
                flock.getGeneticLine(), flock.getBreed(), flock.getEntryDate(), flock.getPhaseStatus(),
                flock.getCurrentHenCount(), flock.getCurrentCockCount(), flock.getInitialHenCount(),
                flock.getInitialCockCount(), flock.getRearingHouseId(), flock.getHealthStatus(),
                flock.getSourceType(), flock.getSupplierRef(), flock.ageInWeeks(), recentEvents);
    }

    @Transactional(readOnly = true)
    public PageResponse<FlockResponse> listFlocks(UUID farmSiteId, FlockPhaseStatus phaseStatus, Pageable pageable) {
        var page = flockRepository.findAll(FlockSpecification.withFilters(farmSiteId, phaseStatus), pageable);
        return PageResponse.from(page.map(flockMapper::toResponse));
    }

    private void closeHousesForClosedFlock(ParentFlock flock, FlockPhaseStatus phaseBeforeCulling) {
        if (phaseBeforeCulling == FlockPhaseStatus.REARING) {
            houseRepository.findById(flock.getRearingHouseId()).ifPresent(this::closeIfOccupied);
            return;
        }
        for (RearingToProductionTransfer transfer : transferRepository.findByFlockId(flock.getId())) {
            for (TransferDestination destination : transfer.getDestinations()) {
                houseRepository.findById(destination.getProductionHouseId()).ifPresent(this::closeIfOccupied);
            }
        }
    }

    private void closeIfOccupied(ParentFlockHouse house) {
        if (house.getStatus() == HouseStatus.OCCUPIED) {
            house.changeStatus(HouseStatus.CLEANING);
            houseRepository.save(house);
        }
    }

    private ParentFlock getFlockOrThrow(UUID flockId) {
        return flockRepository.findById(flockId)
                .orElseThrow(() -> EntityNotFoundException.of("ParentFlock", flockId));
    }

    private TransferResponse toTransferResponse(RearingToProductionTransfer transfer) {
        List<TransferDestinationResponse> destinations = transfer.getDestinations().stream()
                .map(d -> new TransferDestinationResponse(d.getProductionHouseId(), d.getTransferredHenCount(),
                        d.getTransferredCockCount()))
                .toList();
        return new TransferResponse(transfer.getId(), transfer.getFlockId(), transfer.getTransferDate(),
                transfer.getRearingHouseId(), transfer.getBodyWeightAtTransferG(), transfer.getAuthorisedBy(),
                transfer.totalTransferredHens(), transfer.totalTransferredCocks(), destinations);
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=FlockServiceTest`
Expected: PASS (13 tests)

- [ ] **Step 9: Write the controllers**

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.CreateFlockRequest;
import com.ppms.parentflock.application.dto.request.RecordBirdCountRequest;
import com.ppms.parentflock.application.dto.response.FlockDetailResponse;
import com.ppms.parentflock.application.dto.response.FlockResponse;
import com.ppms.parentflock.application.service.FlockService;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flocks")
@RequiredArgsConstructor
public class FlockController {

    private final FlockService flockService;

    @PostMapping
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public ResponseEntity<FlockResponse> register(@Valid @RequestBody CreateFlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flockService.registerFlock(request));
    }

    @GetMapping
    public PageResponse<FlockResponse> list(@RequestParam(required = false) UUID farmSiteId,
                                             @RequestParam(required = false) FlockPhaseStatus phaseStatus,
                                             Pageable pageable) {
        return flockService.listFlocks(farmSiteId, phaseStatus, pageable);
    }

    @GetMapping("/{id}")
    public FlockDetailResponse get(@PathVariable UUID id) {
        return flockService.getFlock(id);
    }

    @PostMapping("/{id}/bird-count-events")
    @PreAuthorize("hasAnyRole('FLOCK_WORKER', 'FLOCK_SUPERVISOR')")
    public FlockResponse recordBirdCountEvent(@PathVariable UUID id, @RequestBody RecordBirdCountRequest request) {
        return flockService.recordBirdCountEvent(id, request);
    }
}
```

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.PhaseTransferRequest;
import com.ppms.parentflock.application.dto.request.RearingWeightSampleRequest;
import com.ppms.parentflock.application.dto.response.RearingWeightSampleResponse;
import com.ppms.parentflock.application.dto.response.TransferResponse;
import com.ppms.parentflock.application.service.FlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flocks/{flockId}")
@RequiredArgsConstructor
public class RearingController {

    private final FlockService flockService;

    @PostMapping("/rearing-weight-samples")
    @PreAuthorize("hasAnyRole('FLOCK_WORKER', 'FLOCK_SUPERVISOR')")
    public ResponseEntity<RearingWeightSampleResponse> recordWeightSample(
            @PathVariable UUID flockId, @Valid @RequestBody RearingWeightSampleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flockService.recordRearingWeightSample(flockId, request));
    }

    @PostMapping("/phase-transfer")
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public TransferResponse recordPhaseTransfer(@PathVariable UUID flockId,
                                                 @Valid @RequestBody PhaseTransferRequest request) {
        return flockService.recordPhaseTransfer(flockId, request);
    }
}
```

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.request.CullingRequest;
import com.ppms.parentflock.application.dto.response.CullingResponse;
import com.ppms.parentflock.application.service.FlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flocks/{flockId}/culling")
@RequiredArgsConstructor
public class CullingController {

    private final FlockService flockService;

    @PostMapping
    @PreAuthorize("hasRole('FLOCK_SUPERVISOR')")
    public CullingResponse recordCulling(@PathVariable UUID flockId, @Valid @RequestBody CullingRequest request) {
        return flockService.recordCulling(flockId, request);
    }
}
```

- [ ] **Step 10: Compile check**

Run: `./mvnw -q -pl parent-flock-service -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 11: Commit**

```bash
git add parent-flock-service/src/main/java/com/ppms/parentflock/application parent-flock-service/src/main/java/com/ppms/parentflock/infrastructure parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/FlockController.java parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/RearingController.java parent-flock-service/src/main/java/com/ppms/parentflock/api/controller/CullingController.java parent-flock-service/src/test/java/com/ppms/parentflock/application/service/FlockServiceTest.java
git commit -m "feat(parent-flock-service): Flock DTOs, mapper, FlockSpecification, service with full PF1 lifecycle rules, controllers"
```

---

## Task 16: `ParentFlockRepositoryIT` — Testcontainers-backed persistence test

**Files:**
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/domain/repository/ParentFlockRepositoryIT.java`

**Interfaces:**
- Consumes: `ParentFlockServiceApplication` (Task 6), `ParentFlockRepository` (Task 10), all migrations through Task 11.

This test requires a running Docker daemon (Testcontainers starts its own ephemeral `postgres:16` container — separate from the docker-compose `postgres-parent-flock` service — and tears it down after the run). If Docker isn't running when you execute this task, `./mvnw test` will fail with a Testcontainers connection error; start Docker Desktop first.

- [ ] **Step 1: Write the test**

```java
package com.ppms.parentflock.domain.repository;

import com.ppms.parentflock.ParentFlockServiceApplication;
import com.ppms.parentflock.domain.enums.FlockSourceType;
import com.ppms.parentflock.domain.model.ParentFlock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = ParentFlockServiceApplication.class)
class ParentFlockRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ParentFlockRepository repository;

    @Test
    void findByFlockCodeReturnsSavedFlock() {
        ParentFlock flock = ParentFlock.register("PF-IT-1", UUID.randomUUID(), "Line", "Breed",
                LocalDate.now().minusWeeks(5), 100, 10, UUID.randomUUID(), FlockSourceType.OWN_STOCK, null);
        repository.saveAndFlush(flock);

        assertThat(repository.findByFlockCode("PF-IT-1")).isPresent();
    }

    @Test
    void flockCodeUniqueConstraintIsEnforced() {
        UUID rearingHouseId = UUID.randomUUID();
        ParentFlock first = ParentFlock.register("PF-IT-2", UUID.randomUUID(), "Line", "Breed",
                LocalDate.now(), 100, 10, rearingHouseId, FlockSourceType.OWN_STOCK, null);
        repository.saveAndFlush(first);

        ParentFlock duplicate = ParentFlock.register("PF-IT-2", UUID.randomUUID(), "Line2", "Breed2",
                LocalDate.now(), 50, 5, rearingHouseId, FlockSourceType.OWN_STOCK, null);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByRearingHouseIdReturnsMatchingFlocks() {
        UUID houseId = UUID.randomUUID();
        ParentFlock flock = ParentFlock.register("PF-IT-3", UUID.randomUUID(), "Line", "Breed",
                LocalDate.now(), 100, 10, houseId, FlockSourceType.OWN_STOCK, null);
        repository.saveAndFlush(flock);

        assertThat(repository.findByRearingHouseId(houseId))
                .extracting(ParentFlock::getFlockCode)
                .contains("PF-IT-3");
    }
}
```

- [ ] **Step 2: Run it**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=ParentFlockRepositoryIT`
Expected: PASS (3 tests) — requires Docker running. If Docker is unavailable in your execution environment, note that explicitly and move on; do not skip writing the test.

- [ ] **Step 3: Commit**

```bash
git add parent-flock-service/src/test/java/com/ppms/parentflock/domain/repository/ParentFlockRepositoryIT.java
git commit -m "test(parent-flock-service): Testcontainers-backed ParentFlockRepository integration test"
```

---

## Task 17: `FlockControllerTest` — `@WebMvcTest` security + validation wiring

**Files:**
- Test: `parent-flock-service/src/test/java/com/ppms/parentflock/api/controller/FlockControllerTest.java`

**Interfaces:**
- Consumes: `FlockController` (Task 15), `SecurityConfig` (Task 14), `JwtTokenProvider`/`JwtProperties` (Task 4).

- [ ] **Step 1: Write the test**

```java
package com.ppms.parentflock.api.controller;

import com.ppms.parentflock.application.dto.response.FlockDetailResponse;
import com.ppms.parentflock.application.service.FlockService;
import com.ppms.parentflock.config.SecurityConfig;
import com.ppms.parentflock.domain.enums.FlockHealthStatus;
import com.ppms.parentflock.domain.enums.FlockPhaseStatus;
import com.ppms.parentflock.domain.enums.FlockSourceType;
import com.ppms.shared.security.JwtProperties;
import com.ppms.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlockController.class)
@Import({SecurityConfig.class, FlockControllerTest.TestBeans.class})
class FlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FlockService flockService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties props = new JwtProperties();
            props.setSecret("dev-only-secret-key-must-be-at-least-32-bytes-long-for-hs256");
            props.setExpirationMs(3_600_000L);
            return props;
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(JwtProperties props) {
            return new JwtTokenProvider(props);
        }
    }

    @Test
    void getFlockWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/flocks/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFlockWithValidTokenReturns200() throws Exception {
        UUID flockId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken("manager1", List.of("MANAGER"));
        when(flockService.getFlock(flockId)).thenReturn(new FlockDetailResponse(flockId, "PF-1", UUID.randomUUID(),
                "Line", "Breed", LocalDate.now(), FlockPhaseStatus.REARING, 100, 10, 100, 10,
                UUID.randomUUID(), FlockHealthStatus.HEALTHY, FlockSourceType.OWN_STOCK, null, 0L, List.of()));

        mockMvc.perform(get("/api/v1/flocks/{id}", flockId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void registerFlockWithoutSupervisorRoleReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("worker1", List.of("FLOCK_WORKER"));

        mockMvc.perform(post("/api/v1/flocks")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"flockCode\":\"PF-2\",\"farmSiteId\":\"" + UUID.randomUUID()
                                + "\",\"entryDate\":\"2026-01-01\",\"initialHenCount\":100,"
                                + "\"initialCockCount\":10,\"rearingHouseId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run it**

Run: `./mvnw -q -pl parent-flock-service -am test -Dtest=FlockControllerTest`
Expected: PASS (3 tests). This one does not need Docker or Postgres — `@WebMvcTest` loads only the web layer plus the explicitly `@Import`-ed `SecurityConfig` and test JWT beans, with `FlockService` mocked out.

- [ ] **Step 3: Commit**

```bash
git add parent-flock-service/src/test/java/com/ppms/parentflock/api/controller/FlockControllerTest.java
git commit -m "test(parent-flock-service): WebMvcTest proving JWT/role wiring on FlockController"
```

---

## Task 18: README + full-reactor verification

**Files:**
- Create: `README.md`

**Interfaces:** None — this task documents and verifies everything built in Tasks 1–17; it doesn't add production code.

- [ ] **Step 1: Write the root `README.md`**

```markdown
# Poultry Production Management System (PPMS)

Four independent Spring Boot services (Parent Flock, Hatchery, Farm,
Slaughterhouse) plus a shared library (`shared-kernel`), coordinated by one
Maven reactor. See `docs/parent-flock-architecture.md` and
`docs/Specs.MD` for the full design. Currently implemented: **PF1
foundation** in `parent-flock-service` (farm sites, houses, flock
lifecycle) — see `docs/superpowers/specs/2026-08-27-parent-flock-pf1-foundation-design.md`
for exactly what's in and out of scope.

## Prerequisites

- JDK 21
- Docker Desktop (for the Postgres databases, and for the
  Testcontainers-backed integration tests)

No local Maven install needed — use the wrapper (`./mvnw` / `mvnw.cmd`).

## Running `parent-flock-service` locally

**1. Start its database:**

```bash
docker compose up -d postgres-parent-flock
```

This starts Postgres 16 on `localhost:5432`, database `parent_flock_db`,
user `ppms` / password `ppms_dev` (see `docker-compose.yml`). Wait for it to
report healthy — `docker compose ps` shows `healthy` once
`pg_isready` succeeds — before starting the service, or Flyway will fail
to connect on the first attempt.

**2. Build the reactor** (installs `shared-kernel` into your local `.m2` so
`parent-flock-service` can resolve it):

```bash
./mvnw clean install -DskipTests
```

**3. Run the service:**

```bash
./mvnw spring-boot:run -pl parent-flock-service
```

It starts on `http://localhost:8080`. On first startup, Flyway runs the
migrations in `shared-kernel`'s `db/shared-migration` and
`parent-flock-service`'s `db/migration`, creating all tables and seeding
four dev users (see below).

**4. Log in and call the API:**

Every seeded user's password is `ChangeMe123!`. Usernames: `supervisor1`
(`FLOCK_SUPERVISOR`), `worker1` (`FLOCK_WORKER`), `vet1` (`FLOCK_VET`),
`manager1` (`MANAGER`).

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"supervisor1","password":"ChangeMe123!"}'
```

Copy the returned `token` and use it as a bearer token on every other
call:

```bash
curl http://localhost:8080/api/v1/farm-sites \
  -H "Authorization: Bearer <token>"
```

**5. Explore the API:** Swagger UI is at
`http://localhost:8080/swagger-ui.html` (use the "Authorize" button with
`Bearer <token>`); the raw OpenAPI document is at `/v3/api-docs`.

## Running the tests

```bash
# Unit tests only (no Docker needed) — fast, run these first
./mvnw test -pl shared-kernel,parent-flock-service -am -Dtest='!*IT'

# Everything, including the Testcontainers-backed repository test
# (needs Docker Desktop running — it starts its own ephemeral Postgres
# container, separate from the docker-compose one above)
./mvnw test -pl shared-kernel,parent-flock-service -am

# The full-context smoke test (ParentFlockServiceApplicationTests) also
# needs the docker-compose Postgres from Step 1 running.
```

## Stopping

```bash
docker compose down          # stop the databases (add -v to also wipe data)
```
```

- [ ] **Step 2: Run the full unit test suite (no Docker dependency)**

Run: `./mvnw test -pl shared-kernel,parent-flock-service -am -Dtest='!*IT'`
Expected: `BUILD SUCCESS`, all unit tests pass (every test written in Tasks 2–15 except `ParentFlockRepositoryIT`).

- [ ] **Step 3: Compile the full reactor (all four services + shared-kernel)**

Run: `./mvnw clean install -DskipTests`
Expected: `BUILD SUCCESS` for all 5 modules — confirms `hatchery-service`, `farm-service`, `slaughterhouse-service` (unmodified skeletons) still build alongside the new `shared-kernel`/`parent-flock-service` code, and that `shared-kernel` installs cleanly for `parent-flock-service` to depend on.

- [ ] **Step 4: If Docker is available, run the full suite including Testcontainers and the context-load smoke test**

```bash
docker compose up -d postgres-parent-flock
./mvnw test -pl shared-kernel,parent-flock-service -am
```

Expected: `BUILD SUCCESS`, including `ParentFlockRepositoryIT` and
`ParentFlockServiceApplicationTests`. If Docker isn't available in your
environment, skip this step and say so explicitly — don't claim it passed
without running it.

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add README with run/test instructions for parent-flock-service"
```

---

## Plan self-review notes

**Spec coverage:** Every PF1 domain-scope item from the design spec (§4) has a task: entities (Tasks 8–11), services (Tasks 12, 13, 15), endpoints (Tasks 12, 13, 15), migrations (Tasks 3, 8, 9, 10, 11), auth (Task 14), tests (every task plus 16–17). The design spec's explicit deferrals (§6) are correctly absent — no PF2–PF9, no `AuditLog`/`Alert`/`Notification`, no scope enforcement on `UserRoleAssignment`, no `HatcheryIntegrationClient`.

**Placeholder scan:** No `TBD`/`TODO` remain; every step has real code, not a description of code.

**Type consistency:** `FlockService`'s constructor parameter order (`ParentFlockRepository, ParentFlockHouseRepository, FlockBirdCountEventRepository, RearingWeightSampleRepository, RearingToProductionTransferRepository, FlockCullingRecordRepository, FlockMapper, RearingWeightSampleMapper, FlockBirdCountEventMapper`) matches exactly between Task 15's service definition and its test's manual construction. `ParentFlock`'s five behavior method signatures (`ageInWeeks`, `assertInRearingPhase`, `applyBirdCountChange`, `transferToProduction`, `applyCulling`) are identical between Task 10 (definition + test) and Task 15 (`FlockService` usage). `HouseStatus.canTransitionTo` is used consistently by `ParentFlockHouse.changeStatus` (Task 9), `HouseService.changeHouseStatus` (Task 13), and `FlockService`'s house-status calls (Task 15).

