#!/usr/bin/env bash

METHOD=DELETE
#PARAMETERS_REQUIRED: username
#PARAMETERS_OPTIONAL: none
#REQUEST_BODY:
#RESPONSE_BODY: Boolean
#AUTHORIZATION: Self or manager

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/delete.html?username=shellapitestuser

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
-u "shellapitestuser":"shellapipassword1234" --basic \
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