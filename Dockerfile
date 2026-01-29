# Dockerfile para Railway - Spring Boot 3.2+ con Java 21
# Basado en mejores prácticas de Docker oficial y Railway

# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

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

# Compilar aplicación y VERIFICAR que el JAR existe
RUN ./gradlew bootJar --no-daemon && \
    echo "=== Verificando JAR ===" && \
    ls -lah build/libs/*.jar && \
    test -f build/libs/*.jar || (echo "ERROR: JAR no generado" && exit 1)

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app

# Crear usuario no-root
RUN addgroup --system javauser && adduser -S -s /usr/sbin/nologin -G javauser javauser

# Copiar JAR desde etapa de build
COPY --from=builder --chown=javauser:javauser /app/build/libs/*.jar app.jar

# Verificar que el JAR se copió correctamente
RUN ls -lah app.jar && test -f app.jar || (echo "ERROR: app.jar no copiado" && exit 1)

# Cambiar a usuario no-root
USER javauser

# Exponer puerto (Railway usa PORT variable)
EXPOSE 8080

# ENTRYPOINT en exec form - UNA LÍNEA (sin backslashes)
# Optimizado para Railway: heap reducido + G1GC + String Deduplication
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
