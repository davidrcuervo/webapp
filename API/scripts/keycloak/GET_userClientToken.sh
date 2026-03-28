#!/usr/bin/env bash

DIR=$(dirname $(dirname $(dirname $(realpath "$0"))))
PASSWD=$(cat $DIR/docker/private/jasypt-password.txt)

ENCRYPTED_VALUE=$(cat $DIR/docker/.env | grep KC_USER_CLIENT_ENC_PASSWORD)
VALUE="${ENCRYPTED_VALUE#KC_USER_CLIENT_ENC_PASSWORD=ENC(}"
VALUE="${VALUE%)}"

CLIENT_SECRET=$(/opt/jasypt/jasypt-1.9.3/bin/decrypt.sh \
algorithm="PBEWITHHMACSHA512ANDAES_256" \
saltGeneratorClassName="org.jasypt.salt.RandomSaltGenerator" \
ivGeneratorClassName="org.jasypt.iv.RandomIvGenerator" \
stringOutputType="base64" verbose=false \
password="$PASSWD" input="$VALUE")

JSON=$(curl -s -X POST --insecure \
-d "client_id=et-user-kc-client" \
-d "client_secret=$CLIENT_SECRET" \
-d "grant_type=client_credentials" \
https://auth.webapp.com/realms/etrealm/protocol/openid-connect/token)

JWT=$(echo "$JSON" | grep -o '"access_token":"[^"]*"' | awk -F'"' '{print $4}')
echo "$JWT"
