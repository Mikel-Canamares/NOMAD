package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);
    private static final int FALLBACK_THRESHOLD = 3;

    private final GooglePlacesClient placesClient;

    public GooglePlacesService(GooglePlacesClient placesClient) {
        this.placesClient = placesClient;
    }

    public List<JsonNode> queryPois(double lat, double lng, double radiusMeters, String category, int limit, String languageCode, String regionCode) {
        long startTime = System.currentTimeMillis();
        CategoryMapping mapping = mapCategoryToPlacesTypes(category);

        log.debug("Google Places query: lat={}, lng={}, radius={}, category={}, types={}, lang={}, region={}",
                lat, lng, radiusMeters, category, mapping.includedTypes, languageCode, regionCode);

        // 1. Nearby Search (principal)
        List<JsonNode> nearbyResults = placesClient.searchNearby(
                lat,
                lng,
                radiusMeters,
                mapping.includedTypes,
                List.of("lodging"), // Siempre excluir hoteles
                Math.min(20, limit),
                languageCode,
                regionCode
        );

        int countNearby = nearbyResults.size();
        int countText = 0;
        List<JsonNode> finalResults = new ArrayList<>(nearbyResults);

        // 2. Text Search fallback si < 3 resultados
        if (nearbyResults.size() < FALLBACK_THRESHOLD && mapping.textQuery != null) {
            log.debug("Nearby returned only {} results, trying Text Search fallback", nearbyResults.size());

            List<JsonNode> textResults = placesClient.searchText(
                    lat,
                    lng,
                    radiusMeters,
                    mapping.textQuery,
                    mapping.fallbackType,
                    10,
                    languageCode,
                    regionCode
            );

            countText = textResults.size();

            // Fusionar evitando duplicados por place.id
            Set<String> existingIds = nearbyResults.stream()
                    .map(node -> node.get("id").asText())
                    .collect(Collectors.toSet());

            for (JsonNode textResult : textResults) {
                String id = textResult.get("id").asText();
                if (!existingIds.contains(id)) {
                    finalResults.add(textResult);
                    existingIds.add(id);
                }
            }
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("Google Places: cat={}, api={}+{}, countNearby={}, countText={}, total={}, lang={}, region={}, ms={}",
                category, "Nearby", (countText > 0 ? "TextSearch" : ""),
                countNearby, countText, finalResults.size(), languageCode, regionCode, elapsedMs);

        return finalResults.stream().limit(limit).collect(Collectors.toList());
    }

    private CategoryMapping mapCategoryToPlacesTypes(String category) {
        if (category == null) {
            return new CategoryMapping(
                    List.of("tourist_attraction", "restaurant", "museum", "park"),
                    null,
                    null
            );
        }

        return switch (category.toLowerCase()) {
            case "restaurant" -> new CategoryMapping(
                    List.of("restaurant", "cafe"),
                    null,
                    null
            );
            case "museum" -> new CategoryMapping(
                    List.of("museum", "art_gallery"),
                    null,
                    null
            );
            case "park" -> new CategoryMapping(
                    List.of("park", "botanical_garden", "national_park", "state_park"),
                    null,
                    null
            );
            case "monument" -> new CategoryMapping(
                    List.of("monument", "historical_landmark", "cultural_landmark", "historical_place", "sculpture"),
                    "monument OR memorial OR monumento",
                    "tourist_attraction"
            );
            case "viewpoint" -> new CategoryMapping(
                    List.of("observation_deck", "tourist_attraction"),
                    "viewpoint OR mirador OR scenic overlook",
                    "tourist_attraction"
            );
            case "heritage" -> new CategoryMapping(
                    List.of("historical_place", "historical_landmark", "cultural_landmark", "museum"),
                    "heritage site OR sitio histórico OR patrimonio",
                    "historical_landmark"
            );
            default -> new CategoryMapping(
                    List.of("tourist_attraction"),
                    null,
                    null
            );
        };
    }

    private record CategoryMapping(List<String> includedTypes, String textQuery, String fallbackType) {}
}
