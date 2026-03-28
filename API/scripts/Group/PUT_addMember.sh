#!/usr/bin/env bash

METHOD=PUT
#PARAMETERS_REQUIRED: user, group
#PARAMETERS_OPTIONAL: -
#REQUEST_BODY: -
#RESPONSE_BODY: GROUP
#AUTHORIZATION: VALID_ACCOUNT, OWNER OF THE GROUP

source ../variables.sh
set -o xtrace

RESPONSE_CODE=$(curl -i -X $METHOD -H "Content-Type: application/json" \
--write-out "%{http_code}" --output .api.output \
-u $USERNAME:$PASSWORD \
$GROUP_API/addMember.html?\
user=shellapitestuser\&\
group=testgroup)

cat .api.output

if [[ "$RESPONSE_CODE" -eq "200" ]]
then
  RESULT=0
else
  RESULT=$RESPONSE_CODE
fi

#echo $RESULT
exit $RESULT
