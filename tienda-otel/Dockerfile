# ─── Stage 1: build ───────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build

USER root
WORKDIR /build

# Copiar fuentes
COPY pom.xml .
COPY src ./src

# Build del fat-jar (quarkus-app)
RUN mvn package -DskipTests -q

# ─── Stage 2: runtime ─────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

ENV LANGUAGE='en_US:en'

# Usuario no-root requerido por OpenShift
USER 185

WORKDIR /deployments

# Copiar el resultado del build
COPY --from=build --chown=185 /build/target/quarkus-app/lib/           ./lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar          ./
COPY --from=build --chown=185 /build/target/quarkus-app/app/           ./app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/       ./quarkus/

EXPOSE 8080

# Puerto de gestión (health/metrics)
EXPOSE 9000

ENTRYPOINT ["java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar"]
