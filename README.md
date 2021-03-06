# constr-sw-2021-2-g1

Esta é uma API de autenticação e gerenciamento de usuários que será utilizada para o projeto do semestre.


## Requisitos

- Alguma JDK na versão 11
- Alguma IDE, preferencialmente IntelliJ
- Docker
- AWS CLI

Para o desenvolvimento, é recomendável subir as imagens Docker do Keycloak e do PostgreSQL, com o comando a seguir:

`./deploy-local.sh keycloak`

## Deploy
Para fazer deploy local:

`./deploy-local.sh`

### ECS utilizando Docker Compose

Para fazer deploy para o ECS (Nota, esta implementação usa o Fargate, que não está no *free tier*):

`./deploy-ecs.sh`

Para especificar um perfil da AWS, pode ser utilizada a variável de ambiente AWS_PROFILE. Exemplo:

`AWS_PROFILE=trabalho ./deploy-ecs.sh`

O *Docker Compose* lê o arquivo `.env` e repassa as variáveis para os contêineres. A API também lê os arquivos `.env` e `.env-default` (este sendo específico do perfil Spring), para que seja possível subir a API localmente sem configurações extras. Para não haver conflitos, esses arquivos não são copiados para a imagem Docker.

https://docs.docker.com/cloud/ecs-integration/#run-a-compose-application

### ECS "semimanual" - *Em construção*
Ainda não automatizamos completamente o deploy para o ECS utilizando instâncias EC2.
O deploy manual consiste em:
1. Criar um cluster no ECS, o que pode ser feito com poucos comandos utilizando o `ecs-cli`;
2. Criar um ELB (Elastic Load Balancer);
3. Criar as definições de tarefas para os contêineres;
4. Criar os serviços para controle e monitoramento das tarefas, com *service discovery* (que utilizar o *Route 53*) e apontamento para o ELB.
