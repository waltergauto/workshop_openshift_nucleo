FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quarkus-app/lib /app/lib
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app /app/app
COPY --from=build /app/target/quarkus-app/quarkus /app/quarkus
ENV JAVA_OPTIONS="-Dotel.traces.exporter=otlp -Dotel.metrics.exporter=none -Dotel.logs.exporter=none"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]