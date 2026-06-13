# Dev2 Implementation Plan

## Context

Dev2 owns the `user-service` bounded context and the matching frontend pages listed in `SRS-Cartzilla.md`.
This document is a working scan and execution plan for implementing Dev2 scope without drifting away from the existing codebase style.

Primary backend module:

- `services/user-service`

Primary database:

- `cartzilla_user_db`

Reference implementation style:

- `services/product-service`

## Scope From Project Docs

Dev2 backend responsibilities:

- Authentication: register, login, token-related flows.
- User profile and account status.
- Address management.
- OAuth account support.
- Voucher management and voucher validation/redeem flow.
- Internal user/voucher APIs consumed by other services.
- Admin user and voucher management.

SRS traceability for Dev2:

| SRS item | Required behavior/API | Current status |
|---|---|---|
| F01 / UC-02 | `POST /api/users/register`, `POST /api/users/login`, `POST /api/users/refresh-token`, `POST /api/users/logout`; refresh token stored server-side; deactivated users rejected | Implemented: register/login, refresh rotation, logout revoke, active-user guard |
| F02 / UC-02 | `GET/PUT /api/users/me`, `GET/POST/PUT/DELETE /api/users/me/addresses`; exactly one default address | Partial: profile/address implemented; default-address test exists; delete default behavior still needs SRS A3 fix |
| F14 / UC-06 | Admin voucher CRUD, audience management, public preview validate, internal idempotent redeem, atomic used count, min account age | Partial: internal validate exists; admin CRUD/redeem/audience still missing |
| F15 / UC-02 | OAuth authorize/callback for Google/Facebook via `OAuthAccount` | Implemented backend authorize/callback flow for configured Google/Facebook providers |
| F17 / UC-02 | Admin user list/detail, role update, status update | Implemented: admin list/detail, role update, status update, last-active-admin guard |
| UC-02 A1 / F12 | Forgot/reset password email, reset link valid 30 minutes | Implemented with `password_reset_tokens`, one-time reset, notification-service email handoff |

Dev2 frontend pages from SRS:

- `/login`
- `/register`
- `/oauth/callback`
- `/forgot-password`
- `/profile`
- `/addresses`
- `/admin/vouchers`
- `/admin/users`

This repo currently contains backend only in this workspace. Frontend work should be planned after confirming the frontend location or repository.

## Current Backend State

### Already Present

`user-service` already has the main DDD/Hexagonal folder layout:

- `api/controller`
- `api/dto`
- `api/exception`
- `application/command`
- `application/usecase`
- `domain/entity`
- `domain/repository`
- `domain/vo`
- `infrastructure/adapter`
- `infrastructure/persistence`

Implemented or partially implemented API/controllers:

- `AuthController`
  - `POST /api/users/register`
  - `POST /api/users/login`
- `InternalUserController`
  - `GET /api/internal/users/{userId}`
  - `GET /api/internal/users/{userId}/default-address`
  - `GET /api/internal/users/{userId}/contact`
- `InternalVoucherController`
  - `GET /api/internal/vouchers/{code}/validate`

Implemented use cases:

- `RegisterUserUseCase`
- `LoginUseCase`

Implemented domain entities:

- `User`
- `Address`
- `RefreshToken`
- `OAuthAccount`
- `Voucher`
- `VoucherUsage`
- `VoucherAllowedUser`

Implemented value objects/enums:

- `Email`
- `Phone`
- `Money`
- `Role`
- `OAuthProvider`
- `DiscountType`
- `VoucherAudienceType`

Implemented persistence:

- `UserJpaRepository`
- `AddressJpaRepository`
- `VoucherJpaRepository`
- `VoucherUsageJpaRepository`
- `VoucherAllowedUserJpaRepository`
- `UserRepositoryAdapter`

Schema:

- `V1__init.sql` creates the expected Dev2 tables and indexes.
- Voucher code has a functional unique index on `upper(code)`.
- Voucher usage has idempotency protection by unique `(voucher_id, order_id)`.

Security/config:

- `SecurityConfig` provides password encoder and auditing.
- `common-security` is used for JWT generation.

### Important Gaps

The current implementation is not yet complete for Dev2 scope.

API/application gaps:

- No public profile API yet.
- No address CRUD API yet.
- No admin user API yet.
- No admin voucher CRUD API yet.
- Refresh/logout token flow implemented in Phase 4.
- Forgot/reset password flow implemented in Phase 4.
- OAuth authorize/callback/linking flow implemented in Phase 4.
- Voucher validation exists, but redeem/commit is not implemented.
- Internal voucher validation uses JPA repositories directly instead of application use cases.
- Internal user endpoints use `AddressJpaRepository` directly instead of a domain port/usecase.

Domain/repository gaps:

- Only `UserRepository` exists as a domain port.
- No `AddressRepository` port.
- No `VoucherRepository` port.
- No `VoucherUsageRepository` port.
- `OAuthAccountRepository`, `RefreshTokenRepository`, and `PasswordResetTokenRepository` ports now exist.
- `Address` has create/default methods but no update method.
- `User` supports activation/password/email changes, but profile update and role changes need explicit domain methods.
- `Voucher` supports create/redeemable/increment/deactivate, but update behavior and no-code-change rule need explicit handling.

Testing gaps:

- Focused `user-service` tests exist for profile, address, vouchers, admin users, auth lifecycle, and OAuth.
- Remaining test gap: provider-level OAuth HTTP exchange tests can be added with a mock HTTP server when needed.

Gateway/API exposure gaps:

- Gateway routes `/api/users/**`, `/api/vouchers/**`, `/api/admin/users/**` to user-service.
- Admin voucher route is not visible in gateway route list yet unless it is placed under an existing path.
- `/api/internal/**` should remain service-internal and not be exposed through gateway.

## Product-Service Style To Follow

Use `product-service` as the local implementation pattern:

- Keep API path constants in an `ApiPaths` class instead of scattering string literals.
- Use nested DTO records per feature, for example `UserDtos`, `AddressDtos`, `VoucherDtos`.
- Request DTOs expose `toCommand()`.
- Response DTOs expose `from(entity)`.
- Controllers stay thin and call use cases.
- Use cases contain application orchestration and transaction boundaries.
- Domain repositories are interfaces under `domain/repository`.
- JPA repositories stay under `infrastructure/persistence`.
- Adapters under `infrastructure/adapter` implement domain ports.
- Prefer domain methods for state changes instead of setting fields from use cases.
- Return `ApiResponse` consistently, with `ResponseEntity` only where status code differs from 200.

Target dependency direction:

```text
api -> application -> domain <- infrastructure
```

Avoid introducing controller-to-JPA coupling in new work. Existing direct JPA usage in internal controllers can be refactored gradually when the matching use cases are added.

## Proposed API Surface

### Auth And Account

- `POST /api/users/register`
- `POST /api/users/login`
- `POST /api/users/refresh-token`
- `POST /api/users/logout`
- `POST /api/users/forgot-password`
- `POST /api/users/reset-password`
- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/me/password`

### Addresses

- `GET /api/users/me/addresses`
- `POST /api/users/me/addresses`
- `PUT /api/users/me/addresses/{id}`
- `DELETE /api/users/me/addresses/{id}`
- `PUT /api/users/me/addresses/{id}/default`

SRS UC-02 A3 says deleting the default address while other addresses exist must require switching default first. The current Phase 1 implementation auto-promotes another address; this must be changed to SRS behavior.

### Admin Users

- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/role`
- `PUT /api/admin/users/{id}/status`

### Admin Vouchers

Prefer one gateway-consistent path:

- `GET /api/admin/vouchers`
- `GET /api/admin/vouchers/{id}`
- `POST /api/admin/vouchers`
- `PUT /api/admin/vouchers/{id}`
- `DELETE /api/admin/vouchers/{id}`
- `POST /api/admin/vouchers/{id}/allowed-users`
- `DELETE /api/admin/vouchers/{id}/allowed-users/{userId}`

### Internal APIs

Keep internal endpoints out of public gateway exposure:

- `GET /api/internal/users/{userId}`
- `GET /api/internal/users/{userId}/default-address`
- `GET /api/internal/users/{userId}/contact`
- `GET /api/internal/vouchers/{code}/validate`
- `POST /api/internal/vouchers/{code}/redeem`

SRS traceability lists one customer-facing voucher endpoint and one internal redeem endpoint:

- `POST /api/vouchers/validate`
- `POST /api/internal/vouchers/redeem`

Redeem should remain service-to-service because it is only valid after checkout/payment success.

## Execution Plan

### Phase 1 Completion Criteria

Phase 1 is complete when:

- Profile read/update APIs exist for the current user.
- Address CRUD APIs exist for the current user.
- Use cases enforce active-user checks.
- The first address becomes default automatically.
- Creating or updating an address as default unsets the previous default.
- Deleting a default address is rejected when other addresses still exist.
- Address operations cannot affect another user's address.
- Focused use case tests cover the rules above.

### Phase 0 - Align Structure

- Add `ApiPaths` for `user-service`.
- Add DTO groups for user/profile/address/voucher/admin/internal responses.
- Add missing domain repository ports and adapters.
- Refactor new code to use use cases first; only refactor existing internal controllers when needed by adjacent work.

Deliverable:

- No behavior change or minimal behavior change.
- Compile passes for `user-service`.

### Phase 1 - Profile And Address

- Implement current-user profile read/update.
- Implement address CRUD.
- Enforce one default address per user.
- Make first address default by default when appropriate.
- Reject deletion of a default address when other addresses exist; require switching default first.
- Add use cases:
  - `GetProfileUseCase`
  - `UpdateProfileUseCase`
  - `ListAddressesUseCase`
  - `CreateAddressUseCase`
  - `UpdateAddressUseCase`
  - `DeleteAddressUseCase`
  - `SetDefaultAddressUseCase`

Testing focus:

- Address creation validation.
- Default-address invariant.
- Default-address delete rejection when other addresses exist.
- User active guard where needed.

### Phase 2 - Voucher Admin And Internal Redeem

Phase 2 is complete when:

- Voucher repository ports/adapters exist for voucher, usage, and allowed users.
- Voucher validation is handled in an application use case, not directly in controllers.
- Public voucher preview endpoint exists and does not mutate `usedCount`.
- Admin voucher CRUD exists.
- Admin can manage `SPECIFIC_USERS` allowed-user entries.
- Internal redeem endpoint is idempotent by `(voucherId, orderId)`.
- Redeem increments `usedCount` with an atomic conditional update.
- Order Saga calls internal redeem after payment success and before confirming the order.
- Focused tests cover validation, audience, min account age, idempotent redeem, duplicate voucher code, and duplicate allowed user.

- Implement voucher CRUD for admin.
- Implement allowed-user management for `SPECIFIC_USERS`.
- Move voucher validation into an application use case.
- Implement voucher redeem endpoint for order-service.
- Implement public voucher preview endpoint `/api/vouchers/validate`.
- Implement internal voucher redeem endpoint `/api/internal/vouchers/redeem`.
- Ensure redeem is idempotent by `(voucher_id, order_id)`.
- Use conditional `usedCount` update to avoid race conditions.

Testing focus:

- Percentage/fixed discount calculation.
- Minimum order amount.
- Time window.
- Per-user limit.
- Max uses race-safe behavior.
- Idempotent redeem by order.
- Audience rules for `ALL_USERS`, `NEW_CUSTOMER`, `LOYAL_CUSTOMER`, `SPECIFIC_USERS`.

### Phase 3 - Admin Users

- Implement admin user list/detail.
- Implement activate/deactivate user.
- Implement role update with guardrails.
- Add paging/filtering similar to product list patterns where useful.

Testing focus:

- Role change validation.
- Deactivation prevents internal checkout/voucher validation.
- Last active admin cannot be demoted or deactivated.

### Phase 4 - Token, Forgot Password, OAuth

Phase 4 is complete for backend scope.

- Added refresh token persistence and `POST /api/users/refresh-token`.
- Added `POST /api/users/logout` to revoke refresh tokens.
- Refresh/login reject deactivated users.
- Reset password revokes active refresh tokens.
- Added full forgot/reset password token flow with 30-minute reset links.
- Added notification-service internal reset-password email endpoint.
- Added OAuth authorize/callback backend for configured Google/Facebook providers.

Testing focus:

- Refresh token rotation/revocation.
- OAuth account uniqueness and inactive-user guard.
- Password reset token one-time use and refresh-token revocation.

## Suggested First Implementation Slice

Start with Phase 0 plus Phase 1.

Reason:

- It completes a visible Dev2 feature area: profile/address.
- It establishes `user-service` structure matching `product-service`.
- It creates repository ports/adapters that later voucher/admin work can reuse.
- It is lower risk than voucher redeem because it does not touch cross-service checkout first.

## Risks And Notes

- Current docs and comments contain mojibake in terminal output. Avoid rewriting large Vietnamese docs/comments unless necessary.
- `SecurityConfig` currently has a TODO for auditor from `X-User-Id`; this matters for audit fields but should not block feature implementation.
- Current controllers do not yet show a standard way to read current user from gateway headers. Decide once and reuse it across profile/address/admin guard code.
- Internal endpoints should not be routed through gateway. Verify gateway behavior before exposing any `/api/internal/**` path.
- Voucher redeem must be treated as a transactional/idempotent operation because order-service may retry.
- Keep database-per-service boundaries: no cross-service joins, only UUID references and snapshots.

## Working Checklist

- [x] Create `ApiPaths` for `user-service`.
- [x] Add `AddressRepository` port and adapter.
- [x] Add `VoucherRepository` and related ports/adapters.
- [x] Implement profile DTOs/usecases/controller.
- [x] Implement address DTOs/usecases/controller.
- [x] Change default-address delete behavior to SRS A3.
- [x] Add focused tests for address invariants.
- [x] Add test for rejecting default-address delete when other addresses exist.
- [x] Add focused tests for profile update and inactive-user guard.
- [x] Implement admin voucher CRUD.
- [x] Implement internal voucher redeem.
- [x] Add voucher tests.
- [x] Integrate order-service Saga with voucher redeem after payment success.
- [x] Implement admin user management.
- [x] Add admin user tests.
- [x] Implement refresh/logout token flow.
- [x] Implement forgot/reset password flow.
- [x] Implement OAuth authorize/callback backend flow.

## Post Phase 4 Notes

Phase 4 backend implements the remaining SRS auth account flows for Dev2.

Implemented API:

- `POST /api/users/refresh-token`
- `POST /api/users/logout`
- `POST /api/users/forgot-password`
- `POST /api/users/reset-password`
- `GET /api/oauth/{provider}/authorize`
- `GET /api/oauth/{provider}/callback?code=...`
- `POST /api/internal/notifications/reset-password-email`

Current behavior:

- Login now stores a server-side refresh token and returns both access and refresh tokens.
- Refresh token use rotates the refresh token and revokes the old token.
- Logout revokes the submitted refresh token.
- OAuth-only users cannot use password login.
- Forgot password does not reveal whether an email exists.
- Reset password requires a valid, unused reset token, changes the password hash, marks the reset token used, and revokes active refresh tokens.
- OAuth callback creates a verified customer if needed, links the provider account, records last login, and issues access/refresh tokens.
- OAuth provider settings are externalized under `oauth.providers.google` and `oauth.providers.facebook`.

Testing:

- Added `AuthLifecycleUseCaseTest`.
- Added `OAuthUseCaseTest`.
- Focused Phase 4 test run: 7 tests passed.
- Full related reactor run passed for `user-service`, `notification-service`, and `api-gateway`: `user-service` 30 tests passed, gateway/notification compiled.
- Startup smoke was checked on a clean runtime DB `cartzilla_user_phase4`; Flyway applied V1 and V2 and the app reached Tomcat startup in foreground.

Runtime notes:

- Existing `cartzilla_user_db` has an older Flyway V1 checksum, so direct runtime startup against that DB fails validation until the DB is repaired or recreated.
- Compose currently declares application service builds but no Dockerfiles exist in the repo, so full `docker compose up user-service` is not available yet.
- OAuth callback with real providers requires client credentials and redirect URIs via environment variables.

## Post Phase 3 Notes

Phase 3 backend implements admin user management for the current SRS scope.

Implemented API:

- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/role`
- `PUT /api/admin/users/{id}/status`

Current behavior:

- Admin endpoints require `X-User-Id` from gateway and verify that the user is active `ADMIN` in `user-service`.
- Gateway `JwtAuth` validates the JWT, removes any client-supplied `X-User-Id` / `X-User-Role`, then injects trusted values from token claims.
- User list supports pagination/filtering: `q`, `role`, `active`, `page`, `limit`, `sort`.
- User list defaults to `page=0`, `limit=20`, `sort=email,asc`; `limit` is capped at 100.
- Role update supports `CUSTOMER`, `STAFF`, and `ADMIN`.
- Status update activates or deactivates users.
- The last active admin cannot be demoted away from `ADMIN`.
- The last active admin cannot be deactivated.

Testing:

- Added `AdminUserUseCaseTest`.
- Latest `user-service` test run: 23 tests passed.
- Latest `api-gateway` reactor test/build passed after adding gateway `SecurityConfig`.
- Runtime gateway test passed:
  - no token returns 401,
  - customer token with spoofed `X-User-Id` / `X-User-Role` is rejected,
  - admin JWT can list users with pagination/filtering and update user role through gateway.

Remaining outside Phase 3:

- Refresh/logout token flow, forgot/reset password, and OAuth.
- Full method-level Spring Security inside `user-service`. Current security boundary is gateway JWT plus downstream DB-backed admin guard.

## Post Phase 1 And Phase 2 Notes

These notes capture the important follow-up context after completing and runtime-testing Phase 1 and Phase 2.

### Completion Status

- Phase 1 profile/address backend is complete for current scope.
- Phase 2 voucher backend and order-service redeem integration are complete for current scope.
- Frontend has not been implemented yet by request.
- Admin user management, refresh/logout token flow, forgot/reset password, and OAuth remain outside Phase 1/2 and are still pending.

### Runtime Verification

Runtime tests were executed against real Spring Boot services and real PostgreSQL/RabbitMQ dependencies.

Verified Phase 1:

- Register user.
- `GET /api/users/me`.
- `PUT /api/users/me`.
- `POST /api/users/me/addresses`.
- `GET /api/users/me/addresses`.
- `PUT /api/users/me/addresses/{id}/default`.
- `DELETE /api/users/me/addresses/{id}`.
- First address becomes default automatically.
- Setting a new default unsets the previous default.
- Deleting the current default address while another address exists is rejected.

Verified Phase 2 voucher API:

- `POST /api/admin/vouchers`.
- `POST /api/vouchers/validate`.
- `POST /api/internal/vouchers/redeem`.
- Redeem is idempotent by `(voucherId, orderId)`.
- `perUserLimit` is enforced after redeem.

Verified Phase 2 order integration:

- `order-service` checkout by SKU with voucher.
- `product-service` stock/price snapshot participates in checkout.
- `payment-service` COD payment succeeds.
- Order Saga reaches `COMPLETED` / `DONE`.
- Order reaches `CONFIRMED`.
- Payment reaches `PAID`.
- Voucher is redeemed by the saga after payment success.
- Calling internal redeem again with the same `orderId` returns `idempotent=true`.

Latest focused test command:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd' -pl services/user-service,services/order-service -am test
```

Result:

- `user-service`: 16 tests passed.
- `order-service`: 8 tests passed.
- Reactor build: success.

### API Behavior To Remember

- Current-user APIs read user identity from `X-User-Id`.
- Admin voucher APIs currently require `X-User-Role: ADMIN`.
- Gateway should not expose `/api/internal/**`.
- Public voucher validate is a preview operation and must not mutate `usedCount`.
- Internal voucher redeem is the only operation that writes `VoucherUsage` and increments `usedCount`.
- Internal redeem endpoint is `POST /api/internal/vouchers/redeem`, not `POST /api/internal/vouchers/{code}/redeem`.
- Some business-rule failures currently return `400` through the shared exception handler, while SRS text mentions `422` in several places. Decide later whether to align the shared handler/status codes globally.

### Domain And Data Rules To Preserve

- There must be exactly one default address per user when the user has at least one address.
- The first created address is default even if the request sends `defaultAddress=false`.
- Deleting a default address is rejected when other addresses exist; the user must set another default first.
- Voucher code is normalized to uppercase and must remain case-insensitive unique.
- Voucher preview discount is snapshotted into order during checkout.
- Voucher redeem happens only after successful payment/saga progress, not during preview.
- `VoucherUsage.orderId` is a reference-only cross-service ID, not a cross-database FK.
- `usedCount` must only increase through the redeem flow and must be protected by conditional update.

### Integration Notes

- `order-service` uses Feign discovery for `user-service`, so runtime saga testing needs Eureka or a future explicit user-service URL config.
- `order-service` uses explicit `clients.product-service.url`, so local runtime can point it at `http://localhost:8082`.
- `shippingAddress` in checkout must be a JSON object string, not a plain text address.
- Product seed SKU used in runtime test: `TSN-001-M-WHT`.
- Runtime test voucher used a fixed discount so expected order total was easy to verify: `199000 - 50000 = 149000`.

### Environment Notes

- The repository currently has no Dockerfiles for services/infra modules, but `docker-compose.yml` uses `build: ./...`. Because of that, `docker compose up --build user-service` is not currently usable.
- Runtime testing used Docker for PostgreSQL/RabbitMQ and Maven for Spring Boot services.
- Maven is available through IntelliJ's bundled Maven on this machine:

```text
C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd
```

- The default `postgres-user` Docker volume had a Flyway checksum mismatch for `V1__init.sql`. For runtime tests, a separate Compose project `cartzilla_rt` with clean volumes was used.
- Do not repair or drop the default volume casually; verify with the team first because it may contain local dev data.
- The parent POM still warns that `maven-compiler-plugin` has no explicit version. Tests pass, but this should be cleaned up later.
- Mockito prints dynamic Java agent warnings on newer JDKs. Tests pass, but the build can be adjusted later if the team wants warning-free output.

### Recommended Next Work

- Phase 3 should start with admin user management because it completes remaining Dev2 admin backend scope.
- After Phase 3, decide demo depth for refresh/logout, forgot/reset password, and OAuth.
- Before frontend work, confirm gateway security behavior for `X-User-Id` and `X-User-Role` headers so UI calls match real auth flow.
