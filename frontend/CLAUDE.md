# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This file supplements the root `CLAUDE.md`. Read that first — this only covers what differs or needs clarification for this module specifically.

## What this module does

Server-side Thymeleaf/Bootstrap 5.3.8 UI. Runs on port 8080 (`frontend`). It is the only module that end-users interact with directly; all other services are internal microservices.

## Security: OAuth2 client, not resource server

Unlike every other module (which are stateless resource servers validating JWTs on each request), this module is a **stateful OAuth2 client**. It uses `oauth2Login` (OIDC authorization code flow) and maintains a server-side session. Roles are extracted from the Keycloak OIDC `realm_access` claim via `userAuthoritiesMapper()` in `FrontendKeycloakSecurityConfiguration`.

Access rules:
- `/manage/**` — requires `role_manager`
- `/user/signup.html`, `/home`, `/`, static assets — public
- Everything else — requires authentication (redirects to Keycloak login)

CSRF is only disabled for the actuator endpoint; it remains active everywhere else (unlike the resource server modules where it is fully disabled).

## `tt` bean — navigation helper for templates

`TemplateToolBoxImplementation` is registered as Spring bean `"tt"`. Use it in Thymeleaf to resolve navigation URLs and highlight active nav items:

```html
<!-- Resolve a property key to its URL path -->
th:href="${@tt.href('seo.home.file')}"

<!-- Mark nav item active if current path matches -->
th:classappend="${@tt.isActive(link) ? 'active' : ''}"

<!-- Get current full URI (used for redirect-back links) -->
th:with="signInHref=${@tt.href('seo.signIn')}, currentPath=${@tt.path}"
th:href="@{${signInHref}(redirect=${currentPath})}">
```

`tt.href(key)` reads from Spring `Environment` — it resolves `seo.*` property keys defined in `API/application.yml`. Never hardcode URL paths in templates; always go through `@tt.href(...)`.

## Dynamic routing

Both `RootController` and `ManagementController` use a catch-all `/{viewPath}` pattern:

- `GET /{viewPath}` → resolves to `templates/Root/{viewPath}.html` (404 if file doesn't exist)
- `GET /manage/{viewPath}` → resolves to `templates/management/{viewPath}.html` (no existence check — Thymeleaf will throw if missing)

Adding a new page under `Root/` or `management/` requires only creating the template file; no controller change is needed.

## Annotation-driven form generation

Model classes annotated with `@HtmlForm` (class-level) and `@HtmlInput` (field-level) from `library` can be turned into a `Form` object via `FormRepositoryImpl.getForm(Forma)`. The repository uses reflection to read annotation metadata and current field values. The resulting `Form` is passed to the `layout/form.html` fragment for rendering.

To add a new form:
1. Annotate a model class (must implement `Forma`) with `@HtmlForm` and its fields with `@HtmlInput`
2. In the controller: `model.addAttribute("form", formRepository.getForm(myObject))`
3. In the template: include the `layout/form.html` fragment

## SEO properties

All public-facing URL paths live under `seo:` in `API/application.yml`. Controller `@RequestMapping` values, security matchers, and template hrefs all reference these properties — never literal strings. When adding a new page, define its path under `seo:` first.

## Work in progress

Most of `ManagementController` is commented out (REST calls to backend services not yet wired). `UserController.findUser` and `signUpPost` are similarly stubbed. The only active management handler is the generic `getView(/{viewPath})` catch-all. `user/test.html` is a temporary debug view and should eventually be removed.
