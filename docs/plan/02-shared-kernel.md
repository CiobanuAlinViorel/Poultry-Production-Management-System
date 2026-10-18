# 02 — shared-kernel (library)

Module `backend/shared-kernel`, package root `com.ppms.shared`. Spring Boot
auto-configuration library (no `@SpringBootApplication`). Everything here is generic;
**no business entity** (no User, Alert, Employee — those belong to their services).
Existing classes (`JwtKeyProvider`, `JwtSecurityAutoConfiguration`, `RoleClaim`,
`UserPrincipal`, `ScopeType`) are kept and extended.

Depends on: `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`,
`spring-kafka`, `jjwt-api/impl/jackson`, `jackson-datatype-jsr310`, `micrometer-core`.
Test-jar classifier `tests` published for `test-support`.

---

## 1. Package map

```
com.ppms.shared
├── persistence      BaseEntity, AuditableEntity listener, UuidV7Generator, JpaConfig (auditing)
├── domain           ScopeType, Money (BigDecimal RON, 4dp), Quantity/Unit enums, CodeGenerator port
├── exception        DomainException(code), EntityNotFoundException, BusinessRuleViolationException,
│                    ValidationException, ConcurrencyConflictException, ForbiddenScopeException
├── web              ApiErrorResponse, FieldValidationError, PageResponse<T>, PageRequestParams,
│                    GlobalExceptionHandler (@ControllerAdvice, @ConditionalOnMissingBean),
│                    RequestContextFilter (traceId/userId MDC), CorrelationId
├── security         JwtKeyProvider, JwtTokenValidator, JwtAuthenticationFilter, UserPrincipal,
│                    RoleClaim, ScopeGuard (@PreAuthorize helper bean "scope"), SecurityDefaults,
│                    CurrentUser (resolver), InternalCallAuthenticator (service-to-service header)
├── events           DomainEvent<T>, EventEnvelope, EventTypes (constants), Topics (constants),
│                    OutboxEntity, OutboxRepository, OutboxPublisher (@Transactional save),
│                    OutboxRelay (@Scheduled poller → KafkaTemplate), ProcessedEventEntity,
│                    IdempotentConsumer (dedupe helper), KafkaConfig (JSON serde, error handler, DLT)
├── audit            Audited (annotation), AuditInterceptor (@Aspect), AuditLogEntity,
│                    AuditLogRepository (save/find only), AuditEventPublisher (→ ppms.audit topic)
├── settings         SettingsClient (reads admin-service settings with cache + defaults),
│                    SettingKeys (constants per service), @SiteSetting resolver
├── pdf              PdfRenderer (openhtmltopdf, Thymeleaf templates), DocumentStorage (S3/MinIO port)
├── export           CsvExporter, XlsxExporter (streaming), ExportColumn metadata
├── util             DateTimeUtil (site tz), Percent, MathUtil (safe division), CodeFormat
└── testsupport      (test-jar) AbstractIntegrationTest (Postgres+Kafka containers),
                     JwtTestTokens (mint RS256 test tokens), KafkaTestConsumer, TestClock
```

Auto-config registration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
`JwtSecurityAutoConfiguration`, `WebAutoConfiguration`, `OutboxAutoConfiguration`,
`AuditAutoConfiguration`, `SettingsAutoConfiguration`, `PdfAutoConfiguration` (conditional
on class), `ExportAutoConfiguration`.

---

## 2. Persistence

### `BaseEntity` (`@MappedSuperclass`)
| field | type | notes |
|---|---|---|
| id | UUID | `@Id`, assigned in `@PrePersist` if null (UUID v7 for index locality) |
| createdAt / updatedAt | Instant | `@CreatedDate`/`@LastModifiedDate` (JPA auditing) |
| createdBy / updatedBy | UUID | from `UserPrincipal`, nullable for system jobs |
| version | long | `@Version` optimistic lock → 409 `CONCURRENCY_CONFLICT` |

`equals/hashCode` on id only. `JpaConfig` enables `@EnableJpaAuditing(auditorAwareRef =
"currentUserAuditor")`.

### Column conventions for migrations
`uuid` PK, `timestamptz` for instants, `date` for business dates, `numeric(12,3)` for
weights/kg/L, `numeric(14,4)` for money, `integer` for counts, `varchar(32)` for enums
(string mapping, never ordinal), `jsonb` for payloads. Every table: `created_at`,
`updated_at`, `created_by`, `updated_by`, `version`.

---

## 3. Errors & web

`DomainException(String code, String message, Map<String,Object> details)` — base.
HTTP mapping in `GlobalExceptionHandler`:

| Exception | HTTP | code examples |
|---|---|---|
| EntityNotFoundException | 404 | `NOT_FOUND` |
| ValidationException / MethodArgumentNotValid | 400 | `VALIDATION_FAILED` + `fieldErrors` |
| BusinessRuleViolationException | 422 | `FARM_WITHDRAWAL_ACTIVE`, `HOUSE_NOT_EMPTY`, ... |
| ConcurrencyConflictException / OptimisticLock | 409 | `CONCURRENCY_CONFLICT` |
| ForbiddenScopeException / AccessDenied | 403 | `FORBIDDEN_SCOPE` |
| DataIntegrityViolation (unique) | 409 | `DUPLICATE` with constraint→code map |
| anything else | 500 | `INTERNAL_ERROR` (no stack trace in body) |

Envelope: `{ timestamp, status, error, code, message, path, traceId, fieldErrors:[{field,
rejectedValue, message}], details:{} }`. Messages are English technical text; the
frontend translates by `code`.

`PageResponse<T> { content, page, size, totalElements, totalPages, sort }`.
`PageRequestParams` caps `size` at 100 and whitelists sort fields per endpoint
(`SortWhitelist` annotation) to prevent injection via sort.

---

## 4. Security

- `JwtKeyProvider`: loads public key (all services) and private key (identity only)
  from `*_FILE` → env PEM → yml; supports `kid` list for rotation.
- `JwtTokenValidator`: RS256 verify, `exp`/`nbf`, issuer `ppms-identity`, returns
  `UserPrincipal { userId, username, displayName, roles: List<RoleClaim{role, scopeType,
  scopeId}>, perms: Set<String>, mustChangePassword, locale }`.
- `JwtAuthenticationFilter`: Bearer header; also accepts `?access_token=` **only** for
  document download routes flagged by `@AllowTokenQueryParam`.
- `SecurityDefaults`: a `SecurityFilterChain` bean (`@ConditionalOnMissingBean`) that
  permits `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/.well-known/**`,
  requires auth for the rest, stateless, CSRF off, CORS from gateway only.
- `ScopeGuard` bean named `scope` for SpEL:
  `@PreAuthorize("@scope.has('FARM_VET','FARM', #farmId)")`,
  `@scope.anyRoleIn('FARM', #farmId, 'FARM_MANAGER','FARM_SUPERVISOR')`,
  `@scope.hasPerm('farm:treatment:write', 'FARM', #farmId)`. `GLOBAL` assignments and
  `SYSTEM_ADMIN`/`MANAGER` (read) satisfy any scope per rules in 04 §4.
- `@CurrentUser UserPrincipal` controller argument resolver.
- `InternalCallAuthenticator`: for the few sync internal calls, header
  `X-Internal-Token` (HMAC of timestamp with shared secret) + source IP allow-list;
  endpoints under `/internal/**` require it.

---

## 5. Domain events, outbox, Kafka

### 5.1 Envelope (JSON, Kafka value)
```json
{
  "eventId": "uuid", "eventType": "farm.lot.created", "version": 1,
  "occurredAt": "2026-10-17T06:00:00Z", "producer": "farm-service",
  "aggregateType": "Lot", "aggregateId": "uuid", "siteId": "uuid",
  "actorId": "uuid|null", "correlationId": "uuid", "causationId": "uuid|null",
  "payload": { ... }
}
```
Kafka key = `aggregateId` (ordering per aggregate). Headers: `eventType`, `version`.

### 5.2 Topics (constants in `Topics`)
| Topic | Producer | Consumers | Notes |
|---|---|---|---|
| `ppms.identity.user` | identity | admin, notification, reporting | user.created/locked/unlocked/disabled/password-changed |
| `ppms.admin.role-assignment` | admin | identity (cache bust), reporting | assigned/revoked/expired |
| `ppms.admin.employee` | admin | all domain services (employee_directory) | created/updated/deactivated; **compacted** |
| `ppms.admin.settings` | admin | all | setting.changed (cache invalidation) |
| `ppms.site.directory` | every domain service | admin, all others | `<type>.site.registered/updated` — **compacted**, key=siteId |
| `ppms.parent-flock.flock` | parent-flock | reporting | registered/transferred/depopulated/count-changed |
| `ppms.parent-flock.production` | parent-flock | reporting | daily-sheet.recorded, weekly-summary.computed |
| `ppms.parent-flock.egg-transfer` | parent-flock | hatchery, reporting | planned/approved/dispatched/cancelled |
| `ppms.hatchery.egg-reception` | hatchery | parent-flock, reporting | received/rejected (closes EggTransferOrder) |
| `ppms.hatchery.incubation` | hatchery | reporting | batch.set, candling.recorded, transferred, hatched |
| `ppms.hatchery.chick-transfer` | hatchery | farm, reporting | planned/approved/dispatched/cancelled |
| `ppms.farm.chick-reception` | farm | hatchery, reporting | received/rejected |
| `ppms.farm.lot` | farm | reporting, slaughterhouse (directory of lots) | created/thinned/closed/kpi.computed |
| `ppms.farm.daily-ops` | farm | reporting | daily-sheet.recorded, treatment.recorded, withdrawal.ending |
| `ppms.farm.delivery` | farm | slaughterhouse, reporting | planned/confirm-requested/dispatched/cancelled, fci.issued |
| `ppms.slaughterhouse.reception` | slaughterhouse | farm, reporting | confirmed/received/discrepancy |
| `ppms.slaughterhouse.processing` | slaughterhouse | reporting | session.closed, grading.recorded, condemnation |
| `ppms.slaughterhouse.cold-chain` | slaughterhouse | reporting | stock-in/out, temperature.breach |
| `ppms.slaughterhouse.sales` | slaughterhouse | reporting | dispatch.confirmed, invoice.issued |
| `ppms.inventory` | all domain services | reporting, notification | stock.received/consumed/below-reorder/expiring |
| `ppms.alerts` | all | notification, reporting | alert.raised/resolved (severity, code, scope, i18n params) |
| `ppms.notification` | notification | reporting | notification.sent (channel) |
| `ppms.audit` | all | reporting | audit.recorded |
| `*.DLT` | framework | ops | dead letters after 3 retries |

### 5.3 Outbox
`outbox_event (id uuid pk, topic, key, headers jsonb, payload jsonb, created_at,
published_at null, attempts int, last_error)` — **created by each service's own
migration V001** (shared-kernel ships the SQL snippet in
`resources/db/shared/outbox.sql` to copy). `OutboxPublisher.publish(DomainEvent)` is
called inside the service transaction. `OutboxRelay` polls `published_at is null order
by created_at limit :batch` with `FOR UPDATE SKIP LOCKED`, sends via `KafkaTemplate`
(sync `get()`), marks published; metrics `ppms_outbox_pending`. Purge job deletes
published rows older than 7 days.

### 5.4 Consumers
`@KafkaListener` classes in `infrastructure/messaging`. Wrap handling in
`IdempotentConsumer.process(eventId, consumerName, () -> …)` which inserts into
`processed_event (event_id, consumer, processed_at)` (same migration snippet) and skips
duplicates. `DefaultErrorHandler` with 3 retries (1 s, 5 s, 30 s) then `DeadLetterPublishingRecoverer`.
Consumers never call other services synchronously.

### 5.5 Alerts contract (payload of `alert.raised`)
```json
{ "alertCode": "FARM_FCR_ABOVE_STANDARD", "severity": "WARNING|INFO|CRITICAL",
  "scopeType": "FARM", "scopeId": "uuid", "entityType": "Lot", "entityId": "uuid",
  "params": {"lotCode":"F1-H3-2026-10-01","fcr":1.92,"standard":1.70},
  "targetRoles": ["FARM_MANAGER","FARM_VET"], "dedupKey": "FARM_FCR|lotId|week" }
```
`AlertPublisher.raise(...)` helper in shared-kernel; `notification-service` persists and
fans out (see 09).

---

## 6. Audit

`@Audited(action = "FARM_TREATMENT_CREATE", entity = "TreatmentRecord", idExpression =
"#result.id")` on service methods. `AuditInterceptor` (AOP `@Around`) writes
`audit_log (id, actor_id, actor_name, action, entity_type, entity_id, site_id,
ip_address, user_agent, request_payload jsonb (DTO, secrets masked), outcome, error_code,
occurred_at)` in the **same transaction** and publishes `audit.recorded`. Repository
exposes `save` + reads only; migration adds a trigger `prevent_audit_mutation` raising
on UPDATE/DELETE (NFR-S-06). Table created by each service's V001 (snippet provided).

---

## 7. Settings client

`SettingsClient.get(String key, ScopeType, UUID scopeId, T default)` → tries
`site`-scoped value, then `GLOBAL`, then default. Backed by Caffeine cache (10 min)
and invalidated by `ppms.admin.settings` events; falls back to defaults if admin-service
unreachable (log WARN once). Keys declared per service in `SettingKeys` (see each
service file §"Settings").

---

## 8. PDF & export

- `PdfRenderer.render(templateName, model, locale) → byte[]` using Thymeleaf HTML + CSS
  print stylesheet + openhtmltopdf; fonts embedded (DejaVu Sans for diacritics).
- `DocumentStorage.put(key, bytes, contentType) → DocumentRef{bucket,key,sha256,size}`;
  implementations `S3DocumentStorage` (MinIO/AWS) and `LocalFsDocumentStorage` (tests).
- `CsvExporter.write(List<ExportColumn<T>>, Stream<T>, OutputStream)` UTF-8 BOM, `;`
  separator (Excel RO locale), `XlsxExporter` with SXSSF streaming; both used by the
  generic `GET …/export?format=csv|xlsx` support in each list controller via
  `ExportSupport` helper.

---

## 9. Test support (`shared-kernel-tests.jar`)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT) @ActiveProfiles("test") @Testcontainers
public abstract class AbstractIntegrationTest {
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));
  @DynamicPropertySource static void props(DynamicPropertyRegistry r) { … datasource, kafka, jwt test keys … }
  protected String tokenFor(String role, ScopeType st, UUID scopeId) { return JwtTestTokens.mint(...); }
}
```
Containers are `static` + `withReuse(true)` to share across test classes. `TestClock`
is a `MutableClock` bean overriding `Clock` in `test` profile so date logic is
deterministic. `KafkaTestConsumer.await(topic, eventType, predicate, timeout)`.

ArchUnit rules shipped as `PpmsArchitectureRules` (import in each service's
`ArchitectureTest`): controllers only call application services; domain does not depend
on Spring web; no class imports another service's package; `@Transactional` only in
application layer; every `@RestController` method under `/api` is covered by
`@PreAuthorize` at service level (checked via a custom condition).

---

## 10. Task list

| # | Task | Acceptance |
|---|---|---|
| K1 | Persistence: `BaseEntity`, UUID v7, auditing config, SQL snippets (`outbox.sql`, `processed_event.sql`, `audit_log.sql`) | identity-service compiles against it; snippet applied in a test schema |
| K2 | Exceptions + `GlobalExceptionHandler` + `PageResponse` + sort whitelist | unit tests for every mapping; 422 body matches envelope |
| K3 | Security: extend `JwtKeyProvider` (file/env/yml, kid), `JwtTokenValidator`, filter, `SecurityDefaults`, `ScopeGuard`, `@CurrentUser`, `InternalCallAuthenticator` | tests: valid/expired/wrong-key tokens; scope SpEL matrix (GLOBAL, site match, mismatch, MANAGER read-only) |
| K4 | Events: envelope, `Topics`, `OutboxPublisher`, `OutboxRelay`, `IdempotentConsumer`, Kafka JSON serde config, DLT | IT: publish in tx → relayed to Kafka container; duplicate eventId processed once; poison message to DLT after 3 retries |
| K5 | Audit: annotation, aspect, entity, immutability trigger, `audit.recorded` event | IT: annotated method writes row; UPDATE on audit_log raises |
| K6 | `SettingsClient` with cache, fallback, invalidation listener | unit tests with WireMock stub of admin-service |
| K7 | `PdfRenderer`, `DocumentStorage` (S3 + local), `CsvExporter`, `XlsxExporter` | sample template renders RO diacritics; CSV opens in Excel RO |
| K8 | `testsupport` jar: containers base, `JwtTestTokens`, `TestClock`, `KafkaTestConsumer`, ArchUnit rules | used by identity-service tests in 03 |
| K9 | README for the module documenting how a service consumes each piece | — |

## Exit criteria
- `./mvnw -pl shared-kernel verify` green with ≥ 80 % coverage.
- identity-service and admin-service switched to `SecurityDefaults` + `GlobalExceptionHandler` without behaviour change.
