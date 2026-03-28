#!/usr/bin/env bash

METHOD=
#PARAMETERS_REQUIRED:
#PARAMETERS_OPTIONAL:
#REQUEST_BODY:
#RESPONSE_BODY:
#AUTHORIZATION:

source ../variables.sh
set -o xtrace

REQUEST=$GROUP_API/path.html

RESPONSE_CODE=$(curl -i --request $METHOD --header "Content-Type: application/json" -m $MAX_TIME \
--write-out "%{http_code}" --output .api.output \
#-u $USERNAME:$PASSWORD --basic \
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