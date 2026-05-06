#!/bin/bash

echo "Iniciando Backend (Quarkus) y Frontend (Astro)..."

# Iniciar backend en segundo plano
echo "Iniciando Backend en http://localhost:8080"
cd backend
mvn quarkus:dev -Dquarkus.http.host=0.0.0.0 &
BACKEND_PID=$!

# Esperar a que el backend esté listo
echo "Esperando a que el backend esté listo..."
sleep 10

# Iniciar frontend
echo "Iniciando Frontend en http://localhost:4321"
cd ../frontend
npm run dev &
FRONTEND_PID=$!

echo ""
echo "Proyectos iniciados:"
echo "   - Backend: http://localhost:8080"
echo "   - Frontend: http://localhost:4321"
echo ""
echo "Presiona Ctrl+C para detener ambos servicios"

# Manejar señal de interrupción
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT

wait
