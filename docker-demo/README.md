# Docker Demo — Workshop de Contenedores

Proyecto Express + TypeScript diseñado para demostrar la diferencia
real entre un Dockerfile básico y uno multi-stage.

## Estructura

```
docker-demo/
├── src/
│   └── index.ts          # App Express en TypeScript
├── package.json          # dependencies + devDependencies pesadas
├── tsconfig.json
├── Dockerfile.basic      # imagen única con node:20 completo
└── Dockerfile.multistage # builder node:20 → prod node:20-alpine
```

## Pasos del demo

### 1. Construir ambas imágenes

```bash
# Básico (tarda más — descarga node:20 ~900 MB)
docker build -f Dockerfile.basic -t demo-app:basic .

# Multi-stage
docker build -f Dockerfile.multistage -t demo-app:prod .
```

### 2. Comparar tamaños — el momento "wow"

```bash
docker images | grep demo-app
```

Resultado esperado:
```
demo-app   prod    xxxxxxxxxxxx   2 min ago   ~160 MB
demo-app   basic   xxxxxxxxxxxx   5 min ago   ~1.1 GB
```

### 3. Verificar qué hay dentro de cada imagen

```bash
# ¿Hay TypeScript en la imagen básica? Sí
docker run --rm demo-app:basic ls node_modules | grep typescript

# ¿Hay TypeScript en la imagen de producción? No
docker run --rm demo-app:prod ls node_modules | grep typescript

# ¿Hay código fuente .ts en producción? No — solo dist/
docker run --rm demo-app:prod ls /app

# ¿El compilador tsc está disponible en producción? No
docker run --rm demo-app:prod npx tsc --version
```

### 4. Inspeccionando las capas

```bash
# Ver las capas y cuánto pesa cada una
docker history demo-app:basic
docker history demo-app:prod
```

### 5. Ambas apps corren igual

```bash
docker run -d -p 3001:3000 --name app-basic demo-app:basic
docker run -d -p 3002:3000 --name app-prod  demo-app:prod

curl http://localhost:3001/health
curl http://localhost:3002/health
# Ambas devuelven: {"status":"ok"}

docker stop app-basic app-prod
docker rm   app-basic app-prod
```

## Por qué importa el tamaño

| | Básico | Multi-stage |
|---|---|---|
| Tamaño | ~1.1 GB | ~160 MB |
| Pull en CI/CD | lento | 7x más rápido |
| Superficie de ataque | alta | mínima |
| Código fuente expuesto | sí | no |
| devTools en prod | sí | no |
