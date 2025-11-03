package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.PoiResponse;
import com.nomad.util.GeoHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PoiService {

    private final OverpassService overpassService;
    private final GooglePlacesService googlePlacesService;
    private final String nearbySource;

    public PoiService(
            OverpassService overpassService,
            GooglePlacesService googlePlacesService,
            @Value("${nearby.source:google_places}") String nearbySource
    ) {
        this.overpassService = overpassService;
        this.googlePlacesService = googlePlacesService;
        this.nearbySource = nearbySource;
    }

    @Cacheable(value = "poiCache", key = "T(com.nomad.util.GeoHashUtil).generateCacheKey(#lat, #lng, #category) + '_' + #radius + '_' + #limit + '_' + #languageCode + '_' + #regionCode")
    public List<PoiResponse> getNearbyPois(double lat, double lng, double radius, String category, int limit, String languageCode, String regionCode) {
        List<JsonNode> elements;

        // Rutear según configuración
        if ("google_places".equals(nearbySource)) {
            elements = googlePlacesService.queryPois(lat, lng, radius, category, limit, languageCode, regionCode);
            return elements.stream()
                    .map(element -> mapGooglePlaceToPoiResponse(element, lat, lng, category))
                    .filter(poi -> poi != null)
                    .sorted(Comparator.comparingDouble(poi ->
                            haversineDistance(lat, lng, poi.lat(), poi.lng())))
                    .collect(Collectors.toList());
        } else if ("osm".equals(nearbySource)) {
            elements = overpassService.queryPois(lat, lng, radius, category);
            return elements.stream()
                    .map(element -> mapOsmToPoiResponse(element, lat, lng))
                    .filter(poi -> poi != null)
                    .sorted(Comparator.comparingDouble(poi ->
                            haversineDistance(lat, lng, poi.lat(), poi.lng())))
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            // mixed o por defecto: usar Google Places
            elements = googlePlacesService.queryPois(lat, lng, radius, category, limit, languageCode, regionCode);
            return elements.stream()
                    .map(element -> mapGooglePlaceToPoiResponse(element, lat, lng, category))
                    .filter(poi -> poi != null)
                    .sorted(Comparator.comparingDouble(poi ->
                            haversineDistance(lat, lng, poi.lat(), poi.lng())))
                    .collect(Collectors.toList());
        }
    }

    private PoiResponse mapGooglePlaceToPoiResponse(JsonNode place, double userLat, double userLng, String requestedCategory) {
        try {
            String id = "gplaces:" + place.get("id").asText();
            String name = place.has("displayName") && place.get("displayName").has("text")
                    ? place.get("displayName").get("text").asText()
                    : "Unknown";

            JsonNode location = place.get("location");
            double poiLat = location.get("latitude").asDouble();
            double poiLng = location.get("longitude").asDouble();

            // Usar la categoría solicitada o inferir del primaryType
            String category = requestedCategory != null ? requestedCategory : "other";

            double distance = haversineDistance(userLat, userLng, poiLat, poiLng);
            double relevance = calculateRelevance(distance);

            return new PoiResponse(id, name, category, poiLat, poiLng, "GooglePlaces", "GooglePlaces", relevance);

        } catch (Exception e) {
            return null;
        }
    }

    private PoiResponse mapOsmToPoiResponse(JsonNode element, double userLat, double userLng) {
        try {
            String type = element.get("type").asText();
            String id = type + "/" + element.get("id").asText();

            JsonNode tags = element.get("tags");
            if (tags == null) return null;

            String name = tags.has("name") ? tags.get("name").asText() : "Unknown";

            double poiLat = element.has("lat") ? element.get("lat").asDouble() :
                    element.has("center") ? element.get("center").get("lat").asDouble() : 0;
            double poiLng = element.has("lon") ? element.get("lon").asDouble() :
                    element.has("center") ? element.get("center").get("lon").asDouble() : 0;

            if (poiLat == 0 || poiLng == 0) return null;

            String category = extractCategoryFromOsm(tags);
            double distance = haversineDistance(userLat, userLng, poiLat, poiLng);
            double relevance = calculateRelevance(distance);

            return new PoiResponse(id, name, category, poiLat, poiLng, "OSM", "ODbL", relevance);

        } catch (Exception e) {
            return null;
        }
    }

    private String extractCategoryFromOsm(JsonNode tags) {
        // Prioridad: historic > tourism > leisure > man_made > amenity
        if (tags.has("historic")) {
            String historic = tags.get("historic").asText();
            return switch (historic) {
                case "monument", "memorial" -> "monument";
                case "castle", "fort", "palace", "city_gate", "archaeological_site", "ruins" -> "heritage";
                default -> historic;
            };
        }
        if (tags.has("tourism")) {
            String tourism = tags.get("tourism").asText();
            return switch (tourism) {
                case "artwork" -> "monument";
                default -> tourism;
            };
        }
        if (tags.has("leisure")) {
            String leisure = tags.get("leisure").asText();
            return switch (leisure) {
                case "park", "garden" -> "park";
                default -> leisure;
            };
        }
        if (tags.has("man_made") && tags.get("man_made").asText().equals("obelisk")) {
            return "monument";
        }
        if (tags.has("amenity")) {
            return tags.get("amenity").asText();
        }
        return "other";
    }

    private double calculateRelevance(double distance) {
        // Relevance = 1 / (distance + 1), normalizado
        return 1.0 / (distance + 1.0);
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
