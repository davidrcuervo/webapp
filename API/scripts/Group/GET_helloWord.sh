#!/usr/bin/env bash

# Author : MySelf.1664
# Create Hello Word
source ../variables.sh
set -o xtrace

echo $SERVER_ADDRESS

curl -i -X GET -H "Content-Type: application/json" \
-u $USERNAME:$PASSWORD \
$GROUP_API/helloword.html?username=shellApiTest

exit 0
