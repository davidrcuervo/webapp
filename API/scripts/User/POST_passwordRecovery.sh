#!/usr/bin/env bash

METHOD=POST
#PARAMETERS_REQUIRED: token
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: password, password2
#RESPONSE_BODY: USER
#AUTHORIZATION: ANY

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/passwordrecovery.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" \
--write-out "%{http_code}" --output .api.output \
--data '{
"password":"passrecovery",
"password2":"passrecovery"
}' \
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