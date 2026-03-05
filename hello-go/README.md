## Go Hello API — Workshop OpenShift
Aplicación Go minimalista para practicar el ciclo completo de desarrollo en OpenShift.
Estructura
.
├── main.go              # Código fuente
├── go.mod               # Módulo Go
├── Dockerfile           # Multi-stage build (scratch final)
├── k8s/
│   └── manifests.yaml   # Deployment + Service + Route
└── README.md
Endpoints
| Metodo | Ruta | Descripcion |
| ------- | ----- | ---------- |
| GET | / | Info General |
| GET | /api/hello | Respuesta Hello World |
| GET | /health | Health check (liveness) |


Ejemplo de respuesta /api/hello
```
json{
  "message": "Hello from OpenShift!",
  "timestamp": "2025-01-01T12:00:00Z",
  "hostname": "go-hello-abc123-xyz",
  "version": "1.0.0"
}
```

Ejecutar localmente
```
go run main.go
```
```
curl http://localhost:8080/api/hello
```
### Build
```
docker build -t go-hello:latest .
```

### Tag para el registry de OpenShift (ajustar namespace)
```
docker tag go-hello:latest default-route-openshift-image-registry.apps.<cluster>/mi-proyecto/go-hello:latest
```

### Login al registry interno
```
oc registry login
```

### Push
```
docker push default-route-openshift-image-registry.apps.<cluster>/mi-proyecto/go-hello:latest
```

Desplegar en OpenShift
### Crear proyecto
```
oc new-project workshop-go
```

### Opción A — desde el registry interno (luego del push)
```
oc apply -f manifests/*.yaml
```

### Opción B — desde código fuente con Source-to-Image (S2I)
```
oc new-app golang~https://github.com/<tu-usuario>/go-hello-openshift --name=go-hello
```
```
oc expose svc/go-hello
```
Notas OpenShift

El contenedor corre como non-root (compatible con SCC restricted-v2).
El securityContext en el manifiesto elimina todos los capabilities.
El health check en /health alimenta los probes de liveness y readiness.
La Route incluye TLS edge termination con redirect desde HTTP.
