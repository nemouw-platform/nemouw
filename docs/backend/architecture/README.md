# Package-by-feature architecture

How the Nemouw backend is organized, which packages may depend on which, and what is public on purpose.

## 1. The idea

Code is grouped by **feature**, not by technical layer. Everything a feature needs (controller, service, repository, DTOs, domain types) lives in one package, so a change to a feature touches one place.

Two rules keep the boundaries real:

- **Package-private by default.** A class or method is `public` only when code in another package uses it. The compiler then enforces the boundaries.
- **Modules where it matters.** The two largest features, `user` and `syndicate`, are Spring Modulith application modules. `ModularityTests` fails the build if a module reaches into another module's internals.

## 2. Layout

```
com.nemouw
├── user                 module: accounts, sign-up, login, sessions
│   ├── identity         users, email identifiers, password hashing and policy
│   ├── session          login, refresh, logout, JWTs, cookies
│   ├── signup           3-step sign-up and the setup token
│   ├── password         forgot and reset password
│   ├── token            OTP and reset-link tokens, throttling
│   └── api              what other modules may use (named interface "api")
├── syndicate            module: syndicates and their staff
│   ├── management       create, read and update syndicates
│   ├── staff            staff, owner and invites
│   └── access           @PreAuthorize rules (admin, owner, staff)
├── country              reference data, one package
├── specialty            reference data, one package
├── condition            reference data, one package
├── security             filter chains, rate limiter, @AdminOnly
└── common               shared infrastructure, no feature logic
    └── config, enums, exception, mail, util, web
```

Only `user` and `syndicate` are modules (`spring.modulith.detection-strategy=explicitly-annotated`, and each has a `package-info.java` with `@ApplicationModule`). `country`, `specialty`, `condition`, `security` and `common` are plain packages.

## 3. Who may depend on whom

```mermaid
graph LR
    S[syndicate module] -->|"user :: api only"| U[user module]
    S -->|CountryLookup| C[country]
    S -->|SpecialtyLookup| P[specialty]
    SEC[security] -.->|claim names, cookie name| U
```

- `syndicate` declares `allowedDependencies = "user :: api"`. It can use `AccountService`, `InviteService`, `UserDirectory` and `UserSummary`, and nothing else in `user`.
- `user` never depends on `syndicate`.
- Inside `syndicate`, `management` may use `staff` (through `SyndicateOwners` and `SyndicateStaffService`), but `staff` does not depend on `management`. `access` depends on neither.
- `common` depends on no feature.
- Reference data is exposed to other packages through a small `*Lookup` class (`CountryLookup.existsById`) instead of its repository or entity.

Inside `user`, the sub-packages form a one-way chain:

```mermaid
graph TD
    signup --> session
    signup --> identity
    signup --> token
    password --> session
    password --> identity
    password --> token
    session --> identity
    api --> identity
    api --> token
```

`token` and `identity` depend on no sibling. `session` exposes `SessionService.issue` and `revokeAllForUser` so that sign-up and password reset can start and end sessions without touching `SessionRepository`.

## 4. What is public, and why

| Kind of class                                                                                                                 | Visibility                                      |
|-------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| Services, repositories, assemblers, config, filters                                                                           | Package-private, constructors too               |
| Controllers and `*Dtos` holders                                                                                               | Public classes, package-private handler methods |
| JPA entities (`Country`, `Specialty`, `Condition`)                                                                            | Package-private classes, public accessors       |
| Types another package uses (`SessionService`, `UserRepository`, `VerificationTokens`, `PasswordHasher`, `CountryLookup`, ...) | Public, with only the methods that are used     |
| `user.api`                                                                                                                    | Public: it is the module's contract             |

Methods that stay `public` even though nothing outside their package calls them:

- `SyndicateAccess.isStaffOf` and `isOwnerOf`: `@PreAuthorize` calls them by name through SpEL.
- Handler methods carrying `@AdminOnly`, `@AdminOrSyndicateOwner` or `@AdminOrSyndicateStaff`: they are method-security proxied.
- `SyndicateStatus.code()`: it carries `@JsonValue`.

## 5. Conventions inside a feature

| Name                 | Role                                                                                                      |
|----------------------|-----------------------------------------------------------------------------------------------------------|
| `XController`        | HTTP only: parse, call the service, build the response                                                    |
| `XService`           | Business rules and transactions                                                                           |
| `XRepository`        | Data access. `JdbcClient` SQL in `user` and `syndicate`, Spring Data JPA in the reference-data packages   |
| `XDtos`              | One holder class with nested request and response records                                                 |
| `XLookup`, `api`     | The read-only or narrow surface a feature offers to others                                                |
| `XAssembler`, `XRow` | Turn repository rows into responses                                                                       |

A feature that needs something from another feature depends on a narrow type that the owner chose to expose, such as `SyndicateOwners` ("what management may ask of staff"), not on the owner's repository.

## 6. Tests

Tests mirror the main packages, so each test sits in the same package as the class it covers and can see its package-private members.

- Shared helpers live in `support` (`Fixtures`).
- When a test needs a package-private type from another package, a public test helper sits next to that type: `TestPasswordHasher` (next to `PasswordConfig`) and `Jwts` (next to `JwtConfig`).
- A test of a feature mocks the public collaborators of other features. For example, `SignupServiceTest` mocks `SessionService`, and `SessionServiceTest` covers real session issuing.

## 7. Adding a feature

1. Create a package under `com.nemouw` and keep everything package-private.
2. Make public only what another package needs, ideally as a `*Lookup` or an `api` package.
3. If it is large or has its own internals, add a `package-info.java` with `@ApplicationModule` and list its `allowedDependencies`.
4. Add its routes and roles to `SecurityConfig`. Routes without a rule are denied.
5. Add a Flyway migration for its tables.
6. Put its tests in the same package.