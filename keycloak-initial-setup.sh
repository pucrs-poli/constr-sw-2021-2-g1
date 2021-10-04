#!/bin/bash
set -e

SERVER_URL=http://$(hostname -i):8080/auth
# KEYCLOAK_USER=admin
# KEYCLOAK_PASSWORD=Pa55w0rd

REALM_NAME=pucrs-cs
ROLE_ADMIN_NAME=admin
ROLE_PROFESSOR_NAME=professor
ROLE_ALUNO_NAME=aluno

USERNAME=admin
USER_PASS=Pa55w0rd
CLIENT_ID=keyseguro
CLIENT_SECRET=6c923abc-a643-4765-a7d0-5f6bd6d5c672

cd /opt/jboss/keycloak/bin

./kcadm.sh config credentials --server ${SERVER_URL} --realm master --user ${KEYCLOAK_USER} --password ${KEYCLOAK_PASSWORD}
./kcadm.sh create realms -s realm=${REALM_NAME} -s enabled=true
./kcadm.sh create users -r ${REALM_NAME} -s username=${USERNAME} -s enabled=true -o | jq '.id'
./kcadm.sh set-password -r ${REALM_NAME} --username ${USERNAME} --new-password ${USER_PASS}
./kcadm.sh create clients -r ${REALM_NAME} -o \
  -s clientId=${CLIENT_ID} \
  -s enabled=true \
  -s 'redirectUris=["http://localhost:8081/auth"]' \
  -s clientAuthenticatorType=client-secret \
  -s secret=${CLIENT_SECRET} \
  -s directAccessGrantsEnabled=true

./kcadm.sh create roles -r ${REALM_NAME} -s name=${ROLE_ADMIN_NAME} -o
ROLE_ID=$(./kcadm.sh get-roles -r ${REALM_NAME} --rolename ${ROLE_ADMIN_NAME} | jq -r '.id')
./kcadm.sh add-roles -r ${REALM_NAME} --rid $ROLE_ID --cclientid realm-management --rolename view-users
./kcadm.sh add-roles -r ${REALM_NAME} --rid $ROLE_ID --cclientid realm-management --rolename query-users
./kcadm.sh add-roles -r ${REALM_NAME} --rid $ROLE_ID --cclientid realm-management --rolename manage-users
./kcadm.sh add-roles -r ${REALM_NAME} --uusername ${USERNAME} --rolename ${ROLE_ADMIN_NAME}

./kcadm.sh create roles -r ${REALM_NAME} -s name=${ROLE_PROFESSOR_NAME} -o
./kcadm.sh create roles -r ${REALM_NAME} -s name=${ROLE_ALUNO_NAME} -o
