# Dockerfile para Flutter Web
FROM nginx:alpine

# Crear directorio para la aplicación
WORKDIR /usr/share/nginx/html

# Copiar los archivos build de Flutter web
COPY build/web/ .

# Configuración personalizada de nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Exponer puerto 80
EXPOSE 80

# Comando por defecto
CMD ["nginx", "-g", "daemon off;"]