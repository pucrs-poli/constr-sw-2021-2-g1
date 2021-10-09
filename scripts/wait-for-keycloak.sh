#!/bin/sh

RETRIES=60

echo "Waiting for keycloak to start on ${KEYCLOAK_URL}"
until curl -f -s "${KEYCLOAK_URL}/realms/master" > /dev/null
do
    RETRIES=$(($RETRIES - 1))
    if [ $RETRIES -eq 0 ]
    then
        echo "Failed to connect"
        exit 1
    fi
    sleep 1
done