# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM docker.io/maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

WORKDIR /app
COPY --from=builder /app/target/products-api-1.0.0.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
