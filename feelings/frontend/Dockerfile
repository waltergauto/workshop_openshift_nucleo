# Build stage
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json .

RUN npm install

COPY public/ public/
COPY src/ src/
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL:-http://feelings-backend:5000}

RUN echo $REACT_APP_URI_URL

RUN npm run build

# Production stage
FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
