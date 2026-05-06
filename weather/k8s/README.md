# Apply in order: namespace, backend, frontend
# kubectl apply -f k8s/ -n weather-dashboard

# Or apply individually:
# kubectl apply -f k8s/namespace.yaml -n weather-dashboard
# kubectl apply -f k8s/backend-deployment.yaml
# kubectl apply -f k8s/frontend-deployment.yaml
# kubectl apply -f k8s/routes.yaml

# Prerequisites: build and push images to internal registry
# oc new-build --name weather-backend --docker-image python:3.11-slim --binary -l app=weather-backend
# oc new-build --name weather-frontend --docker-image nginx:alpine --binary -l app=weather-frontend