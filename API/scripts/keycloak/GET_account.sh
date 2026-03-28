#!/usr/bin/env bash

#set -o xtrace

JSON=$(curl -X POST \
-d "client_id=et-user-kc-client" \
-d "client_secret=secret" \
-d "username=myself" \
-d "password=secret" \
-d "grant_type=password" \
http://127.0.0.1:8001/realms/etrealm/protocol/openid-connect/token)

JWT=$(echo "$JSON" | grep -o '"access_token":"[^"]*"' | awk -F'"' '{print $4}')

RESPONSE_CODE=$(curl -v -H "Authorization: Bearer $JWT" \
-H "Accept: application/json" \
--write-out "%{http_code}" --output .api.output \
http://127.0.0.1:8001/realms/etrealm/account)

echo $RESPONSE_CODE
cat .api.output