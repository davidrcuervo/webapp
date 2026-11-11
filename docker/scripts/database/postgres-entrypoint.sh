#!/bin/bash

if [ -z "$SCHEMA_DATASOURCE_DB_NAME" ]; then
  echo "Variable, SCHEMA_DATASOURCE_DB_NAME, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_DATASOURCE_ENC_PASSWORD" ]; then
  echo "Variable, SCHEMA_DATASOURCE_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_ENC_PASSWORD" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_DATASOURCE_USERNAME" ]; then
  echo "Variable, SCHEMA_DATASOURCE_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_USERNAME" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_DB_NAME" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_DB_NAME, is unset" >&2
  exit 1
fi

if [ ! -f /opt/mypostgres/scripts/pg_hba.conf ]; then
  echo "File, /opt/mypostgres/scripts/pg_hba.conf, is missing";
  exit 1
fi

SCHEMA_DATASOURCE_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$SCHEMA_DATASOURCE_ENC_PASSWORD")
SCHEMA_KEYCLOAK_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$SCHEMA_KEYCLOAK_ENC_PASSWORD")
LOG_PATH=/opt/mypostgres/logs/postgresql.$(date +%F.%Hh%Mm%Ss).log
DATA_PATH=/opt/mypostgres/data
HBA_CONF_PATH=/opt/mypostgres/scripts/pg_hba.conf

# Before PostgresSQL can function correctly, the database cluster must be initialized:
/usr/lib/postgresql/18/bin/initdb -D $DATA_PATH

# internal start of server in order to allow set-up using psql-client
# does not listen on external TCP/IP and waits until start finishes
/usr/lib/postgresql/18/bin/pg_ctl -D $DATA_PATH -l "$LOG_PATH" -o "-c listen_addresses='*'" -o "-c hba_file='$HBA_CONF_PATH'" -w start

# create a user or role
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "CREATE USER $SCHEMA_DATASOURCE_USERNAME WITH PASSWORD '$SCHEMA_DATASOURCE_PASSWORD';"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE $SCHEMA_DATASOURCE_DB_NAME TEMPLATE template0 ENCODING 'UNICODE';"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "ALTER DATABASE $SCHEMA_DATASOURCE_DB_NAME OWNER TO $SCHEMA_DATASOURCE_USERNAME;"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE $SCHEMA_DATASOURCE_DB_NAME TO $SCHEMA_DATASOURCE_USERNAME;"

#set springsession database
#/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "CREATE USER springsession WITH PASSWORD '$DATABASE_SPRINGSESSION_PASSWORD';"
#/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -f $DATABASE_SPRINGSESSION_CREATE_DB
#/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -d springsession -U springsession -f $DATABASE_SPRINGSESSION_CREATE_TABLES

#set keycloak database
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "CREATE USER $SCHEMA_KEYCLOAK_USERNAME WITH PASSWORD '$SCHEMA_KEYCLOAK_PASSWORD';"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE $SCHEMA_KEYCLOAK_DB_NAME TEMPLATE template0 ENCODING 'UNICODE';"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "ALTER DATABASE $SCHEMA_KEYCLOAK_DB_NAME OWNER TO $SCHEMA_KEYCLOAK_USERNAME;"
/usr/lib/postgresql/18/bin/psql -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE $SCHEMA_KEYCLOAK_DB_NAME TO $SCHEMA_KEYCLOAK_USERNAME;"

function close(){
  echo "Stopping Postgres SQL...."
  /usr/lib/postgresql/18/bin/pg_ctl -D "$DATA_PATH" -m fast -w stop
  echo "Closed, good bye!!"
  exit 0
}

# stop internal postgres server
trap close SIGTERM SIGINT SIGQUIT SIGHUP
tail -f "$LOG_PATH" &
CHILD_PID=$!
wait "$CHILD_PID"