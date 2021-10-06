#!/bin/sh

cd $(dirname $0)/..
./scripts/wait-for-keycloak.sh && java -jar /app/construcao-software.jar
