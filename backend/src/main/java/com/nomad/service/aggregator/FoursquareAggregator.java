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
public class FoursquareAggregator {

    private static final Logger log = LoggerFactory.getLogger(FoursquareAggregator.class);
    private final WebClient webClient;
    private final String apiKey;

    public FoursquareAggregator(WebClient.Builder webClientBuilder,
                                 @Value("${api.foursquare.key:PLACEHOLDER_FOURSQUARE_KEY}") String apiKey) {
        this.webClient = webClientBuilder
            .baseUrl("https://api.foursquare.com/v3/places")
            .build();
        this.apiKey = apiKey;
    }

    public Map<String, Object> searchPlace(String name, double lat, double lng) {
        Map<String, Object> data = new HashMap<>();

        if ("PLACEHOLDER_FOURSQUARE_KEY".equals(apiKey)) {
            log.warn("Foursquare API key not configured, skipping");
            return data;
        }

        try {
            log.info("Fetching Foursquare data: name={}, lat={}, lng={}", name, lat, lng);

            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search")
                    .queryParam("query", name)
                    .queryParam("ll", lat + "," + lng)
                    .queryParam("limit", "1")
                    .build())
                .header("Authorization", apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode place = response.get("results").get(0);

                if (place.has("rating")) {
                    data.put("rating", place.get("rating").asDouble());
                }

                if (place.has("stats") && place.get("stats").has("total_ratings")) {
                    data.put("user_ratings_total", place.get("stats").get("total_ratings").asInt());
                }

                if (place.has("website")) {
                    data.put("website", place.get("website").asText());
                }

                if (place.has("location") && place.get("location").has("formatted_address")) {
                    data.put("address", place.get("location").get("formatted_address").asText());
                }

                log.info("Foursquare data extracted: {} fields", data.size());
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Foursquare data: {}", e.getMessage());
        }

        return data;
    }
}
