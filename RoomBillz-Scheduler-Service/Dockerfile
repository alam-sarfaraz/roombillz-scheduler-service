# ----------- Stage 1: Build -----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B


# ----------- Stage 2: Runtime -----------
FROM eclipse-temurin:21-jre

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl

# Create non-root user
RUN useradd -ms /bin/bash appuser

WORKDIR /home/appuser/app

# Copy JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Give permission to appuser
RUN chown -R appuser:appuser /home/appuser/app

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8083

# Healthcheck using actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
