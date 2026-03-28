#!/bin/bash

function install(){
#set admin credentials
KC_ADMIN_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$KC_BOOTSTRAP_ADMIN_ENC_PASSWORD")
kcadm.sh config credentials \
--server https://$KC_PUBLIC_ADDRESS:$KC_PUBLIC_PORT \
--realm master \
--user $KC_BOOTSTRAP_ADMIN_USERNAME \
--password $KC_ADMIN_PASSWORD

#SET EMAIL TO etadmuser
echo "Set email to keycloak super user"
##Get user id
HOME_MASTER_USER_ID=$(kcadm.sh get users -r master \
-q q=username:$KC_BOOTSTRAP_ADMIN_USERNAME \
--fields id \
--format csv --noquotes)
##Set email
kcadm.sh update users/$HOME_MASTER_USER_ID -r master \
-s firstName="Keycloak" \
-s lastName="Admin User" \
-s email=myself@la-etienda.com \
-s emailVerified=true

#create realm
echo "Create realm"
SMTP_EMAIL_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$SMTP_EMAIL_ENC_PASSWORD")
REALM_ID=$(kcadm.sh create realms -s realm=etrealm \
-s enabled=true \
-s verifyEmail=true \
-s loginWithEmailAllowed=true \
-s registrationAllowed=true \
-s resetPasswordAllowed=true \
-s rememberMe=true -i)
kcadm.sh update realms/$REALM_ID -f - << EOF
{"smtpServer" : {
    "starttls" : "true",
    "auth" : "true",
    "ssl" : "true",
    "password" : "$SMTP_EMAIL_PASSWORD",
    "port" : "$SMTP_EMAIL_PORT",
    "host" : "$SMTP_EMAIL_HOST",
    "from" : "$SMTP_EMAIL_USERNAME",
    "fromDisplayName" : "La e-Tienda",
    "user" : "$SMTP_EMAIL_USERNAME"
  }
}
EOF

#create realm-roles
echo "create role role_manager"
kcadm.sh create roles -r etrealm -s name=role_manager -s 'description=Manager of the application.'
kcadm.sh create roles -r etrealm -s name=role_service -s 'description=Application service.'

#CREATE KC CLIENTS (one for each microservice)
#create client for frontend
echo "Create frontend client"
KC_FRONTEND_CLIENT_ID_SECRET=$(/opt/jasypt/jdecrypt.sh "$KC_FRONTEND_CLIENT_ENC_PASSWORD")
kcadm.sh create clients -r etrealm -f - << EOF
{
  "clientId":"$KC_FRONTEND_CLIENT_ID",
  "name":"Et. Frontend KC Client",
  "enabled":"true",
  "clientAuthenticatorType":"client-secret",
  "secret":"$KC_FRONTEND_CLIENT_ID_SECRET",
  "redirectUris":[
    "http://127.0.0.1:$PORT_FRONTEND/*",
    "http://frontend:$PORT_FRONTEND/*",
    "https://$WEBAPP_PUBLIC_ADDRESS:$KC_PUBLIC_PORT/*",
    "http://$WEBAPP_PUBLIC_ADDRESS/*"]
}
EOF

#create client for user microservice
echo "Create user client"
KC_USER_CLIENT_ID_SECRET=$(/opt/jasypt/jdecrypt.sh "$KC_USER_CLIENT_ENC_PASSWORD")
kcadm.sh create clients -r etrealm -f - << EOF
{
  "clientId":"$KC_USER_CLIENT_ID",
  "name":"Et. User KC Client",
  "enabled":"true",
  "clientAuthenticatorType":"client-secret",
  "secret":"$KC_USER_CLIENT_ID_SECRET",
  "redirectUris":["http://127.0.0.1:8080/*"],
  "directAccessGrantsEnabled":"true",
  "serviceAccountsEnabled":"true"
}
EOF
#add view-role to user client in order to microservice be able to find user info
CLIENT_ID=$(kcadm.sh get clients -r etrealm -q clientId="$KC_USER_CLIENT_ID" -F id --format csv --noquotes)
SERVICE_USER_USERNAME=$(kcadm.sh get clients/"$CLIENT_ID"/service-account-user -r etrealm -F username --format csv --noquotes)
kcadm.sh add-roles -r etrealm --uusername "$SERVICE_USER_USERNAME" --cclientid realm-management --rolename view-users --rolename manage-users
kcadm.sh add-roles -r etrealm --uusername "$SERVICE_USER_USERNAME" --rolename role_service

#create client for webapp microservices
echo "Create webapp client"
KC_WEBAPP_CLIENT_ID_SECRET=$(/opt/jasypt/jdecrypt.sh "$KC_WEBAPP_CLIENT_ENC_PASSWORD")
kcadm.sh create clients -r etrealm -f - << EOF
{
  "clientId":"$KC_WEBAPP_CLIENT_ID",
  "name":"Et. Webapp KC Client",
  "enabled":"true",
  "clientAuthenticatorType":"client-secret",
  "secret":"$KC_WEBAPP_CLIENT_ID_SECRET",
  "serviceAccountsEnabled":"true"
}
EOF
#add view-role to user client in order to microservice be able to find user info
CLIENT_ID=$(kcadm.sh get clients -r etrealm -q clientId="$KC_WEBAPP_CLIENT_ID" -F id --format csv --noquotes)
SERVICE_USER_USERNAME=$(kcadm.sh get clients/"$CLIENT_ID"/service-account-user -r etrealm -F username --format csv --noquotes)
kcadm.sh add-roles -r etrealm --uusername "$SERVICE_USER_USERNAME" --rolename role_service

#create users
echo "create webapp admin user"
APP_USER_ADMIN_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$APP_USER_ADMIN_ENC_PASSWORD")
kcadm.sh create users -r etrealm -s username=$APP_USER_ADMIN_USERNAME -s enabled=true \
-s email=admin@la-etienda.com \
-s firstName="Realm Admin" \
-s lastName="Keycloak" \
-s emailVerified=true
kcadm.sh add-roles --uusername $APP_USER_ADMIN_USERNAME --rolename role_manager -r etrealm
kcadm.sh set-password -r etrealm --username $APP_USER_ADMIN_USERNAME --new-password $APP_USER_ADMIN_PASSWORD

#enable keycloak to send roles in token (userinfo.token.claim: true)
echo "enable keycloak to send roles in token"
ROLES_SCOPE_ID=$(kcadm.sh get client-scopes -r etrealm -F id,name --format csv --noquotes | grep roles | awk -F "," '{print $1}')
REALM_ROLE_MAPPER_ID=$(kcadm.sh get client-scopes/$ROLES_SCOPE_ID/protocol-mappers/models -r etrealm -F id,name --format csv --noquotes | grep "realm roles" | awk -F "," '{print $1}')
kcadm.sh update client-scopes/$ROLES_SCOPE_ID/protocol-mappers/models/$REALM_ROLE_MAPPER_ID -r etrealm -f - << EOF
{
  "id" : "$REALM_ROLE_MAPPER_ID",
  "name" : "realm roles",
  "protocol" : "openid-connect",
  "protocolMapper" : "oidc-usermodel-realm-role-mapper",
  "consentRequired" : false,
  "config" : {
    "introspection.token.claim" : "true",
    "multivalued" : "true",
    "userinfo.token.claim" : "true",
    "user.attribute" : "foo",
    "id.token.claim" : "false",
    "lightweight.claim" : "false",
    "access.token.claim" : "true",
    "claim.name" : "realm_access.roles",
    "jsonType.label" : "String"
  }
}{ "config" : {
    "userinfo.token.claim" : "true",
  }
}
EOF

##CREATE TEST USER
echo "create test user"
APP_TEST_USER_PASSWORD=$(/opt/jasypt/jdecrypt.sh "$APP_USER_TEST_ENC_PASSWORD")
kcadm.sh create users -r etrealm -s username=$APP_USER_TEST_USERNAME -s enabled=true \
-s email=myself@la-etienda.com \
-s firstName="Test User" \
-s lastName="Keycloak" \
-s emailVerified=true

#set password to test user
kcadm.sh set-password -r etrealm --username $APP_USER_TEST_USERNAME --new-password $APP_TEST_USER_PASSWORD
echo "configuarion finished"
}

if [ -z "$KC_BOOTSTRAP_ADMIN_ENC_PASSWORD" ]; then
  echo "Variable, KC_BOOTSTRAP_ADMIN_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$KC_BOOTSTRAP_ADMIN_USERNAME" ]; then
  echo "Variable, KC_BOOTSTRAP_ADMIN_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$SMTP_EMAIL_ENC_PASSWORD" ]; then
  echo "Variable, SMTP_EMAIL_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$SMTP_EMAIL_PORT" ]; then
  echo "Variable, SMTP_EMAIL_PORT, is unset" >&2
  exit 1
fi

if [ -z "$SMTP_EMAIL_HOST" ]; then
  echo "Variable, SMTP_EMAIL_HOST, is unset" >&2
  exit 1
fi

if [ -z "$SMTP_EMAIL_USERNAME" ]; then
  echo "Variable, SMTP_EMAIL_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$KC_FRONTEND_CLIENT_ID" ]; then
  echo "Variable, KC_FRONTEND_CLIENT_ID, is unset" >&2
  exit 1
fi

if [ -z "$KC_FRONTEND_CLIENT_ENC_PASSWORD" ]; then
  echo "Variable, KC_FRONTEND_CLIENT_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$KC_USER_CLIENT_ID" ]; then
  echo "Variable, KC_USER_CLIENT_ID, is unset" >&2
  exit 1
fi

if [ -z "$KC_USER_CLIENT_ENC_PASSWORD" ]; then
  echo "Variable, KC_USER_CLIENT_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$APP_USER_ADMIN_USERNAME" ]; then
  echo "Variable, APP_USER_ADMIN_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$APP_USER_ADMIN_ENC_PASSWORD" ]; then
  echo "Variable, APP_USER_ADMIN_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$APP_USER_TEST_USERNAME" ]; then
  echo "Variable, APP_USER_TEST_USERNAME, is unset" >&2
  exit 1
fi

if [ -z "$APP_USER_TEST_ENC_PASSWORD" ]; then
  echo "Variable, APP_USER_TEST_ENC_PASSWORD, is unset" >&2
  exit 1
fi

if [ -z "$PORT_KEYCLOAK" ]; then
  echo "Variable, PORT_KEYCLOAK, is unset" >&2
  exit 1
fi

if [ -z "$PORT_KEYCLOAK_CONF" ]; then
  echo "Variable, PORT_KEYCLOAK_CONF, is unset" >&2
  exit 1
fi

if [ -z "$KC_PUBLIC_ADDRESS" ]; then
  echo "Variable, KC_PUBLIC_ADDRESS, is unset" >&2
  exit 1
fi

if [ -z "$KC_PUBLIC_PORT" ]; then
  echo "Variable, KC_PUBLIC_PORT, is unset" >&2
  exit 1
fi

if [ -z "$PORT_FRONTEND" ]; then
  echo "Variable, PORT_FRONTEND, is unset" >&2
  exit 1
fi

if [ -z "$WEBAPP_PUBLIC_ADDRESS" ]; then
  echo "Variable, WEBAPP_PUBLIC_ADDRESS, is unset" >&2
  exit 1
fi

#TEST IF CONFIGURATION SCRIPT NEEDS TO BE EXECUTED
if sh -c /opt/keycloak/kc.check.sh; then
  echo "keycloak is already configured"
else
  echo "Configuring keycloak..."
  install
fi

##OPEN PORT TO LISTEN CONNECTION AND CHECK WHEN CONFIGURATION HAS FINISHED
echo "open port and send healthy status"
nc -l -p "$PORT_KEYCLOAK_CONF"