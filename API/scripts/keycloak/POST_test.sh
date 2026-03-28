#!/usr/bin/env bash

set -o xtrace

KCADM=/opt/keycloak/keycloak-26.1.5/bin/kcadm.sh

if [ -z $KC_USER_CLIENT_ID ];then
  KC_USER_CLIENT_ID=et-user-kc-client
fi

CLIENT_ID=$("$KCADM" get clients -r etrealm -q clientId="$KC_USER_CLIENT_ID" -F id --format csv --noquotes)
echo "$CLIENT_ID"

SERVICE_USER_USERNAME=$("$KCADM" get clients/"$CLIENT_ID"/service-account-user -r etrealm -F username --format csv --noquotes)
echo "$SERVICE_USER_USERNAME"

"$KCADM" add-roles -r etrealm --uusername "$SERVICE_USER_USERNAME" --cclientid realm-management --rolename view-users