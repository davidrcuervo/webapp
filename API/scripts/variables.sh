#!/usr/bin/env bash

# Author : MySelf.1664
# Create variables
echo "Loading local variables"

SERVER_ADDRESS="172.24.48.1"
USERNAME="admuser"
PASSWORD="secret"
MAX_TIME=10

USER_API="http://$SERVER_ADDRESS:8081/api/v0/user"
GROUP_API="http://$SERVER_ADDRESS:8081/api/v0/group"
