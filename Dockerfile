FROM europe-west3-docker.pkg.dev/gematik-all-infra-prod/demis-dev/base/jetty:9.4.53-jre17-eclipse-temurin-231114

# The STOPSIGNAL instruction sets the system call signal that will be sent to the container to exit
# SIGTERM = 15 - https://de.wikipedia.org/wiki/Signal_(Unix)
STOPSIGNAL SIGTERM

# Define the exposed port or range of ports for the service
EXPOSE 8080

# Define the Health Check
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=30s \
            --retries=3 \
            CMD ["/usr/bin/wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/notification-gateway/actuator/health"]

COPY --chown=jetty target/notification-gateway /var/lib/jetty/webapps/notification-gateway

USER root

RUN rm -rf /var/lib/jetty/webapps/notification-gateway/WEB-INF/classes/certs && \
    ln -sdf /app/config/notification-gateway/certs /var/lib/jetty/webapps/notification-gateway/WEB-INF/classes/certs && \
    chown jetty:jetty -R /var/lib/jetty/webapps/notification-gateway/WEB-INF/classes/certs

USER jetty

### Labels 
ARG COMMIT_HASH
ARG VERSION

LABEL de.gematik.vendor="gematik GmbH" \
      maintainer="software-development@gematik.de" \
      de.gematik.app="DEMIS Notification Gateway" \
      de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/git/demis/notification-gateway.git" \
      de.gematik.commit-sha=$COMMIT_HASH \
      de.gematik.version=$VERSION