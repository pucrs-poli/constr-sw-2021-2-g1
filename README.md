# constr-sw-2021-2-g1


### Imagens docker

### login
`aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 574344221492.dkr.ecr.sa-east-1.amazonaws.com`


### image api

`docker build  --build-arg JAR_FILE=build/libs/\*.jar  -f Dockerfile.api -t api .`
`docker tag api:latest 574344221492.dkr.ecr.sa-east-1.amazonaws.com/api:latest`
`docker push 574344221492.dkr.ecr.sa-east-1.amazonaws.com/api:latest`



### image keycloak

`docker build -f keycloak/Dockerfile -t keycloak .`
`docker tag keycloak:latest 574344221492.dkr.ecr.sa-east-1.amazonaws.com/keycloak:latest`
`docker push 574344221492.dkr.ecr.sa-east-1.amazonaws.com/keycloak:latest`
