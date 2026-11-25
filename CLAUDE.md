# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**La eTienda** — a multi-module Spring Boot 3.5.12 / Java 25 e-commerce/business platform. It consists of independent microservices fronted by Nginx, secured via Keycloak OAuth2/JWT, with all sensitive config values encrypted using Jasypt.

## Build & Run

Building and compiling it is done when docker images are built. For details review @Dockerfile and @docker-compose.yml

```
docker compose build postgres
docker compose build keycloak
docker compose build nginx
docker compose build user
docker compose build schema
docker compose build messenger
docker compose build company
```

All encrypted properties require the Jasypt password, read from environment variable that is passed through docker compose secrets, and it is placed at `/run/secrets/jasypt-password` and passed as:
```
-Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password)
```

**Build (all modules, skip tests):**
```bash
mvn clean install -DskipTests -Pcontainer -Dcontainer.target.dir=<output_dir>
```

**Docker Compose (recommended for development):**
```bash
# Start dependencies first
docker compose up -d postgres keycloak nginx
docker compose up -d                   # Start all remaining services
```

**Run a single service locally (example: company):**
```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Djasypt.encryptor.password=$JASYPT_PASSWORD \
  -Dspring.config.additional-location=./API/,./etc/" \
  -f company/pom.xml
```

## Testing

Tests require live dependencies (Keycloak, PostgreSQL). The Docker `testcontainer` service wires these up:
```bash
./docker/scripts/webapp/test.sh <module_name>
```

**Run tests locally (Maven):**
```bash
# All tests in a module
mvn test -f company/pom.xml \
  -Djasypt.encryptor.password=$JASYPT_PASSWORD \
  -Dspring.config.additional-location=./API/,./

# Single test class
mvn test -f company/pom.xml -Dtest=CompanyTests \
  -Djasypt.encryptor.password=$JASYPT_PASSWORD \
  -Dspring.config.additional-location=./API/,./

# Single test method
mvn test -f company/pom.xml -Dtest=CompanyTests#health \
  -Djasypt.encryptor.password=$JASYPT_PASSWORD \
  -Dspring.config.additional-location=./API/,./
```

**Compile → Test → Start pipeline:**
```bash
./docker/scripts/webapp/safestart.sh
```

## Module Architecture

```
library     ← Spring Boot parent BOM, base security config, KeycloakGrantedAuthoritiesConverter
model       ← JPA entities (Company, Member, Friend, KcUser, Form, Input, ThankyouPage)
utils       ← API client helpers (ApiUser, ApiSchema) shared across services
userKc      ← Keycloak user management service            (port 8081 / etuser)
schema      ← Generic form/schema data service            (port 8083 / etschema)
messenger   ← Email service (SMTP via Zoho)               (port 8082 / etmail)
company     ← Company/Member/Friend management service    (port 8084 / etcompany)
frontend    ← Server-side Thymeleaf/Bootstrap 5 UI        (port 8080 / frontend)
webapp-test ← Integration test module (Java 21)
```

`library` and `utils` are shared libraries, not runnable services. All services depend on them. `schema` and `company` also depend on `userKc` being running.

## Key Architectural Patterns

**Security:** Every service is a stateless OAuth2 Resource Server. JWT tokens issued by Keycloak are validated on each request. `KeycloakGrantedAuthoritiesConverter` (in `library`) maps realm/client roles to Spring `GrantedAuthority`. CSRF is disabled everywhere.

**Controllers:** URL paths are injected from `application.yml` properties (e.g., `@RequestMapping("${api.company.folder}")`), not hardcoded. The canonical API paths live in `API/application.yml`.

**Services:** Interface + Implementation pattern (e.g., `CompanyService` / `CompanyServiceImplementation`). Validation uses Jakarta Bean Validation + custom `NotValidCustomException`.

**Repositories:** Custom repository pattern using `EntityManager` directly — not Spring Data JPA repositories. Each module has `XxxRepository` interface + `XxxRepositoryImplementation`.

**Database schema:** The `schema` module owns DDL with `ddl-auto: create-drop` — it drops and recreates all tables on every startup. Do not rely on persistent schema state during development.

## Spring Configuration

Config is loaded from multiple locations, layered in priority order:
1. `API/application.yml` — API path definitions, shared across all services
2. Module's own `src/main/resources/application.yml` — service-specific settings
3. External `/etc/application.yml` — environment overrides (injected in Docker)

Pass additional locations at startup:
```
-Dspring.config.additional-location=./API/,./etc/
```

## Environment & External Services

All secrets in `.env` use Jasypt `ENC(...)` cipher text. Required external services:

| Service    | Docker name    | Default port |
|------------|---------------|-------------|
| PostgreSQL | postgreset    | 5432        |
| Keycloak   | keycloaket    | 9001 (internal), 8443 (public) |
| Nginx      | etnginx       | 80, 444     |

Required TLS key material must exist under `docker/private/keys/` before starting Nginx or Keycloak (self-signed certs; see README or docker scripts for generation).

**Databases:** `webapp` (app data), `keycloak` (Keycloak realm), `springsession` (optional). SQL init scripts are in `docker/scripts/database/`.

## Useful Scripts

| Script | Purpose |
|--------|---------|
| `docker/scripts/webapp/test_start_all.sh` | compile, test and start all java modules|
| `docker/scripts/webapp/test_stop_all.sh` | stop all java modules |
| `docker/scripts/webapp/compile.sh` | Maven build with `container` profile |
| `docker/scripts/webapp/test.sh` | Run JUnit tests via JUnit Platform Console |
| `docker/scripts/webapp/start.sh` | Launch service from compiled JAR |
| `docker/scripts/webapp/safestart.sh` | Full compile → test → start pipeline |
| `docker/scripts/jasypt/jdecrypt.sh` | Decrypt an `ENC(...)` property value |
| `docker/scripts/keycloak/kc.config.sh` | Configure Keycloak realm (clients, users, roles) |
