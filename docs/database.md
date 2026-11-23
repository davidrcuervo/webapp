# Database helper code snippets

## Check if database container is up and responding.

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