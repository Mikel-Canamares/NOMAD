# Endpoint /ask - Agregador de Información

## Resumen

El endpoint `/ask` agrega información de múltiples fuentes en cascada:

1. **OSM** - Datos base, etiquetas wikidata/wikipedia
2. **Wikidata** - Sitio oficial, imagen, año, estilo, patrimonio
3. **Wikipedia** - Extracto en locale solicitado (fallback inglés)
4. **OpenTripMap** - Descripción breve y categorías
5. **Foursquare** - Rating, reviews, website, dirección, horarios
6. **Google Places** - Website, rating, reviews, horarios, dirección, teléfono

## Request

**Endpoint:** `POST /api/ask?lat={lat}&lng={lng}`

**Body:**
```json
{
  "text": "Museo del Prado",
  "locale": "es",
  "poiId": "node/123456"
}
```

**Parámetros:**
- `text` (required): Nombre del POI o consulta
- `locale` (optional): Código de idioma (es, en, fr, etc.)
- `poiId` (optional): ID del POI desde /poi/nearby
- `lat` (query param): Latitud (required si no hay poiId)
- `lng` (query param): Longitud (required si no hay poiId)

## Response

```json
{
  "markdown": "# Museo del Prado\n\nEl Museo del Prado es...",
  "facts": {
    "name": "Museo del Prado",
    "official_site": "https://www.museodelprado.es",
    "address": "Paseo del Prado, s/n, 28014 Madrid",
    "phone": "+34 91 330 2800",
    "opening_hours": "Mo-Sa 10:00-20:00; Su 10:00-19:00",
    "rating": 4.7,
    "user_ratings_total": 45320,
    "inception_year": "1819",
    "image_url": "http://commons.wikimedia.org/wiki/Special:FilePath/...",
    "style": "Neoclassical",
    "heritage": "Bien de Interés Cultural",
    "wikipedia_extract": "El Museo del Prado es..."
  },
  "sources": [
    {"title": "OpenStreetMap", "url": "https://www.openstreetmap.org/"},
    {"title": "Wikidata", "url": "https://www.wikidata.org/wiki/Q160112"},
    {"title": "Wikipedia", "url": "https://es.wikipedia.org/wiki/Museo_del_Prado"}
  ],
  "used_sources": ["OSM", "Wikidata", "Wikipedia", "GooglePlaces"]
}
```

## Caché

- **Duración:** 30 minutos
- **Clave:** `poiId:locale` o `geohash5:nombre:locale`

## Configuración API Keys

Las APIs de terceros requieren keys. Configura variables de entorno:

```bash
export OPENTRIPMAP_API_KEY=your_key_here
export FOURSQUARE_API_KEY=your_key_here
export GOOGLE_PLACES_API_KEY=your_key_here
```

O en `application.yml`:

```yaml
api:
  opentripmap:
    key: your_opentripmap_key
  foursquare:
    key: your_foursquare_key
  google:
    places:
      key: your_google_key
```

**Sin keys:** Las fuentes OSM, Wikidata y Wikipedia funcionan sin configuración. Las demás se saltarán con un warning en los logs.

## Ejemplo de prueba (sin keys)

```bash
curl -X POST "http://localhost:8081/api/ask?lat=40.4138&lng=-3.6921" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Museo del Prado",
    "locale": "es"
  }'
```

Esto devolverá información agregada de OSM, Wikidata y Wikipedia.

## Logs

Los logs muestran claramente qué fuentes se consultaron:

```
INFO: Source: Looking up POI from cache with id=node/123456
INFO: Fetching OSM data: lat=40.4138, lng=-3.6921, radius=100
INFO: OSM data extracted: 5 fields
INFO: Source: Wikidata with QID=Q160112
INFO: Extracted 6 Wikidata claims
INFO: Source: Wikipedia with title=Museo_del_Prado
INFO: Wikipedia extract found: 856 chars
WARN: Foursquare API key not configured, skipping
WARN: Google Places API key not configured, skipping
INFO: Aggregation complete: 3 sources used, 12 facts collected
```

## Manejo de errores

Si no hay datos suficientes, devuelve 200 OK con respuesta mínima:

```json
{
  "markdown": "No detailed information available for this location.",
  "facts": {},
  "sources": [],
  "used_sources": []
}
```
