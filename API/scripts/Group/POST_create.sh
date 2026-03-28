#!/usr/bin/env bash

# Author : MySelf.1664

source ../variables.sh
set -o xtrace

curl -i -X POST -H "Content-Type: application/json" \
-d '{
    "name":"testgroup",
    "description":"This group test the create api"
}' \
-u $USERNAME:$PASSWORD \
$GROUP_API/create.html

exit 0
