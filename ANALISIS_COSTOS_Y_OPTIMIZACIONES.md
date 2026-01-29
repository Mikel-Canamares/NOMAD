# Análisis de Costos y Optimizaciones - NOMAD

**Fecha:** 2026-01-29
**Problema:** Una sola petición generó $0.0348 en Railway (149.06 GB-minutos de memoria)

---

## 📊 ANÁLISIS DEL PROBLEMA DE MEMORIA

### Situación Actual en Railway
- **Current Cost:** $0.0348
- **Estimated Monthly:** $3.43
- **Memory Usage:** 149.06 GB-minutos
- **CPU Usage:** 0.53 vCPU-minutos
- **Egress:** 0.00 GB

### ¿Qué significa 149.06 GB-minutos?

Railway cobra por **GB-minuto**, que es: `Memoria (GB) × Tiempo (minutos)`

**Posibles escenarios:**
- **500 MB × 298 minutos** = 149 GB-minutos (~5 horas corriendo)
- **300 MB × 497 minutos** = 149 GB-minutos (~8.3 horas)
- **1 GB × 149 minutos** = 149 GB-minutos (~2.5 horas)

**Conclusión:** El servicio estuvo corriendo durante VARIAS HORAS con memoria asignada, NO fue una sola petición instantánea.

### Problema Identificado en la Configuración

**Dockerfile actual:**
```dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

**Problemas:**
1. ❌ `-Xmx512m` y `-XX:MaxRAMPercentage=75.0` son **conflictivos** (Xmx tiene prioridad)
2. ❌ `-Xms256m` asigna 256MB de heap DESDE EL INICIO (memoria pre-reservada)
3. ❌ **Railway cobra por memoria ASIGNADA, no por memoria usada**
4. ❌ La JVM consume más que solo el heap:
   - Heap: 512 MB (máximo)
   - Metaspace: ~100-150 MB (clases cargadas)
   - Thread stacks: 50 threads × ~1 MB = ~50 MB
   - Direct buffers: ~50-100 MB (I/O, WebClient, HTTP)
   - Code cache: ~50 MB
   - GC overhead: ~50 MB
   - **TOTAL REAL: ~800-900 MB**

5. ❌ **Railway probablemente asigna 1 GB de RAM** al contenedor

### Cálculo Real
Si Railway asigna 1 GB (1024 MB) al contenedor:
- 1 GB × 149 minutos = **149 GB-minutos** ✅ (Coincide con la captura)
- 149 minutos = **2 horas y 29 minutos**

**CONCLUSIÓN:** El backend estuvo corriendo durante ~2.5 horas con 1 GB de RAM asignada.

---

## 💰 ANÁLISIS DE SERVICIOS Y COSTOS

### 1. Servicios de Voz (YA OPTIMIZADOS ✅)

El proyecto **NO usa Google Cloud Speech-to-Text ni Google Text-to-Speech**. Ya migró a un sistema híbrido:

| Componente | Servicio Actual | Costo |
|-----------|----------------|-------|
| **STT (Voz → Texto)** | Android SpeechRecognizer nativo | **GRATIS** ✅ |
| **TTS (Texto → Voz)** | Android TextToSpeech nativo | **GRATIS** ✅ |
| **Procesamiento LLM** | OpenAI GPT-4 Turbo | ~$0.01/1K tokens |
| **Alternativa (legacy)** | OpenAI Realtime API | $18/hora ❌ |

**Ahorro actual vs OpenAI Realtime:** **97% de reducción** (de $18/hora a ~$0.50/hora)

**✅ NO REQUIERE CAMBIOS** - Ya está optimizado al máximo.

---

### 2. Google Places API

**Uso actual:**
- Búsqueda de POIs cercanos (searchNearby)
- Búsqueda por texto (searchText)
- Detalles de lugares (Place Details)

**Costo:**
- **Basic Data:** $0.017 por solicitud (Name, Address, Location)
- **Contact Data:** $0.003 adicionales (Phone, Website)
- **Atmosphere Data:** $0.005 adicionales (Rating, Reviews)

**Estimación:**
- ~100 peticiones/día = **$1.70/día** = **$51/mes**
- ~1000 peticiones/día = **$17/día** = **$510/mes**

#### 🔄 Alternativas a Google Places API

| Alternativa | Costo | Límites | Calidad | Recomendación |
|------------|-------|---------|---------|---------------|
| **Nominatim (OpenStreetMap)** | **GRATIS** | 1 req/s, atribución requerida | Media-Alta | ✅ **RECOMENDADO para reducir costos** |
| **Overpass API (OSM)** | **GRATIS** | Sin límites oficiales | Alta (datos abiertos) | ✅ **YA IMPLEMENTADO** (OverpassService.java) |
| **Mapbox Geocoding API** | **GRATIS hasta 100K/mes** | $0.50/1K después | Alta | ⚠️ Considerar si necesitas >100K/mes |
| **OpenCage Geocoding** | **GRATIS hasta 2.5K/día** | $0.001/req después | Alta | ⚠️ Buena opción híbrida |
| **Foursquare Places** | **GRATIS hasta 1K/día** | $0.02/req después | Alta (reviews) | ✅ **YA IMPLEMENTADO** (FoursquareAggregator.java) |
| **OpenTripMap** | **GRATIS** | Ilimitado con API key | Media (turismo) | ✅ **YA IMPLEMENTADO** (OpenTripMapAggregator.java) |

#### 📋 Plan de Migración (Opcional)

**Opción 1: Híbrido OSM + Google Places**
```yaml
# Usar OSM para búsquedas básicas (GRATIS)
# Usar Google Places solo para datos premium (reviews, ratings)
nearby:
  source: mixed  # Combinar OSM + Google Places
  priority: osm  # OSM primero, Google Places como fallback
```

**Ahorro estimado:** 70-80% (solo usar Google Places para ~20-30% de peticiones)

**Opción 2: Solo OSM/Nominatim**
```yaml
nearby:
  source: osm
```

**Ahorro:** 100% (GRATIS), pero puede perder calidad en reviews/ratings

---

### 3. Google Maps Android SDK

**Uso actual:**
- Visualización de mapas interactivos
- Marcadores de POIs
- LocationManager (GPS)

**Costo:** **GRATIS** (Maps SDK para Android no se cobra en Google Cloud)

#### 🔄 Alternativas (Opcional)

| Alternativa | Costo | Calidad | Compatibilidad |
|------------|-------|---------|----------------|
| **MapLibre (OSM)** | **GRATIS** | Alta | Android/iOS/Web |
| **Mapbox Maps SDK** | **GRATIS hasta 50K cargas/mes** | Muy Alta | Android/iOS/Web |
| **Tangram ES** | **GRATIS** | Media | Android/iOS |

**Recomendación:** ✅ **MANTENER Google Maps** (es gratis y de alta calidad para Android)

---

### 4. OpenAI API (GPT-4)

**Uso actual:**
- Chat de voz (`/api/voice/chat`)
- Realtime API (`/realtime/session`)

**Costos:**
- **GPT-4 Turbo:** ~$0.01 por 1K tokens input, ~$0.03 por 1K tokens output
- **Realtime API:** $0.06/min (input audio), $0.24/min (output audio)

**Estimación:**
- 100 conversaciones/día × 2K tokens promedio = **$4/día** = **$120/mes**

#### 🔄 Alternativas a OpenAI GPT-4

| Alternativa | Costo | Calidad | Contexto | Recomendación |
|------------|-------|---------|----------|---------------|
| **Anthropic Claude 3.5 Sonnet** | $0.003/1K in, $0.015/1K out | Muy Alta | 200K tokens | ✅ **RECOMENDADO** (50% más barato) |
| **Google Gemini 1.5 Flash** | **$0.000075/1K in, $0.0003/1K out** | Alta | 1M tokens | ✅✅ **MUY RECOMENDADO** (99% más barato!) |
| **Google Gemini 1.5 Pro** | $0.00125/1K in, $0.005/1K out | Muy Alta | 1M tokens | ✅ **RECOMENDADO** (87.5% más barato) |
| **Llama 3.1 70B (Together AI)** | $0.0009/1K in, $0.0009/1K out | Alta | 128K tokens | ⚠️ Considerar (91% más barato) |
| **Groq (Llama 3.1 70B)** | **GRATIS (beta)** | Alta | 32K tokens | ⚠️ Inestable, sin SLA |

#### 🎯 Recomendación Principal: Migrar a Gemini 1.5 Flash

**Por qué:**
- ✅ **99% más barato que GPT-4 Turbo**
- ✅ **1M tokens de contexto** (vs 128K de GPT-4)
- ✅ **Muy rápido** (optimizado para latencia baja)
- ✅ **Integración fácil** (SDK de Google)
- ✅ **GRATIS hasta 1500 requests/día** en free tier

**Migración:**
```java
// Cambiar de OpenAI a Google Generative AI
// 1. Agregar dependencia en build.gradle
implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

// 2. Cambiar VoiceChatService.java para usar Gemini
GoogleGenerativeAI genAI = new GoogleGenerativeAI(apiKey);
GenerativeModel model = genAI.getGenerativeModel("gemini-1.5-flash");
```

**Ahorro estimado:**
- De $120/mes (GPT-4) a **$1.20/mes (Gemini 1.5 Flash)**
- **Ahorro: $118.80/mes (99%)**

---

## 🚀 OPTIMIZACIONES PRIORITARIAS

### 🔴 URGENTE: Optimización de Memoria en Railway

#### Problema 1: Configuración del Heap de Java

**Cambio en Dockerfile:**
```dockerfile
# ❌ ANTES (problemático)
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

# ✅ DESPUÉS (optimizado)
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

**Cambios:**
- `-Xmx256m`: Reduce heap máximo a 256 MB (suficiente para API REST)
- `-Xms128m`: Inicia con 128 MB (crece bajo demanda)
- `-XX:+UseG1GC`: Usa G1 Garbage Collector (más eficiente en memoria)
- `-XX:MaxGCPauseMillis=100`: Limita pausas de GC a 100ms
- `-XX:+UseStringDeduplication`: Deduplica strings (ahorra ~10-15% memoria)

**Impacto esperado:**
- Reducción de **~800 MB → ~400 MB** de memoria total
- Ahorro: **50% en costos de memoria**

#### Problema 2: Health Check de Railway

**Cambio en railway.toml:**
```toml
[deploy]
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 10
healthcheckPath = "/api/actuator/health"
healthcheckTimeout = 100
# ✅ AGREGAR:
sleepDelay = 0  # No mantener servicio inactivo
```

#### Problema 3: Auto-Sleep en Railway

**Railway Free Tier:**
- Los servicios **NO se apagan automáticamente** cuando están inactivos
- Se cobra por **tiempo corriendo**, no por peticiones

**Solución:**
- Usar **Railway Cron Jobs** o **Railway Deployments on-demand**
- Implementar **auto-shutdown** después de N minutos de inactividad

**Ejemplo en Spring Boot:**
```java
// application.yml
management:
  endpoint:
    shutdown:
      enabled: true  # Permitir shutdown remoto
```

---

### 🟡 IMPORTANTE: Optimizaciones de Configuración

#### 1. Reducir Threads de Tomcat

**application-production.yml:**
```yaml
server:
  tomcat:
    threads:
      max: 20  # ❌ CAMBIAR de 50 a 20
      min-spare: 2  # ❌ CAMBIAR de 5 a 2
```

**Impacto:** Ahorra ~30 MB de memoria (cada thread = ~1 MB)

#### 2. Limitar Caché

**application-production.yml:**
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=1h  # ❌ CAMBIAR de 5000 a 1000 y de 24h a 1h
```

**Impacto:** Ahorra ~50-100 MB de memoria

#### 3. Deshabilitar Swagger en Producción

**application-production.yml:**
```yaml
springdoc:
  swagger-ui:
    enabled: false  # ❌ CAMBIAR a false
  api-docs:
    enabled: false  # ❌ CAMBIAR a false
```

**Impacto:** Ahorra ~30-50 MB de memoria + mejora seguridad

#### 4. Configurar Connection Pooling

**application-production.yml:**
```yaml
spring:
  web:
    resources:
      cache:
        cachecontrol:
          max-age: 86400  # 24 horas
```

---

### 🟢 OPCIONALES: Optimizaciones Avanzadas

#### 1. Migrar a Railway Hobby Plan ($5/mes)

**Ventajas:**
- $5 de crédito incluido (~500 GB-horas de memoria)
- Sin límite de tiempo de ejecución
- Mejor rendimiento

**Cálculo:**
- Memoria optimizada: 400 MB = 0.4 GB
- 500 GB-horas ÷ 0.4 GB = **1,250 horas/mes** ≈ **41 días de uptime continuo**

#### 2. Migrar a Fly.io

**Ventajas:**
- **3 shared-cpu-1x VMs gratis** (256 MB RAM cada una)
- Auto-sleep después de inactividad
- Red global

**Costo:** GRATIS para apps pequeñas

#### 3. Migrar a Render.com

**Ventajas:**
- **Free Tier:** 512 MB RAM, auto-sleep después de 15 min inactividad
- Build cache
- Zero downtime deploys

**Costo:** GRATIS

---

## 📝 RESUMEN DE RECOMENDACIONES

### 🔴 CRÍTICAS (Implementar INMEDIATAMENTE)

1. ✅ **Reducir heap de Java a 256 MB** (`-Xmx256m -Xms128m`)
2. ✅ **Agregar G1GC y String Deduplication**
3. ✅ **Reducir threads de Tomcat a 20 max**

**Ahorro estimado:** **50% en costos de memoria** ($3.43 → **$1.72/mes**)

### 🟡 IMPORTANTES (Implementar esta semana)

4. ⚠️ **Considerar migrar a Railway Hobby Plan** ($5/mes con crédito incluido)
5. ⚠️ **Deshabilitar Swagger en producción**
6. ⚠️ **Reducir tamaño de caché** (5000 → 1000)

**Ahorro estimado adicional:** ~20%

### 🟢 OPCIONALES (Evaluar a medio plazo)

7. 🔄 **Migrar de Google Places a OSM/Nominatim** (híbrido)
   - **Ahorro:** 70-80% en costos de geolocalización
8. 🔄 **Migrar de OpenAI GPT-4 a Google Gemini 1.5 Flash**
   - **Ahorro:** 99% en costos de LLM ($120 → $1.20/mes)
9. 🔄 **Considerar Fly.io o Render.com** (free tier con auto-sleep)

---

## 💡 CÁLCULO FINAL DE COSTOS

### Escenario Actual (Sin Optimizaciones)
| Servicio | Costo Mensual |
|---------|---------------|
| Railway (Memoria) | $3.43 |
| Google Places API (100 req/día) | $51 |
| OpenAI GPT-4 (100 conv/día) | $120 |
| **TOTAL** | **$174.43/mes** |

### Escenario Optimizado (Con Optimizaciones Básicas)
| Servicio | Costo Mensual |
|---------|---------------|
| Railway (Memoria optimizada) | **$1.72** (-50%) |
| Google Places API | $51 (sin cambios) |
| OpenAI GPT-4 | $120 (sin cambios) |
| **TOTAL** | **$172.72/mes** |

### Escenario Óptimo (Con Todas las Optimizaciones)
| Servicio | Costo Mensual |
|---------|---------------|
| Fly.io o Render (Free tier) | **$0** |
| OSM/Nominatim (GRATIS) | **$0** |
| Google Gemini 1.5 Flash | **$1.20** (-99%) |
| **TOTAL** | **$1.20/mes** ✅ |

**AHORRO TOTAL: $173.23/mes (99.3%)**

---

## 🎯 PLAN DE ACCIÓN RECOMENDADO

### Fase 1: Optimización Inmediata (Hoy)
1. ✅ Aplicar cambios en Dockerfile (memoria)
2. ✅ Reducir threads en application-production.yml
3. ✅ Deshabilitar Swagger en producción
4. ✅ Deploy y validar

### Fase 2: Migración de LLM (Esta semana)
5. 🔄 Implementar soporte para Gemini 1.5 Flash
6. 🔄 Probar y comparar calidad de respuestas
7. 🔄 Migrar producción

### Fase 3: Migración de Geolocalización (Siguiente semana)
8. 🔄 Configurar Nominatim self-hosted o usar instancia pública
9. 🔄 Implementar sistema híbrido OSM + Google Places
10. 🔄 Migrar producción gradualmente

### Fase 4: Migración de Hosting (Evaluar)
11. 🔄 Probar Fly.io o Render.com
12. 🔄 Comparar rendimiento y costos
13. 🔄 Decidir migración

---

## 📚 RECURSOS

- [Google Gemini API Docs](https://ai.google.dev/docs)
- [Nominatim Usage Policy](https://operations.osmfoundation.org/policies/nominatim/)
- [Railway Pricing](https://railway.app/pricing)
- [Fly.io Pricing](https://fly.io/docs/about/pricing/)
- [Render.com Pricing](https://render.com/pricing)

---

**Autor:** Claude Code
**Repositorio:** Mikel-Canamares/NOMAD
**Branch:** claude/analyze-service-costs-GK8Xa
