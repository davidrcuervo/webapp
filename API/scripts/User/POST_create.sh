#!/usr/bin/env bash

#METHOD: POST
#PARAMETERS_REQUIRED: -
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: USER
#RESPONSE_BODY: USER
#AUTHORIZATION: ANY

source ../variables.sh
set -o xtrace

REQUEST=$USER_API/create.html

RESPONSE_CODE=$(curl -i --request POST --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
--data '{
"username":"shellapitestuser",
"firstname":"Linux",
"lastname":"api",
"middlename":"Shell",
"email":"shellapitestuser@mail.com",
"password":"shellapipassword1234",
"password2":"shellapipassword1234"
}' \
$REQUEST)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else if [[ "$RESPONSE_CODE" -eq "000" ]]
then
  RESULT=-1
else
  RESULT=$RESPONSE_CODE
fi

#echo $RESULT
exit $RESULT