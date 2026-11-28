---
name: troubleshoot-test
description: >
  Diagnose a failing, erroring, or hanging test or endpoint in the La eTienda
  webapp (any module: userKc, schema, messenger, company, frontend). Use whenever
  the user points at a test, class, method, module, or HTTP endpoint and wants the
  cause found — especially when they would otherwise paste the error. Claude RUNS
  the test and reads the real stack trace itself instead of asking for the message,
  because ExceptionController trims exceptions down to a status code + message.
---

# Troubleshoot a failing test or endpoint

The user usually knows *which* test fails but not *why*. Your job is to run it,
read the full error yourself, and report the root cause + smallest fix. Do **not**
ask the user to paste the error — the message they can see is already trimmed.

Commands below are the **development** forms (run from the repo root via Maven).
The Jasypt password is read from the secret file; config comes from `API/` + repo
root passed as absolute paths.

## 1. Run it yourself (pick the narrowest scope)

- Single method:
  `mvn test -f <module>/pom.xml -Dtest=<Class>#<method> -Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) -Dspring.config.additional-location=$(pwd)/API/,$(pwd)/`
- Single class: same without `#<method>`.
- All tests in a module: same without `-Dtest=...`.
- Whole module via the container/JUnit console: `./docker/scripts/webapp/test.sh <module>`
- Bring the full stack up + run everything: `./docker/scripts/webapp/test_start_all.sh`
  (stop with `./docker/scripts/webapp/test_stop_all.sh`)
- Compile → test → start pipeline: `./docker/scripts/webapp/safestart.sh`
- A `mvn clean install` build (shared libs or when a jar is needed) also needs
  `-Daether.installer.hardLink=false` (project tree and local repo are on different filesystems).

If a command needs approval and you can't proceed, ask the user to run it with a
leading `!` so the output lands in the session.

## 2. Check preconditions BEFORE deep digging

Many "mystery" failures are just a missing dependency:
- `docker compose ps` — are the needed services healthy?
- **postgres + keycloak + nginx** must be up for any test (dev uses nginx to expose
  the Keycloak endpoints; without it, token/login calls fail).
- Tests that call `getTestUsers` / real tokens also need **userKc** up.
- Token-forwarding tests also need the **downstream** service (schema / company).
- Only `frontend` is an OAuth2 *login* client; the others are stateless resource servers.

## 3. Surface the REAL error

- `frontend`'s `ExceptionController` collapses exceptions into a status + body
  (`ex.getMessage()`), so MockMvc only shows "not 200". Add
  `.andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())`
  to the failing `mvc.perform(...)` to see status + body.
- For the full stack trace, read the service logs (container logs, or the
  `$PID_DIR` logs started by `test_start_all.sh`).
- Re-run the single method after adding `print()` — don't run the whole suite.

## 4. Known traps (check these first)

- **302 instead of 404/401** → `frontend` uses `oauth2Login` (session), not a
  resource server. A `Bearer` header is ignored; unauthenticated → redirect to
  Keycloak. Use `.with(oidcLogin())`; to forward a real token downstream, seed the
  user's token into `OAuth2AuthorizedClientService` and pin `oidcLogin()` to the
  `keycloak` registration with the user id as subject (see `FrontendTests.loginAs`).
- **403 on a POST** → CSRF is enabled everywhere except actuator. MockMvc form posts
  need `.with(csrf())` (from `SecurityMockMvcRequestPostProcessors`).
- **POST expected a 3xx redirect but got 200 (form re-rendered)** → a submitted param
  failed binding/validation (bad enum constant, `@Pattern`/`@Size`/`@NotNull` miss), so
  `BindingResult` has errors and the controller re-renders the form instead of redirecting.
  It's silent — no exception — so it looks like a "wrong status," not a cause. Check the
  param values against the model's constraints (e.g. `memberPolicy` must be an exact
  `CompanyMemberPolicy` constant: `PUBLIC`/`REGISTRATION_REQUIRED`/`AUTHORIZATION_REQUIRED`);
  `model().attributeHasFieldErrors("company", ...)` pinpoints which field.
- **"Instantiation of new objects and access to static classes or parameters is
  forbidden in this context"** → a `@bean` call (`@tt`, `@usr`, `@comp`, `@utils`,
  or `new`/`T(...)`/`param`) in a **restricted** Thymeleaf context. The restricted
  contexts are `th:href` / `th:src` / `th:action` **and any `~{...}` fragment
  expression** (the `th:replace`/`th:insert` fragment name *and its arguments* — even
  when the fragment only drops the value into a `th:text`). Fix: resolve it in a
  `th:with` (or a model attribute) and reference the plain variable. `th:with` must
  be on a **wrapping** element, not the same tag as `th:replace` (replace, prec 100,
  runs before with, prec 400; but `th:action` at prec 1000 runs after with, so
  `th:with`+`th:action` on one tag is fine).
- **"Could not parse as expression: x=${...}"** → a `th:*` typo (e.g. `th:width`
  instead of `th:with`); the value is parsed as an expression and the `=` breaks it.
- **Schema looks stale/wrong** → the `schema` module uses `ddl-auto: create-drop`;
  tables are dropped and recreated on every startup.
- **Repositories** use `EntityManager` directly (custom `XxxRepository` +
  `XxxRepositoryImplementation`), not Spring Data JPA — don't look for Spring Data.

## 5. Stubs that fail on purpose

Methods left as `fail()` are unimplemented reminders. They should be `@Disabled("TODO: ...")`
(skipped, not failed) or `@Tag("wip")` + excluded (`--exclude-tag wip` /
`-DexcludedGroups=wip`). If one is failing the run, flag it rather than "fixing" it.

## 6. Report

Give: the failing `file:line`, the root-cause line from the trace, why it only
fails in that scenario, and the smallest fix. Never just restate the status code.
</content>
