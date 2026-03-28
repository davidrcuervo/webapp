#!/usr/bin/env bash

METHOD=PUT
#PARAMETERS_REQUIRED: None
#PARAMETERS_OPTIONAL: None
#REQUEST_BODY: User
#RESPONSE_BODY: User
#AUTHORIZATION: manager or self

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/update.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
-u "shellapitestuser":"passrecovery" --basic \
--data '{
"username":"shellapitestuser",
"firstname":"Linux",
"lastname":"api",
"middlename":"Towards",
"email":"shellapitestuser@address.com"
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