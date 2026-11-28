# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**La eTienda** — a multi-module Spring Boot 3.5.12 / Java 25 e-commerce/business platform. It consists of independent microservices fronted by Nginx, secured via Keycloak OAuth2/JWT, with all sensitive config values encrypted using Jasypt.

Project can be deployed in **production** or **development**. Commands and script to deploy are similar but not always the same. Production environment runs completely in docker containers while development environment may run in IntelliJ or bash.

**Containers:**
- Postgres: Required for *production* and *development*. For both scenarios, docker container should de up and running.
- Keycloak: Required for *production* and *development*. For both scenarios, docker container should de up and running.
- Nginx: Required for *production* and *development*. In development is required to expose keycloak end points. In production, it will expose both, keycloak and frontend endpoints. In development, fronted end points are accessed directly through `http` and port 8080.
- Java microservices containers. For *production,* they should be deployed as docker containers, for *development* they may run through bash commands. The java modules are:
  - User
  - Schema
  - Messenger
  - Company
  - Frontend.

## Build

Building and compiling it is done when docker images are built. For details review @Dockerfile and @docker-compose.yml

**Build required containers - production & development**
```
docker compose build postgres
docker compose build keycloak
docker compose build nginx
```

**Build java modules - production**
```
docker compose build user
docker compose build schema
docker compose build messenger
docker compose build company
docker compsoe build frontend
```

**Build java modules - development**
```bash
#replace the name of module <module_name>. For example: "userKc", "schema", "messenger", "company", "frontend"
#Notice it will test the code, if testing fails, jar file will not be created.
#Testing required postgres, keycloak and nginx to be running
mvn clean install -f <module_name>/pom.xml \
-Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
-Daether.installer.hardLink=false
```

## Running

**Run required container - production & development**
```bash
docker compose up -d postgres keycloak nginx
```

**Run java modules - production**
```bash
docker compose up -d user schema messenger company frontend
```

**Run java modules - development**
```bash
java -ea -Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
-jar /home/claude/.m2/repository/com/laetienda/<module_name>/1.0-SNAPSHOT/<module_name>-1.0-SNAPSHOT.jar
```

**Run all java modules script - development**
```bash
./docker/scripts/webapp/test_start_all.sh
```

**Stop all java modules script - development**
```bash
./docker/scripts/webap/test_stop_all.sh
```

## Testing

**Postgres Database - production & development**
```bash
# Test connection to webapp database
PG_URL="${SCHEMA_DATASOURCE_URL#jdbc:}"
psql -U "$SCHEMA_DATASOURCE_USERNAME" "$PG_URL"
```

```bash
#Test connection to keycloak database.
PG_URL="${SCHEMA_KEYCLOAK_URL#jdbc:}"
psql -U "$SCHEMA_KEYCLOAK_USERNAME" "$PG_URL"
```

**Java modules - Production** Replace with name of module
```bash
#replace the name of module. For example: "userKc", "schema", "messenger", "company", "frontend"
#run the script from the docker container.
./docker/scripts/webapp/test.sh <module_name>
```

**Java modules - development**
```bash
# All tests in a module
mvn test <module_name>/pom.xml \
-Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
-Dspring.config.additional-location=$(pwd)/API/,$(pwd)/
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
