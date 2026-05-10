# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Fast JAR runtime
FROM eclipse-temurin:21-jre-alpine AS runtime-fastjar
WORKDIR /app
COPY --from=build /app/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/target/quarkus-app/*.jar ./
COPY --from=build /app/target/quarkus-app/app/ ./app/
COPY --from=build /app/target/quarkus-app/quarkus/ ./quarkus/
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]

# Stage 3: Native build (uncomment to use native executable)
# FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS native-build
# WORKDIR /app
# COPY --from=build /app/src ./src
# COPY --from=build /app/pom.xml .
# RUN ./mvnw package -Pnative -DskipTests
# 
# FROM quay.io/quarkus/ubi-quarkus-minimal-mode:jdk-21
# WORKDIR /app
# COPY --from=native-build /app/target/*-runner /app/application
# EXPOSE 8080
# ENTRYPOINT ["/app/application"]
