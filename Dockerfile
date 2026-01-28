# Dockerfile para Railway - Spring Boot 3.2+ con Java 21
# Basado en mejores prácticas de Docker oficial y Railway

# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copiar archivos de configuración de Gradle
COPY backend/gradle ./gradle
COPY backend/gradlew .
COPY backend/settings.gradle .
COPY backend/build.gradle .

# Hacer ejecutable gradlew
RUN chmod +x gradlew

# Descargar dependencias (cacheado)
RUN ./gradlew dependencies --no-daemon || true

# Copiar código fuente
COPY backend/src ./src

# Compilar aplicación
RUN ./gradlew bootJar --no-daemon

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app

# Crear usuario no-root
RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser

# Copiar JAR desde etapa de build
COPY --from=builder --chown=javauser:javauser /build/build/libs/*.jar app.jar

# Cambiar a usuario no-root
USER javauser

# Exponer puerto (Railway usa PORT variable)
EXPOSE 8080

# ENTRYPOINT en exec form para correcto manejo de señales
# Railway requiere que la app escuche en $PORT
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
