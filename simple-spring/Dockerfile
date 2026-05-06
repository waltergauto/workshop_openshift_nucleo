FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

WORKDIR /app

COPY target/simple-spring-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
