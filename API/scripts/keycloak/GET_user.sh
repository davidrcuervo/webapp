#!/usr/bin/env bash

#set -0 xtrace
DIR=$(dirname $(realpath "$0"))
echo "$DIR"

JWT=$($DIR/GET_userClientToken.sh)

RESPONSE_CODE=$(curl --insecure -H "Authorization: Bearer $JWT" \
-H "Accept: application/json" \
--write-out "%{http_code}" --output .api.output \
https://auth.webapp.com/admin/realms/etrealm/users?username=samsepi0l)
#https://auth.webapp.com/realms/etrealm/account)

echo $RESPONSE_CODE
cat .api.output
