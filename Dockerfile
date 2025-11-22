# Build stage: usar una imagen válida de Maven + Temurin
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar primero archivos clave
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn


RUN chmod +x ./mvnw

# Copiar el resto del proyecto
COPY src ./src

# Compilar el proyecto
RUN ./mvnw -B -DskipTests package

# Runtime stage usando JRE estable
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR generado
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]