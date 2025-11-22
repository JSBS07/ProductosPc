# Build stage: usar imagen oficial de Maven con JDK para compilar
FROM maven:3.9.4-eclipse-temurin-24 AS build

WORKDIR /app

# Copiar sólo lo necesario primero para aprovechar cache (pom y wrapper)
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Copiar el resto del proyecto
COPY src ./src

# Construir el proyecto sin tests
RUN mvn -B -DskipTests package


# Runtime stage: imagen ligera con JRE
FROM eclipse-temurin:24-jre-jammy

WORKDIR /app

# JAR generado desde el stage de build
COPY --from=build /app/target/*.jar app.jar

# Puerto expuesto por la app Spring Boot
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
