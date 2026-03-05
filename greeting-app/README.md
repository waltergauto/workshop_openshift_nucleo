# Local (hardcodeado)
podman build -t astro-frontend .

# Apuntando a un host remoto o servicio
podman build --build-arg PUBLIC_API_URL=http://<IP>:8080 -t astro-frontend .

# En OpenShift con nombre de servicio
podman build --build-arg PUBLIC_API_URL=http://hello-quarkus:8080 -t astro-frontend .
