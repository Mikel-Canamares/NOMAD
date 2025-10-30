# Herramientas (Tools) para OpenAI Realtime API

## Resumen

El backend define 2 herramientas que OpenAI Realtime puede invocar automáticamente durante conversaciones:

1. **poi_nearby** - Buscar POIs cercanos a coordenadas
2. **poi_context** - Obtener información detallada de un POI

Estas herramientas están **automáticamente incluidas** en las sesiones Realtime creadas por `/realtime/session`.

## Definición de Tools

### 1. poi_nearby

**Descripción:** Busca puntos de interés cerca de coordenadas dadas.

**Parámetros:**
```json
{
  "lat": 40.4179,           // Latitud (required)
  "lng": -3.7142,           // Longitud (required)
  "radius": 500,            // Radio en metros (required)
  "cat": "museum"           // Categoría: monument|museum|viewpoint|restaurant (optional)
}
```

**Respuesta:**
```json
[
  {
    "id": "node/123456",
    "name": "Museo del Prado",
    "category": "museum",
    "lat": 40.4138,
    "lng": -3.6921,
    "source": "OSM",
    "license": "ODbL",
    "relevance": 0.95
  },
  ...
]
```

### 2. poi_context

**Descripción:** Obtiene información agregada de múltiples fuentes sobre un POI específico.

**Parámetros:**
```json
{
  "poiId": "node/123456",   // ID del POI (optional)
  "name": "Museo del Prado", // Nombre del POI (required si no hay poiId)
  "lat": 40.4138,           // Latitud (required)
  "lng": -3.6921,           // Longitud (required)
  "locale": "es"            // Idioma: es|en|fr|... (optional, default: en)
}
```

**Respuesta:**
```json
{
  "markdown": "# Museo del Prado\n\nEl Museo del Prado es...",
  "facts": {
    "name": "Museo del Prado",
    "official_site": "https://www.museodelprado.es",
    "address": "Paseo del Prado s/n",
    "phone": "+34 91 330 2800",
    "opening_hours": "Mo-Sa 10:00-20:00",
    "rating": 4.7,
    "user_ratings_total": 45320,
    "inception_year": "1819"
  },
  "sources": [
    {"title": "OpenStreetMap", "url": "https://www.openstreetmap.org/"},
    {"title": "Wikidata", "url": "https://www.wikidata.org/wiki/Q160112"}
  ],
  "used_sources": ["OSM", "Wikidata", "Wikipedia", "GooglePlaces"]
}
```

## Endpoint para Tool Execution

**URL:** `POST /api/realtime/tool-call`

**Body:**
```json
{
  "id": "call_abc123",       // Tool call ID from OpenAI
  "name": "poi_nearby",      // Tool name
  "arguments": {             // Tool arguments (JSON)
    "lat": 40.4179,
    "lng": -3.7142,
    "radius": 500
  }
}
```

**Response:**
```json
{
  "id": "call_abc123",       // Same ID
  "result": "[...]"          // JSON string with results
}
```

## Seguridad

### ✅ Datos Sanitizados

- **NO se exponen** API keys en las respuestas
- **NO se exponen** detalles internos del servidor
- **NO se exponen** rutas de archivos o configuraciones
- Solo se devuelven datos públicos de fuentes oficiales (OSM, Wikidata, Wikipedia, Google Places)

### Validaciones

- Parámetros requeridos validados
- Tipos de datos verificados
- Errores capturados y sanitizados
- Logs claros sin información sensible

## Ejemplo de Uso en Realtime

**1. Usuario pregunta:** "¿Qué museos hay cerca del Palacio Real de Madrid?"

**2. OpenAI Realtime invoca automáticamente:**
```json
{
  "type": "function_call",
  "function": {
    "name": "poi_nearby",
    "arguments": {
      "lat": 40.4179,
      "lng": -3.7142,
      "radius": 1000,
      "cat": "museum"
    }
  }
}
```

**3. Backend ejecuta** y devuelve lista de museos

**4. OpenAI usa** los datos para responder al usuario:
> "Cerca del Palacio Real encontré estos museos: Museo Arqueológico Nacional, Real Armería, Real Cocina de Palacio..."

**5. Usuario pregunta:** "Cuéntame más sobre la Real Armería"

**6. OpenAI invoca:**
```json
{
  "type": "function_call",
  "function": {
    "name": "poi_context",
    "arguments": {
      "name": "Real Armería",
      "lat": 40.4168,
      "lng": -3.7152,
      "locale": "es"
    }
  }
}
```

**7. Backend agrega** información de múltiples fuentes

**8. OpenAI responde** con contexto rico citando fuentes

## Logs

Los logs muestran claramente la ejecución:

```
INFO: Executing tool: poi_nearby with id: call_abc123
INFO: poi_nearby returned 15 POIs
INFO: Executing tool: poi_context with id: call_def456
INFO: Fetching OSM data: lat=40.4168, lng=-3.7152, radius=100
INFO: OSM data extracted: 3 fields
INFO: Fetching Google Places data: name=Real Armería, lat=40.4168, lng=-3.7152
INFO: Google Places details extracted: 5 fields
INFO: Aggregation complete: 2 sources used, 8 facts collected
INFO: poi_context returned 8 facts from 2 sources
```

## Testing Manual

```bash
# Test poi_nearby
curl -X POST "http://localhost:8081/api/realtime/tool-call" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test_123",
    "name": "poi_nearby",
    "arguments": {"lat": 40.4179, "lng": -3.7142, "radius": 500}
  }'

# Test poi_context
curl -X POST "http://localhost:8081/api/realtime/tool-call" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test_456",
    "name": "poi_context",
    "arguments": {"name": "Palacio Real", "lat": 40.4179, "lng": -3.7142, "locale": "es"}
  }'
```

## Notas

- Las herramientas se ejecutan **en el servidor backend**, no en el cliente
- OpenAI Realtime decide **automáticamente** cuándo invocarlas basándose en la conversación
- Las respuestas son **JSON serializadas como strings** para compatibilidad con Realtime API
- **Sin límite** de invocaciones (respeta rate limits de APIs externas)
