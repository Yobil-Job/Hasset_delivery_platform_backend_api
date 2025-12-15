# Multi-stage build for Spring Boot (Java 21)

# ==== Build stage ====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (better layer caching)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# ==== Run stage ====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/delivery-0.0.1-SNAPSHOT.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]
