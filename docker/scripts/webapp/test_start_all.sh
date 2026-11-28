#!/usr/bin/env bash

set -euo pipefail

# Directory where background module PIDs are recorded, so the stop
## script (a separate process) can find and terminate them later.
PID_DIR="${PID_DIR:-$(echo $HOME)/.pids}"
mkdir -p "$PID_DIR"

function compile() {
  echo "Testing and Compiling module: $1"
  mvn clean install -f "$1/pom.xml" \
  -Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
  -Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
  -Daether.installer.hardLink=false
}

#VARIABLES
# $1: Name of module. For example: userKc, schema, messenger, company
# $2: Port of module. For example: 8081, 8082
function run(){
  echo "Running module: $1"
  java -ea -Djasypt.encryptor.password=$(cat /run/secrets/jasypt-password) \
  -Dspring.config.additional-location=$(pwd)/API/,$(pwd)/ \
  -jar "/home/claude/.m2/repository/com/laetienda/$1/1.0-SNAPSHOT/$1-1.0-SNAPSHOT.jar" & \
  PID_THIS=$!

  # Record the PID so test_stop_all.sh can kill it later.
  echo "$PID_THIS" > "$PID_DIR/$1.pid"

  echo "Waiting module to become ready..."
  until curl --fail --silent "http://localhost:$2/actuator/health" > /dev/null; do
    if ! kill -0 "$PID_THIS" 2>/dev/null; then
      echo "module $1 exited before becoming ready" >&2
      rm -f "$PID_DIR/$1.pid"
      exit 1
    fi
    sleep 5
  done
  echo "Module is up and ready!"
}

#Compile libraries
mvn clean install -Daether.installer.hardLink=false -f library/pom.xml
mvn clean install -Daether.installer.hardLink=false -f model/pom.xml
mvn clean install -Daether.installer.hardLink=false -f utils/pom.xml

#Start user module
compile "userKc"
run "userKc" "$PORT_USUARIO"

#start schema module
compile "schema"
run "schema" "$PORT_SCHEMA"

#start messenger module
compile "messenger"
run "messenger" "$PORT_MESSENGER"

#start company module
compile "company"
run "company" "$PORT_COMPANY"

echo "All modules started successfully!!!"
exit 0