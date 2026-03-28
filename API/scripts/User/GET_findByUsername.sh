#!/usr/bin/env bash

# Author : MySelf.1664

source ../variables.sh
set -o xtrace
RESULT=1

RESPONSE_CODE=$(curl -i --request GET --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
-u $USERNAME:$PASSWORD --basic \
$USER_API/user.html?\
username=shellapitestuser)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else
  RESULT=$RESPONSE_CODE
fi

echo $RESULT
exit $RESULT