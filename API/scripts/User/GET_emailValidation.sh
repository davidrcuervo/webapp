#!/usr/bin/env bash

METHOD=GET
#PARAMETERS_REQUIRED: token
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: -
#RESPONSE_BODY: Boolean
#AUTHORIZATION: AUTHENTICATED

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/emailvalidation.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" \
--write-out "%{http_code}" --output .api.output \
-u shellapitestuser:shellapipassword1234 --basic \
$REQUEST?\
token=$1)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else
  RESULT=$RESPONSE_CODE
fi

#echo $RESULT
exit $RESULT