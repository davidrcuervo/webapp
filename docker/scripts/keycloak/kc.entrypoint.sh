#!/bin/bash

if [ -z "$KC_BOOTSTRAP_ADMIN_USERNAME" ]; then
  echo "Variable, KC_BOOTSTRAP_ADMIN_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$KC_BOOTSTRAP_ADMIN_ENC_PASSWORD" ]; then
  echo "Variable, KC_BOOTSTRAP_ADMIN_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_TYPE" ]; then
  echo "Variable, SCHEMA_TYPE, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_URL" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_URL, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_USERNAME" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$SCHEMA_KEYCLOAK_ENC_PASSWORD" ]; then
  echo "Variable, SCHEMA_KEYCLOAK_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$PORT_KEYCLOAK_MANAGEMENT" ]; then
  echo "Variable, PORT_KEYCLOAK_MANAGEMENT, is unset" >&2
  exit 1
fi

export KC_BOOTSTRAP_ADMIN_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$KC_BOOTSTRAP_ADMIN_ENC_PASSWORD")

SCHEMA_KEYCLOAK_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$SCHEMA_KEYCLOAK_ENC_PASSWORD")

echo "keycloak username: $KC_BOOTSTRAP_ADMIN_USERNAME"
echo "keycloak database type: $SCHEMA_TYPE"
echo "keycloak database url: $SCHEMA_KEYCLOAK_URL"
echo "keycloak database username: $SCHEMA_KEYCLOAK_USERNAME"

#START KEYCLOAK
$HOME/keycloak-26.8.2/bin/kc.sh start \
--db=$SCHEMA_TYPE \
--db-url=$SCHEMA_KEYCLOAK_URL \
--db-username=$SCHEMA_KEYCLOAK_USERNAME \
--db-password=$SCHEMA_KEYCLOAK_PASSWORD \
--health-enabled=true \
--metrics-enabled=true \
--http-management-port $PORT_KEYCLOAK_MANAGEMENT \
--hostname-strict false \
--https-certificate-file=/opt/docker/certs/kc.crt \
--https-certificate-key-file=/opt/docker/certs/kc.unsecure.key \
& PID_KC=$!
#--http-enabled true \
#--proxy-headers=forwarded \
#--http-port=$PORT_KEYCLOAK
#--hostname=auth.webapp.com \
#--hostname-backchannel-dynamic true \

#Wait keycloak to start
echo "Waiting keycloak to become ready..."
until curl --fail --insecure --silent "https://localhost:$PORT_KEYCLOAK_MANAGEMENT/health/ready" > /dev/null; do
  if ! kill -0 "$PID_KC" 2>/dev/null; then
    echo "keycloak exited before becoming ready" >&2
    wait "$PID_KC"; exit 1
  fi
  sleep 5
done
echo "Keycloak up and ready!"

#Run configuration script.
echo "Running realm configuration..."
KC_CONFIG_SERVER="https://localhost:$PORT_KEYCLOAK" /opt/keycloak/kc.config.sh & wait "$PID_KC"