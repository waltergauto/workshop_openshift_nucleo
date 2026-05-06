# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .

ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM docker.io/nginx:stable-alpine3.23-slim

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
