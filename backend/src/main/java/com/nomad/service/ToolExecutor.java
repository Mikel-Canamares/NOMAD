package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nomad.dto.AskRequest;
import com.nomad.dto.AskResponse;
import com.nomad.dto.PoiResponse;
import com.nomad.dto.realtime.ToolCall;
import com.nomad.dto.realtime.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final PoiService poiService;
    private final AskService askService;
    private final ObjectMapper objectMapper;

    @Value("${googleplaces.language-code}")
    private String defaultLanguageCode;

    @Value("${googleplaces.region-code}")
    private String defaultRegionCode;

    public ToolExecutor(PoiService poiService, AskService askService, ObjectMapper objectMapper) {
        this.poiService = poiService;
        this.askService = askService;
        this.objectMapper = objectMapper;
    }

    public ToolResponse execute(ToolCall toolCall) {
        log.info("Executing tool: {} with id: {}", toolCall.name(), toolCall.id());

        try {
            String result = switch (toolCall.name()) {
                case "poi_nearby" -> executePoiNearby(toolCall.arguments());
                case "poi_context" -> executePoiContext(toolCall.arguments());
                default -> {
                    log.warn("Unknown tool: {}", toolCall.name());
                    yield "{\"error\":\"Unknown tool: " + toolCall.name() + "\"}";
                }
            };

            log.info("Tool {} executed successfully, result length: {}", toolCall.name(), result.length());
            return new ToolResponse(toolCall.id(), result);

        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolCall.name(), e.getMessage(), e);
            String errorResult = String.format("{\"error\":\"%s\"}",
                e.getMessage().replace("\"", "\\\""));
            return new ToolResponse(toolCall.id(), errorResult);
        }
    }

    private String executePoiNearby(JsonNode arguments) throws Exception {
        double lat = arguments.get("lat").asDouble();
        double lng = arguments.get("lng").asDouble();
        double radius = arguments.get("radius").asDouble();
        String cat = arguments.has("cat") ? arguments.get("cat").asText() : null;
        int limit = arguments.has("limit") ? arguments.get("limit").asInt() : 25;
        String locale = arguments.has("locale") ? arguments.get("locale").asText() : null;
        String region = arguments.has("region") ? arguments.get("region").asText() : null;

        // Normalizar locale/region
        String[] normalized = normalizeLocaleAndRegion(locale, region);
        String languageCode = normalized[0];
        String regionCode = normalized[1];

        log.info("Executing poi_nearby: lat={}, lng={}, radius={}, cat={}, limit={}, lang={}, region={}",
                lat, lng, radius, cat, limit, languageCode, regionCode);

        List<PoiResponse> pois = poiService.getNearbyPois(lat, lng, radius, cat, limit, languageCode, regionCode);

        // Sanitize response - remove any internal details
        String jsonResult = objectMapper.writeValueAsString(pois);

        log.info("poi_nearby returned {} POIs", pois.size());
        return jsonResult;
    }

    private String[] normalizeLocaleAndRegion(String locale, String region) {
        String languageCode = defaultLanguageCode;
        String regionCode = defaultRegionCode;

        if (locale != null && !locale.isBlank()) {
            // Parse "es-ES" → lang="es", region="ES"
            if (locale.contains("-")) {
                String[] parts = locale.split("-");
                languageCode = parts[0].toLowerCase();
                if (region == null && parts.length > 1) {
                    regionCode = parts[1].toUpperCase();
                }
            } else {
                languageCode = locale.toLowerCase();
            }
        }

        if (region != null && !region.isBlank()) {
            regionCode = region.toUpperCase();
        }

        return new String[]{languageCode, regionCode};
    }

    private String executePoiContext(JsonNode arguments) throws Exception {
        String poiId = arguments.has("poiId") ? arguments.get("poiId").asText() : null;
        String name = arguments.has("name") ? arguments.get("name").asText() : null;
        String locale = arguments.has("locale") ? arguments.get("locale").asText() : "en";
        Double lat = arguments.has("lat") ? arguments.get("lat").asDouble() : null;
        Double lng = arguments.has("lng") ? arguments.get("lng").asDouble() : null;

        log.info("Executing poi_context: poiId={}, name={}, locale={}, lat={}, lng={}",
            poiId, name, locale, lat, lng);

        if (name == null || name.isBlank()) {
            return "{\"error\":\"Missing required parameter: name\"}";
        }

        if (lat == null || lng == null) {
            return "{\"error\":\"Missing required parameters: lat and lng\"}";
        }

        AskRequest request = new AskRequest(name, locale, poiId);
        AskResponse response = askService.aggregateInformation(request, lat, lng);

        // No need to sanitize - just use the response directly
        String jsonResult = objectMapper.writeValueAsString(response);

        log.info("poi_context returned {} facts from {} sources",
            response.facts().size(), response.used_sources().size());
        return jsonResult;
    }
}
