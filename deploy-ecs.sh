#!/bin/bash
set -e
source .env
ECR_IMAGE_API=${ECR_URL}/api:latest
ECR_IMAGE_KEYCLOAK=${ECR_URL}/keycloak:latest
ECS_CONTEXT=cs-t1

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin ${ECR_URL}

docker context inspect ${ECS_CONTEXT} >/dev/null || docker context create ecs ${ECS_CONTEXT}

echo Building API image
docker build -t api .
docker tag api:latest ${ECR_IMAGE_API}
echo Pushing API image
docker push ${ECR_IMAGE_API}

(
  cd ./keycloak
  echo Building Keycloak image
  docker build -t keycloak .
  docker tag keycloak:latest ${ECR_IMAGE_KEYCLOAK}
  echo Pushing Keycloak image
  docker push ${ECR_IMAGE_KEYCLOAK}
)

echo Deploying application
docker --context ${ECS_CONTEXT} compose up