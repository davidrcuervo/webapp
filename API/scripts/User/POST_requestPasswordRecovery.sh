#!/usr/bin/env bash

METHOD=POST
#PARAMETERS_REQUIRED: -
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: username
#RESPONSE_BODY: STRING(token)
#AUTHORIZATION: ANY

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/requestpasswordrecovery.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" \
--write-out "%{http_code}" --output .api.output \
--data '{
"username":"shellapitestuser"
}' \
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