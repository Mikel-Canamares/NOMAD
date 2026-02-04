# Instrucciones Finales - NOMAD Instalado

## ✅ Estado Actual

### Android App
- ✅ **APK compilado**: `app-android/app/build/outputs/apk/debug/app-debug.apk`
- ✅ **Instalado en dispositivo**: AKBC9X4626G02375
- ✅ **Tamaño**: 44 MB

### Backend
- ✅ **Compilado exitosamente** en Railway
- ⚠️ **Healthcheck fallando** - Necesita configuración

---

## 🔧 Pasos para Completar el Despliegue

### 1. Verificar Variables de Entorno en Railway

Ve a **Railway Dashboard > Tu Proyecto > Variables** y asegúrate de tener:

```bash
SPRING_PROFILES_ACTIVE=production
OPENAI_API_KEY=sk-proj-XXXXXXXXXX
GOOGLE_PLACES_API_KEY=AIzaXXXXXXXXXXXX
```

**⚠️ CRÍTICO**: Sin estas variables, el backend NO arrancará.

### 2. Ver los Logs del Backend

En Railway:
1. Ve a **Deployments**
2. Click en el deployment actual
3. Click en **View Logs**
4. Busca errores que indiquen por qué falla el arranque

**Errores comunes**:
- `Failed to load OpenAI API key` → Verifica OPENAI_API_KEY
- `Google Places API error` → Verifica GOOGLE_PLACES_API_KEY
- `Port already in use` → Railway maneja esto automáticamente
- `Connection refused` → Problema de red, contacta soporte de Railway

### 3. Obtener URL Pública del Backend

Una vez que el backend arranque correctamente:

1. En Railway > Tu servicio > **Settings**
2. Sección **Networking** o **Domains**
3. Click en **Generate Domain** si no existe
4. Copiar la URL (ejemplo: `https://nomad-production.up.railway.app`)

### 4. Configurar URL en la App Android

#### Opción A: Configurar en tiempo de ejecución (Rápido)

1. Abrir NOMAD en el dispositivo
2. Tocar el icono **⚙️ Ajustes** (arriba a la derecha)
3. En "URL del Backend", cambiar a tu URL de Railway
4. Ejemplo: `https://nomad-production.up.railway.app/api/`
5. **IMPORTANTE**: Debe terminar en `/api/`
6. Volver al mapa

#### Opción B: Recompilar con URL de Release (Para producción)

1. Editar `app-android/app/build.gradle.kts` línea 36:
```kotlin
buildConfigField("String", "DEFAULT_BACKEND_URL", "\"https://TU-URL-RAILWAY.up.railway.app/api/\"")
```

2. Recompilar release:
```bash
cd app-android
gradlew.bat assembleRelease
```

3. Reinstalar:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 Verificar que Todo Funciona

### 1. Backend Health Check

```bash
curl https://TU-URL-RAILWAY.up.railway.app/api/actuator/health
```

**Respuesta esperada**:
```json
{"status":"UP"}
```

### 2. Probar Endpoint de POIs

```bash
curl "https://TU-URL-RAILWAY.up.railway.app/api/poi/nearby?lat=40.4168&lng=-3.7038&radius=2000"
```

Debería retornar una lista de POIs cercanos al Palacio Real de Madrid.

### 3. Probar en la App

1. Abrir NOMAD en el dispositivo
2. Permitir permisos de **Ubicación** (CRÍTICO)
3. Permitir permisos de **Micrófono** (opcional, para voz)
4. Verificar que el mapa carga
5. Verificar que se muestran POIs cercanos (marcadores de colores)
6. Tocar un marcador y verificar información
7. Tocar "Pregunta al asistente" y verificar que responde

---

## 🐛 Troubleshooting Rápido

### Backend no arranca en Railway

**Síntomas**: Healthcheck falla constantemente

**Soluciones**:
1. Verificar que las 3 variables de entorno están configuradas
2. Ver logs en Railway para identificar error exacto
3. Verificar que las API keys son válidas:
   - OpenAI: https://platform.openai.com/api-keys
   - Google Places: https://console.cloud.google.com

### App no carga POIs

**Síntomas**: Pantalla vacía o mensaje "Error cargando POIs"

**Soluciones**:
1. Verificar URL del backend en Ajustes
2. Asegurarse de que termina en `/api/`
3. Verificar permisos de ubicación (debe ser "Permitir siempre" o "Solo mientras uso la app")
4. Verificar conexión a internet del dispositivo
5. Abrir Chrome en el dispositivo y visitar: `https://TU-URL-RAILWAY.up.railway.app/api/actuator/health`
   - Si no carga → Backend no está funcionando
   - Si carga {"status":"UP"} → Backend OK, problema en la app

### Asistente de voz no responde

**Síntomas**: Al presionar botón de voz, no pasa nada

**Soluciones**:
1. Verificar permiso de micrófono concedido
2. Verificar que OPENAI_API_KEY es válida y tiene crédito
3. Ver logs de Railway, buscar errores relacionados con OpenAI

### Google Maps no se muestra

**Síntomas**: Pantalla gris en lugar del mapa

**Soluciones**:
1. Verificar que `app-android/app/secrets.properties` tiene la API key correcta
2. Verificar que la API key tiene habilitado "Maps SDK for Android"
3. Verificar que la restricción de Android app incluye el package name: `com.nomad.app`

---

## 📊 Verificación de Costos

Una vez que todo funcione, puedes verificar el uso de las APIs:

**OpenAI**:
- Dashboard: https://platform.openai.com/usage
- Costo esperado: ~$0.01 por conversación

**Google Places**:
- Console: https://console.cloud.google.com
- Pricing: $200 de crédito mensual gratuito

**Railway**:
- Dashboard: https://railway.app/project/tu-proyecto
- Costo: $5 de crédito mensual gratuito

---

## 🎯 Siguiente Paso

1. **Configura las variables de entorno** en Railway
2. **Reinicia el servicio** en Railway (Deployments > Redeploy)
3. **Espera que el healthcheck pase** (~1 minuto)
4. **Obtén la URL pública**
5. **Configúrala en la app** Android (Ajustes)
6. **Prueba la app** en el dispositivo

---

## 📞 Soporte

Si después de configurar las variables de entorno el backend sigue sin arrancar:

1. Comparte los logs completos de Railway
2. Verifica que las API keys funcionan probándolas manualmente:

```bash
# OpenAI
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer TU-OPENAI-KEY"

# Google Places
curl "https://places.googleapis.com/v1/places:searchNearby" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: TU-GOOGLE-KEY" \
  -H "X-Goog-FieldMask: places.displayName"
```

¡Buena suerte! 🚀
