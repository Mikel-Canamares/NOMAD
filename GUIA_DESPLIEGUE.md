# Guía de Despliegue - NOMAD MCP

Esta guía detalla el proceso completo para desplegar el backend de NOMAD en Railway y configurar la aplicación Android para demo en dispositivos físicos.

## Índice

1. [Requisitos Previos](#requisitos-previos)
2. [Despliegue del Backend en Railway](#despliegue-del-backend-en-railway)
3. [Configuración de la Aplicación Android](#configuración-de-la-aplicación-android)
4. [Verificación del Despliegue](#verificación-del-despliegue)
5. [Troubleshooting](#troubleshooting)

---

## Requisitos Previos

### APIs y Claves Necesarias

1. **Cuenta Railway** (https://railway.app)
   - Crear cuenta gratuita o de pago
   - Plan gratuito: $5 de crédito mensual

2. **OpenAI API Key**
   - Obtener de https://platform.openai.com/api-keys
   - Modelo requerido: GPT-4 (gpt-4)
   - Costo estimado: ~$0.50/hora de uso

3. **Google Places API Key**
   - Crear proyecto en https://console.cloud.google.com
   - Habilitar APIs:
     - Places API (New)
     - Maps SDK for Android
   - Crear credencial tipo "API Key"
   - Restricciones recomendadas:
     - Aplicación: Restricción por referrer HTTP (para backend)
     - Aplicación: Restricción por Android app (para Android)

### Herramientas de Desarrollo

1. **Backend**:
   - Java 21 o superior
   - Gradle 8.7+
   - Git

2. **Android**:
   - Android Studio Iguana (2023.2.1) o superior
   - Android SDK 26-35
   - Dispositivo físico Android 8.0+ (recomendado Android 10+)
   - Cable USB para instalación

---

## Despliegue del Backend en Railway

### Paso 1: Preparar el Repositorio

```bash
# 1. Asegúrate de estar en la rama correcta
git checkout feature/prototipo

# 2. Hacer commit de todos los cambios
git add .
git commit -m "Backend listo para despliegue en Railway"

# 3. Push al repositorio remoto
git push origin feature/prototipo
```

### Paso 2: Crear Proyecto en Railway

1. Acceder a https://railway.app
2. Click en "New Project"
3. Seleccionar "Deploy from GitHub repo"
4. Autorizar Railway para acceder a tu repositorio
5. Seleccionar el repositorio NOMAD
6. Railway detectará automáticamente el Dockerfile

### Paso 3: Configurar Variables de Entorno

En el dashboard de Railway, ir a la pestaña "Variables" y añadir:

```bash
# CRÍTICO: APIs requeridas
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx
GOOGLE_PLACES_API_KEY=AIzaSyxxxxxxxxxxxxx

# CRÍTICO: Perfil de Spring Boot
SPRING_PROFILES_ACTIVE=production

# Opcional: Puerto (Railway lo asigna automáticamente)
# PORT=8080
```

### Paso 4: Configurar el Dominio

1. En Railway, ir a "Settings" > "Networking"
2. Click en "Generate Domain"
3. Railway generará un dominio público: `nomad-backend-production.up.railway.app`
4. **IMPORTANTE**: Anotar esta URL para configurar la app Android

### Paso 5: Desplegar

```bash
# Railway desplegará automáticamente al hacer push

# Para redesplegar manualmente:
# 1. En Railway dashboard, click en "Deployments"
# 2. Click en "Deploy" en el último commit
```

### Paso 6: Verificar el Despliegue

```bash
# Verificar que el backend está activo
curl https://nomad-backend-production.up.railway.app/api/actuator/health

# Respuesta esperada:
# {"status":"UP"}

# Verificar endpoint de POIs (requiere lat/lng válidos)
curl "https://nomad-backend-production.up.railway.app/api/poi/nearby?lat=40.4168&lng=-3.7038&radius=2000"
```

### Monitoreo del Backend

Railway proporciona métricas automáticas:

1. **Logs**: Ver logs en tiempo real en la pestaña "Logs"
2. **Métricas**: CPU, memoria, red en la pestaña "Metrics"
3. **Health Checks**: Railway monitoreará `/actuator/health` automáticamente

---

## Configuración de la Aplicación Android

### Paso 1: Configurar Google Maps API Key

1. Copiar el archivo de ejemplo:
```bash
cd app-android/app
cp secrets.properties.example secrets.properties
```

2. Editar `app-android/app/secrets.properties`:
```properties
MAPS_API_KEY=AIzaSy_TU_CLAVE_DE_GOOGLE_MAPS
```

### Paso 2: Configurar URL del Backend en Release

Editar `app-android/app/build.gradle.kts` (línea 36):

```kotlin
release {
    isMinifyEnabled = false
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
    // CAMBIAR esta URL por la URL de Railway
    buildConfigField("String", "DEFAULT_BACKEND_URL", "\"https://TU-DOMINIO-RAILWAY.up.railway.app/api/\"")
}
```

**Ejemplo real**:
```kotlin
buildConfigField("String", "DEFAULT_BACKEND_URL", "\"https://nomad-backend-production.up.railway.app/api/\"")
```

### Paso 3: Compilar Release Build

```bash
cd app-android

# Opción 1: APK release (recomendado para demo)
./gradlew assembleRelease

# Opción 2: Bundle para Google Play (si es necesario)
./gradlew bundleRelease
```

El APK se generará en:
```
app-android/app/build/outputs/apk/release/app-release.apk
```

### Paso 4: Instalar en Dispositivo Físico

#### Opción A: Instalación vía Android Studio

1. Conectar el dispositivo por USB
2. Habilitar "Opciones de Desarrollador" en el dispositivo:
   - Ir a Ajustes > Acerca del teléfono
   - Tocar "Número de compilación" 7 veces
3. Habilitar "Depuración USB" en Opciones de Desarrollador
4. En Android Studio:
   - Seleccionar el dispositivo en el selector
   - Click en Run (▶️) con build variant "release"

#### Opción B: Instalación Manual (ADB)

```bash
# Verificar que el dispositivo está conectado
adb devices

# Instalar APK
adb install app-android/app/build/outputs/apk/release/app-release.apk

# Si ya está instalado, usar -r para reinstalar:
adb install -r app-android/app/build/outputs/apk/release/app-release.apk
```

#### Opción C: Instalación por Transferencia de Archivo

1. Copiar el APK al dispositivo (cable USB, email, Drive, etc.)
2. En el dispositivo:
   - Ir a Ajustes > Seguridad
   - Habilitar "Fuentes desconocidas" o "Instalar apps desconocidas"
3. Abrir el APK desde el explorador de archivos
4. Confirmar instalación

### Paso 5: Permisos Requeridos en el Dispositivo

Al abrir NOMAD por primera vez, solicitar:

1. **Ubicación** (CRÍTICO):
   - Permitir "Todo el tiempo" o "Solo mientras uso la app"
   - Necesario para detectar POIs cercanos

2. **Micrófono** (para asistente de voz):
   - Permitir para usar comandos de voz
   - Opcional si solo se usa interfaz táctil

---

## Verificación del Despliegue

### Checklist Backend

- [ ] Backend accesible en URL pública de Railway
- [ ] `/actuator/health` retorna `{"status":"UP"}`
- [ ] `/api/poi/nearby` retorna POIs (con parámetros válidos)
- [ ] `/api/voice/chat` responde correctamente
- [ ] Logs de Railway no muestran errores críticos
- [ ] Métricas de Railway muestran uso normal de CPU/memoria

### Checklist Android

- [ ] App instalada en dispositivo físico
- [ ] Permisos de ubicación concedidos
- [ ] Mapa se carga correctamente
- [ ] Se muestran POIs cercanos a ubicación actual
- [ ] Categorías funcionan (Historia, Gastronomía, etc.)
- [ ] Asistente de voz responde (si se concedió permiso de micrófono)
- [ ] Ajustes persisten entre sesiones
- [ ] No hay crashes al usar la app

### Test de Conectividad Backend-Android

1. Abrir NOMAD en el dispositivo
2. Ir a Ajustes (icono ⚙️ arriba a la derecha)
3. Verificar que "URL del Backend" muestra la URL de Railway
4. Volver al mapa
5. Verificar que se cargan POIs (puede tardar 2-5 segundos)

Si no carga POIs:
- Verificar logs de Railway (puede ser problema de API keys)
- Verificar conexión a internet del dispositivo
- Verificar que la URL en Ajustes es correcta

---

## Troubleshooting

### Backend no despliega en Railway

**Síntoma**: El build falla en Railway

**Soluciones**:
1. Verificar logs de Railway para ver el error exacto
2. Asegurarse de que `backend/Dockerfile` existe y es correcto
3. Verificar que `railway.toml` está en la raíz de `backend/`
4. Railway debe detectar el directorio `backend/` como root path

**Configuración de Root Directory en Railway**:
- Settings > General > Root Directory: `backend`

### Backend despliega pero retorna 500

**Síntoma**: `/actuator/health` retorna error 500 o timeout

**Causas comunes**:
1. Variables de entorno faltantes
   - Verificar que `OPENAI_API_KEY` está configurada
   - Verificar que `GOOGLE_PLACES_API_KEY` está configurada
   - Verificar que `SPRING_PROFILES_ACTIVE=production`

2. API Keys inválidas
   - Probar las keys en https://platform.openai.com (OpenAI)
   - Probar las keys en https://console.cloud.google.com (Google)

### Android no conecta con el backend

**Síntoma**: App no carga POIs, muestra "Error cargando POIs"

**Soluciones**:
1. Verificar URL del backend en Ajustes
   - Debe terminar en `/api/`
   - Debe usar `https://` (no `http://`)
   - Ejemplo: `https://nomad-backend-production.up.railway.app/api/`

2. Verificar conectividad:
```bash
# Desde el dispositivo Android, abrir Chrome y visitar:
https://TU-URL-RAILWAY.up.railway.app/api/actuator/health

# Debe mostrar: {"status":"UP"}
```

3. Verificar logs de Railway para ver si llegan las peticiones

### Asistente de voz no responde

**Síntoma**: Al presionar el botón de voz, no pasa nada o muestra error

**Soluciones**:
1. Verificar permiso de micrófono concedido
2. Verificar que `OPENAI_API_KEY` es válida y tiene crédito
3. Verificar logs de Railway:
```bash
# Buscar errores relacionados con OpenAI:
# "401 Unauthorized" -> API key inválida
# "429 Too Many Requests" -> Sin crédito o límite excedido
```

### Google Maps no se muestra

**Síntoma**: Pantalla gris o mensaje "Error loading map"

**Soluciones**:
1. Verificar que `secrets.properties` tiene la API key correcta
2. Verificar que la API key tiene habilitado "Maps SDK for Android"
3. Verificar que la restricción de Android app incluye el package name:
   - Package name: `com.nomad.app`
   - SHA-1: Obtener con:
```bash
cd app-android
./gradlew signingReport
```

### Consumo excesivo de batería

**Síntoma**: La app consume mucha batería

**Soluciones**:
1. El sistema de debounce ya limita las actualizaciones de POIs (cada 200m)
2. Aumentar la distancia mínima en Ajustes:
   - Cambiar "Radio de búsqueda de POIs" a 5000 metros
   - Esto reduce la frecuencia de llamadas al backend

### Errores de compilación Android

**Síntoma**: `gradlew assembleRelease` falla

**Errores comunes**:

1. **BuildConfig not found**:
```bash
# Asegurarse de que build.gradle.kts tiene:
buildFeatures {
    compose = true
    buildConfig = true  // <- CRÍTICO
}
```

2. **secrets.properties not found**:
```bash
# Crear el archivo:
cd app-android/app
echo "MAPS_API_KEY=TU_API_KEY" > secrets.properties
```

3. **Sync Gradle**:
```bash
# Limpiar y reconstruir:
cd app-android
./gradlew clean
./gradlew assembleRelease
```

---

## Costos Estimados

### Railway (Backend)

**Plan Gratuito**:
- $5 de crédito mensual
- Suficiente para ~100 horas de uso ligero
- Recomendado para demos y testing

**Plan Developer ($5/mes)**:
- $5 de crédito incluido
- Adicional: $0.000463/GB-second de RAM

**Estimación para demo**:
- Demo de 2 horas con 3 clientes: ~$0.50
- Uso mensual moderado (10 horas): ~$2-3

### OpenAI API

**GPT-4 (gpt-4)**:
- Input: $0.03 / 1K tokens
- Output: $0.06 / 1K tokens

**Estimación**:
- Conversación típica: 200 tokens input + 150 tokens output = ~$0.015
- 1 hora de uso intensivo: ~$0.50
- Sistema híbrido reduce 97% vs Realtime API (que costaría ~$18/hora)

### Google Places API

**Precios**:
- Nearby Search: $32 / 1,000 requests
- Place Details: $17 / 1,000 requests (si se usa)

**Estimación**:
- Demo de 2 horas: ~50 requests = ~$1.60
- Crédito gratuito mensual: $200 (suficiente para demos)

**Total Demo (2 horas, 3 dispositivos)**:
- Railway: $0.50
- OpenAI: $1.50
- Google Places: $1.60
- **Total: ~$3.60**

---

## Próximos Pasos

1. **Antes de la Demo**:
   - [ ] Probar el flujo completo end-to-end
   - [ ] Verificar la ruta Madrid-Toledo en Google Maps
   - [ ] Preparar datos de ejemplo de POIs en la ruta
   - [ ] Cargar crédito en OpenAI si es necesario

2. **Durante la Demo**:
   - [ ] Tener laptop con Railway dashboard abierto (monitoreo)
   - [ ] Tener dispositivo con batería completa
   - [ ] Tener datos móviles o WiFi estable
   - [ ] Preparar script de demo (ver GUIA_DEMO.md)

3. **Post-Demo**:
   - [ ] Revisar logs de Railway para errores
   - [ ] Revisar uso de APIs (costos)
   - [ ] Recopilar feedback de clientes
   - [ ] Planificar mejoras según feedback
