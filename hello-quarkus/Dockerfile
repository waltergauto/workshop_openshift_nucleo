# Stage 1: Build the Quarkus application using your existing Maven image
FROM localhost/openjdk-21-devel:v1 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the project's pom.xml and download dependencies
# This step helps leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src/ src/

# Package the Quarkus application as a fast-jar for optimized JVM execution
# -Pprod activates the 'prod' profile if defined in your pom.xml
# -Dquarkus.package.type=fast-jar creates an optimized JAR structure
# -DskipTests skips running tests during the build
RUN mvn package -Pprod -Dquarkus.package.type=fast-jar -DskipTests -B

# Stage 2: Create the final runtime image using a Red Hat UBI OpenJDK Runtime
# UBI images provide a stable, secure, and supported base for your applications.
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime AS final

# Set the working directory
WORKDIR /app

# Copy the built application artifacts from the 'build' stage
# The fast-jar structure places dependencies in 'lib' and the main JAR in the root
COPY --from=build /app/target/quarkus-app/lib /app/lib
COPY --from=build /app/target/quarkus-app/*.jar /app
COPY --from=build /app/target/quarkus-app/quarkus /app/quarkus
COPY --from=build /app/target/quarkus-app/app/ /app/app

# Expose the default Quarkus HTTP port
EXPOSE 8080

# Set environment variables for Quarkus JVM execution
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Define the command to run the Quarkus application
CMD ["java", "-jar", "quarkus-run.jar"]
