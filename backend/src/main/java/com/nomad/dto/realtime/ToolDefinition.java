package com.nomad.dto.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ToolDefinition(
    String type,
    String name,
    String description,
    Map<String, Object> parameters
) {
    public static ToolDefinition poiNearby() {
        return new ToolDefinition(
            "function",
            "poi_nearby",
            "Search for points of interest (POI) near given coordinates. Returns monuments, museums, viewpoints, restaurants from OpenStreetMap.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "lat", Map.of(
                        "type", "number",
                        "description", "Latitude of the location"
                    ),
                    "lng", Map.of(
                        "type", "number",
                        "description", "Longitude of the location"
                    ),
                    "radius", Map.of(
                        "type", "number",
                        "description", "Search radius in meters (e.g., 500, 1000)"
                    ),
                    "cat", Map.of(
                        "type", "string",
                        "description", "Category filter: monument, museum, viewpoint, restaurant",
                        "enum", new String[]{"monument", "museum", "viewpoint", "restaurant"}
                    )
                ),
                "required", new String[]{"lat", "lng", "radius"}
            )
        );
    }

    public static ToolDefinition poiContext() {
        return new ToolDefinition(
            "function",
            "poi_context",
            "Get detailed information about a point of interest from multiple sources (OSM, Wikidata, Wikipedia, Google Places, etc.). Use when user asks for details about a specific place.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "poiId", Map.of(
                        "type", "string",
                        "description", "POI identifier from poi_nearby (e.g., 'node/123456')"
                    ),
                    "name", Map.of(
                        "type", "string",
                        "description", "Name of the POI (required if poiId not provided)"
                    ),
                    "lat", Map.of(
                        "type", "number",
                        "description", "Latitude (required if poiId not provided)"
                    ),
                    "lng", Map.of(
                        "type", "number",
                        "description", "Longitude (required if poiId not provided)"
                    ),
                    "locale", Map.of(
                        "type", "string",
                        "description", "Language code for response (e.g., 'es', 'en', 'fr')",
                        "default", "en"
                    )
                ),
                "required", new String[]{}
            )
        );
    }
}
