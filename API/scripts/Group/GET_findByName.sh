#!/usr/bin/env bash

# Author : MySelf.1664

source ../variables.sh
set -o xtrace

curl -i -X GET -H "Content-Type: application/json" \
-u $USERNAME:$PASSWORD \
$GROUP_API/group.html?\
name=testgroup

exit 0