#!/usr/bin/env bash

METHOD=GET
#PARAMETERS_REQUIRED: None
#PARAMETERS_OPTIONAL:
#REQUEST_BODY: None
#RESPONSE_BODY: UserList
#AUTHORIZATION: manager

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/users.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
-u $USERNAME:$PASSWORD --basic \
$REQUEST)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else
  RESULT=$RESPONSE_CODE
fi

#echo $RESULT
exit $RESULT