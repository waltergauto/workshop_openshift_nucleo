# --- STAGE 1: Build ---
# Usamos la imagen de desarrollo que tiene el JDK y Maven/Gradle
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS builder

USER root
WORKDIR /build

# Copiamos el archivo de configuración de dependencias primero para aprovechar la caché de Docker
COPY pom.xml .
# Opcional: descargar dependencias (esto acelera builds posteriores)
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Run ---
# Usamos la imagen runtime, mucho más pequeña y segura
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

LABEL maintainer="Walter Gauto <wgauto@data.com.py>"
LABEL description="Spring Boot App con Java 21 en OpenShift"

WORKDIR /deployments

# Copiamos solo el JAR desde la etapa anterior
# El nombre 'app.jar' es estándar para simplificar el ENTRYPOINT
COPY --from=builder /build/target/*.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8080

# Configuraciones de seguridad para OpenShift:
# Las imágenes UBI de Red Hat usan el usuario 185 por defecto
USER 185

# Ejecutamos la aplicación
# Usamos la sintaxis de array para que las señales de Linux (SIGTERM) se manejen correctamente
ENTRYPOINT ["java", "-jar", "app.jar"]
