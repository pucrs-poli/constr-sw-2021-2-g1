FROM quay.io/keycloak/keycloak:latest
USER jboss
COPY ./pucrs-cs-realm.json /tmp/pucrs-cs-realm.json
# COPY --chown=jboss:root docker-entrypoint.sh /opt/jboss/tools/
ENTRYPOINT ["/opt/jboss/tools/docker-entrypoint.sh"]
