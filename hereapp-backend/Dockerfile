# Etapa 1: Compilar el proyecto con Maven
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
# Copia los archivos pom.xml y el directorio src al contenedor
COPY pom.xml .
COPY src ./src
# Compila el proyecto (se genera el JAR en la carpeta target)
RUN mvn clean package -DskipTests

# Etapa 2: Crear la imagen final usando Amazon Corretto 21
FROM amazoncorretto:21
WORKDIR /app
# Copia el JAR compilado de la etapa build
COPY --from=build /app/target/hereapp-backend-0.0.1-SNAPSHOT.jar hereapp-backend-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "hereapp-backend-0.0.1-SNAPSHOT.jar"]