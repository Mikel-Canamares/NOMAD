package com.nomad.service.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class GooglePlacesAggregator {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesAggregator.class);
    private final WebClient webClient;
    private final String apiKey;

    public GooglePlacesAggregator(WebClient.Builder webClientBuilder,
                                   @Value("${api.google.places.key:PLACEHOLDER_GOOGLE_KEY}") String apiKey) {
        this.webClient = webClientBuilder
            .baseUrl("https://maps.googleapis.com/maps/api/place")
            .build();
        this.apiKey = apiKey;
    }

    public Map<String, Object> searchPlace(String name, double lat, double lng) {
        Map<String, Object> data = new HashMap<>();

        if ("PLACEHOLDER_GOOGLE_KEY".equals(apiKey)) {
            log.warn("Google Places API key not configured, skipping");
            return data;
        }

        try {
            log.info("Fetching Google Places data: name={}, lat={}, lng={}", name, lat, lng);

            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/nearbysearch/json")
                    .queryParam("location", lat + "," + lng)
                    .queryParam("radius", "100")
                    .queryParam("keyword", name)
                    .queryParam("key", apiKey)
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode place = response.get("results").get(0);

                if (place.has("place_id")) {
                    String placeId = place.get("place_id").asText();
                    return getPlaceDetails(placeId);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Google Places data: {}", e.getMessage());
        }

        return data;
    }

    private Map<String, Object> getPlaceDetails(String placeId) {
        Map<String, Object> data = new HashMap<>();

        try {
            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "website,rating,user_ratings_total,opening_hours,formatted_address,formatted_phone_number")
                    .queryParam("key", apiKey)
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.has("result")) {
                JsonNode result = response.get("result");

                if (result.has("website")) {
                    data.put("website", result.get("website").asText());
                }

                if (result.has("rating")) {
                    data.put("rating", result.get("rating").asDouble());
                }

                if (result.has("user_ratings_total")) {
                    data.put("user_ratings_total", result.get("user_ratings_total").asInt());
                }

                if (result.has("formatted_address")) {
                    data.put("address", result.get("formatted_address").asText());
                }

                if (result.has("formatted_phone_number")) {
                    data.put("phone", result.get("formatted_phone_number").asText());
                }

                if (result.has("opening_hours") && result.get("opening_hours").has("weekday_text")) {
                    data.put("opening_hours", result.get("opening_hours").get("weekday_text"));
                }

                log.info("Google Places details extracted: {} fields", data.size());
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Google Places details: {}", e.getMessage());
        }

        return data;
    }
}
