FROM gradle:7.1.1-jdk11 AS builder
ARG KEYCLOAK_URL
ENV KEYCLOAK_URL=${KEYCLOAK_URL}

WORKDIR /app

COPY ./src ./src
COPY ./*.kts ./

RUN gradle assemble --no-daemon

FROM eclipse-temurin:11

ENV TZ=America/Sao_Paulo
EXPOSE 8080
WORKDIR /app

COPY --from=builder /app/build/libs/construcao-software.jar ./
COPY ./scripts ./scripts

CMD ["./scripts/run-api.sh"]
