# Dockerfile para Railway - En la raíz del proyecto
# Construye el backend que está en ./backend/

# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de configuración de Gradle desde backend/
COPY backend/gradle ./gradle
COPY backend/gradlew .
COPY backend/settings.gradle .
COPY backend/build.gradle .

# Hacer ejecutable gradlew y descargar dependencias
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copiar código fuente
COPY backend/src ./src

# Compilar aplicación
RUN ./gradlew bootJar --no-daemon

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S nomad && adduser -S nomad -G nomad
USER nomad

# Copiar JAR desde etapa de build
COPY --from=builder /app/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Configuración de JVM para contenedor
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
