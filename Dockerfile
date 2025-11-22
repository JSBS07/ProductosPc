# Imagen con JDK
FROM eclipse-temurin:24-jdk-alpine

# Crear carpeta en el contenedor
WORKDIR /app

# Copiar archivo pom.xml y fuente
COPY . .

# Empaquetar el proyecto
RUN ./mvnw -B package

# Exponer el puerto
EXPOSE 8080

# Ejecutar el .jar
CMD ["java", "-jar", "target/*.jar"]
