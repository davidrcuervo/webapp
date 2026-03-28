#!/usr/bin/env bash

METHOD=PUT
#PARAMETERS_REQUIRED: group, name
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: -
#RESPONSE_BODY: group
#AUTHORIZATION: VALID_ACCOUNT & Owner of Group

source ../variables.sh
set -o xtrace

REQUEST=$GROUP_API/addOwner.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" \
b-u $USERNAME:$PASSWORD --basic \
$REQUEST?\
group=testgroup\&\
user=shellapitestuser)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else
  RESULT=$RESPONSE_CODE
fi

#echo $RESULT
exit $RESULT