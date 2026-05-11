FROM node:20-alpine

# Crear el directorio de la aplicación
WORKDIR /usr/src/app

# Instalar dependencias de la aplicación
COPY package*.json ./
RUN npm install --production

# Copiar el código fuente
COPY . .

# Exponer el puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD [ "npm", "start" ]
