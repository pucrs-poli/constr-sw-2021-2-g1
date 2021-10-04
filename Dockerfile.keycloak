FROM quay.io/keycloak/keycloak:latest
USER root 
RUN microdnf install jq
USER jboss
# COPY --chown=jboss:root docker-entrypoint.sh /opt/jboss/tools/
ENTRYPOINT ["/opt/jboss/tools/docker-entrypoint.sh"]