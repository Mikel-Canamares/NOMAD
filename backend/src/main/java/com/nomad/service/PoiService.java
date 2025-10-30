package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.PoiResponse;
import com.nomad.util.GeoHashUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PoiService {

    private final OverpassService overpassService;

    public PoiService(OverpassService overpassService) {
        this.overpassService = overpassService;
    }

    @Cacheable(value = "poiCache", key = "T(com.nomad.util.GeoHashUtil).generateCacheKey(#lat, #lng, #category) + '_' + #radius")
    public List<PoiResponse> getNearbyPois(double lat, double lng, double radius, String category) {
        List<JsonNode> elements = overpassService.queryPois(lat, lng, radius, category);

        return elements.stream()
            .map(element -> mapToPoiResponse(element, lat, lng))
            .filter(poi -> poi != null)
            .collect(Collectors.toList());
    }

    private PoiResponse mapToPoiResponse(JsonNode element, double userLat, double userLng) {
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

            String category = extractCategory(tags);
            double relevance = calculateRelevance(userLat, userLng, poiLat, poiLng);

            return new PoiResponse(id, name, category, poiLat, poiLng, "OSM", "ODbL", relevance);

        } catch (Exception e) {
            return null;
        }
    }

    private String extractCategory(JsonNode tags) {
        if (tags.has("tourism")) {
            return tags.get("tourism").asText();
        }
        if (tags.has("amenity") && tags.get("amenity").asText().equals("restaurant")) {
            return "restaurant";
        }
        return "other";
    }

    private double calculateRelevance(double lat1, double lng1, double lat2, double lng2) {
        double distance = haversineDistance(lat1, lng1, lat2, lng2);
        return Math.max(0.0, 1.0 - (distance / 10000.0)); // Normalize to 0-1, max 10km
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
