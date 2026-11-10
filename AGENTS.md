# Repository Guidelines

Halo is a full-stack monorepo: Java 17 / Spring Boot WebFlux + R2DBC backend and a Vue 3 + TypeScript + TailwindCSS frontend. Work from the repo root and load **every relevant module guide** for the areas you touch.

> This fork builds with **Maven** (the original upstream uses Gradle). Java target is 17 here.

## Modules

|       Module        |                           Path                            |
|---------------------|-----------------------------------------------------------|
| API library         | `api/` (extension model, contracts, security)             |
| Backend application | `application/` (services, routers, migrations, packaging) |
| Frontend            | `ui/` (console, user center, workspace packages)          |

## Quick Commands

Run from the repo root:

- `mvn package -DskipTests` or `build-jar.bat` — package the executable JAR.
- `build-war.bat` — package the executable WAR (temporarily switches packaging to war, then restores the pom).
- `mvn test -pl api` / `mvn test -pl application` — backend tests.
- `pnpm -C ui typecheck && pnpm -C ui lint` — frontend validation.
- `pnpm -C ui test:unit` — frontend unit tests.

## Run Without Docker (local)

1. Start MariaDB: `D:\mariadb\start.bat` (port 3306, root password `mariadb`).
2. Start Halo:
   `java -Djava.io.tmpdir=H:/halo2-maven-test/tmp -jar application/target/halo-2.28.0-SNAPSHOT.jar --spring.profiles.active=mariadb --halo.work-dir=H:/halo2-maven-test --server.port=8090`
3. Open `http://localhost:8090`.

## Cross-Module Rules

- Keep API contracts, backend handlers, and UI usage in sync; never hand-edit `ui/packages/api-client/src/` — regenerate it after contract changes.
- Versions live in the root `pom.xml` (`<properties>` + `<dependencyManagement>`) or `ui/package.json`; never hard-code them.
- Branch naming: `feat/`, `fix/`, `improvement/`, `upgrade/`.
- Stage specific files only; never `git add -A` (H2 artifacts and build outputs appear under `application/`).
- Add tests for changed behavior; keep tests green.
- Never commit secrets or introduce blocking I/O into reactive flows.
