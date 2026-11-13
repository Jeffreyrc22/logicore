# LogiCore

Workspace for logistics operations, private documents and inventory. Angular 21 and TypeScript frontend; Java 21, Spring Boot, Spring Security, Flyway and PostgreSQL backend.

## Run the existing local environment

```powershell
./scripts/Stop-Local.ps1
./scripts/Build-Local.ps1
./scripts/Start-Local.ps1
./scripts/Test-Local.ps1
./scripts/Stop-Local.ps1
```

The UI runs at http://127.0.0.1:4200 and the API at http://127.0.0.1:8090. Local credentials are in the ignored `.runtime/local-env.json`; never commit or share that file. The current Windows setup shares portable Java/Maven/PostgreSQL binaries with the sibling AURUM Coffee `.runtime` directory. Database roles and databases are separate. Tests use `logicore_test` and port 8091, not the application database.

## Container setup

Copy `.env.example` to `.env`, set unique database and administrator passwords, then run `docker compose up --build`. The web entry point is http://127.0.0.1:8098. Containers use their own persistent PostgreSQL volume. Docker configuration is supplied but has not been run in this environment.

## Features and boundaries

- SUPER_ADMIN manages users, settings and audit; MANAGER manages operational resources; OPERATOR works with assigned operations.
- Server-side permissions, CSRF protection, HttpOnly sessions and password hashing. Disabling users or changing passwords revokes sessions.
- Versioned operations with enforced transitions, history and assignment checks.
- Private documents with file validation, authorized downloads and separate metadata/content storage.
- Inventory movements use database locks; withdrawals cannot make stock negative.
- Dashboard and CSV reports use authorized database records. PDF export uses the browser print dialog.
- Password recovery uses expiring, single-use hashed tokens. SMTP must be configured; the UI explicitly reports when email recovery is unavailable. Actual email delivery has not been tested.

## Source map

- `backend/src/main/java`: domain services, API controllers and authorization.
- `backend/src/main/resources/db/migration`: schema migrations, including recovery tokens and document content separation.
- `frontend/src/app/core`: API and session state.
- `frontend/src/app/features`: dashboard, resources, authentication and reporting screens.
- `frontend/src/app/shared`: shared UI and dialog keyboard focus handling.
- `tests/api.integration.test.mjs`: isolated database integration journeys.
- `scripts`: local build, startup and verification.

## Verification

On 2026-11-13 the isolated API suite passed 18 reported tests: authentication/CSRF, all three roles, contacts, scoped operations, transitions, documents, file validation, inventory concurrency, reporting, audit, settings, session revocation, recovery expiry and token reuse. Maven verification and the Angular production build also completed.

This is a locally verified implementation, not a claim of production certification. Before deployment configure HTTPS, SMTP, backups, secrets and operational monitoring. Request throttling is process-local; aggregate reporting needs pagination/aggregation review for large datasets. Dependency audit findings and browser checks are recorded in `docs/verification.md`.


