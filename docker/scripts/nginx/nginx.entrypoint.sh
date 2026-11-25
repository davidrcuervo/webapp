#!/usr/bin/env bash

function close(){
  echo "Closing nginx..."
  rm /etc/nginx/sites-enabled/keycloak
  rm /etc/nginx/sites-enabled/webapp
  echo "...Bye nginx"
  exit 0
}

function start_webapp(){
  COUNTER=0
  WEBAPP_FLAG=false

  while [ $COUNTER -lt 10 ]
  do
    if nc -z frontend "$PORT_FRONTEND"; then
      echo "Starting webapp"
      ln -s /etc/nginx/sites-available/webapp.nginx /etc/nginx/sites-enabled/webapp
      nginx -s reload
      WEBAPP_FLAG=true
      break
    else
      sleep 2
      COUNTER=$((COUNTER+1))
      echo "waiting webapp to start..."
    fi
  done

  if [ "$WEBAPP_FLAG" = false ]; then
    echo "Nginx failed to start webapp frontend."
  fi
}

function run(){
  export DOLLAR="$"

  echo "Setting nginx site configuration file"
  envsubst < keycloak.template.nginx > /etc/nginx/sites-available/keycloak.nginx
  envsubst < webapp.template.nginx > /etc/nginx/sites-available/webapp.nginx
  ln -s /etc/nginx/sites-available/keycloak.nginx /etc/nginx/sites-enabled/keycloak
#  ln -s /etc/nginx/sites-available/webapp.nginx /etc/nginx/sites-enabled/webapp

  trap close SIGTERM SIGINT SIGQUIT SIGHUP
  echo "Starting nginx service"
  nginx -g "daemon off;" &
  NGINX_PID=$!
  start_webapp
  wait "$NGINX_PID"
}

if [ ! -f keycloak.template.nginx ];then
  echo "ERROR, file keycloak.template.nginx does not exitst" >&2
  exit 1
fi

if [ ! -f webapp.template.nginx ];then
  echo "ERROR, file webapp.template.nginx does not exitst" >&2
  exit 1
fi

if [ -z "$PORT_KEYCLOAK" ]; then
  echo "ERROR: env variable PORT_KEYCLOAK is unset" >&2
  exit 1
fi

if [ -z "$KC_PUBLIC_ADDRESS" ]; then
  echo "ERROR: env variable KC_PUBLIC_ADDRESS is unset" >&2
  exit 1
fi

if [ -z "$PORT_FRONTEND" ]; then
  echo "ERROR: env variable PORT_FRONTEND is unset" >&2
  exit 1
fi

if [ -z "$WEBAPP_PUBLIC_ADDRESS" ]; then
  echo "ERROR: env variable WEBAPP_PUBLIC_ADDRESS is unset" >&2
  exit 1
fi

if [ -z "$WEBAPP_PUBLIC_PORT" ]; then
  echo "ERROR: env variable WEBAPP_PUBLIC_PORT is unset" >&2
  exit 1
fi

run
