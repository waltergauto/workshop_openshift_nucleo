# =============================================================================
# Stage 1: build
# =============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build

USER root
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests -q

# =============================================================================
# Stage 2: runtime
# =============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

LABEL org.opencontainers.image.title="metrics-demo" \
      org.opencontainers.image.description="Demo de métricas Micrometer + Prometheus en Quarkus" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.authors="Workshop OpenShift"

ENV LANGUAGE=en_US:en

USER 185
WORKDIR /deployments

COPY --from=build --chown=185 /build/target/quarkus-app/lib/      ./lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar     ./
COPY --from=build --chown=185 /build/target/quarkus-app/app/      ./app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/  ./quarkus/

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -sf http://localhost:8080/health/ready || exit 1

ENTRYPOINT ["java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar"]
