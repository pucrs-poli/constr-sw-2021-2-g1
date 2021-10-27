#!/bin/bash
set -e
source .env
ECR_IMAGE_API=${AWS_ECR_URL}/api:latest
ECR_IMAGE_KEYCLOAK=${AWS_ECR_URL}/keycloak:latest

aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${AWS_ECR_URL}

docker context inspect ${DOCKER_ECS_CONTEXT} >/dev/null || docker context create ecs ${DOCKER_ECS_CONTEXT}

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
docker --context ${DOCKER_ECS_CONTEXT} compose --project-name ${DOCKER_ECS_CONTEXT} up
