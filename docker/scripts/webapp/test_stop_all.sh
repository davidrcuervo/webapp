#!/usr/bin/env bash

set -euo pipefail

PID_DIR="${PID_DIR:-$(echo $HOME)/.pids}"

#VARIABLES
# $1: Name of module. For example: userKc, schema, messenger, company
# $2: Port of module. For example: 8081, 8082
function stop(){
  echo "Stopping module: $1"
  if curl --fail "http://localhost:$2/actuator/health" > /dev/null; then
    curl -X POST "http://localhost:$2/actuator/shutdown"
  fi

  PID=$(cat $PID_DIR/$1.pid || true)
  echo "PID= $PID"

  if [ -n "$PID" ]; then
    until ! kill -0 "$PID" 2>/dev/null; do
      sleep 1
    done
    rm -f "$PID_DIR/$1.pid"
  fi

  echo "Module stopped!"
}

stop "company" "$PORT_COMPANY"
stop "messenger" "$PORT_MESSENGER"
stop "schema" "$PORT_SCHEMA"
stop "userKc" "$PORT_USUARIO"

echo "All modules have stopped correctly!!!"
exit 0