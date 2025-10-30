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
public class OpenTripMapAggregator {

    private static final Logger log = LoggerFactory.getLogger(OpenTripMapAggregator.class);
    private final WebClient webClient;
    private final String apiKey;

    public OpenTripMapAggregator(WebClient.Builder webClientBuilder,
                                  @Value("${api.opentripmap.key:PLACEHOLDER_OPENTRIPMAP_KEY}") String apiKey) {
        this.webClient = webClientBuilder
            .baseUrl("https://api.opentripmap.com/0.1/en/places")
            .build();
        this.apiKey = apiKey;
    }

    public Map<String, Object> searchByCoordinates(double lat, double lng, int radius) {
        Map<String, Object> data = new HashMap<>();

        if ("PLACEHOLDER_OPENTRIPMAP_KEY".equals(apiKey)) {
            log.warn("OpenTripMap API key not configured, skipping");
            return data;
        }

        try {
            log.info("Fetching OpenTripMap data: lat={}, lng={}, radius={}", lat, lng, radius);

            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/radius")
                    .queryParam("radius", radius)
                    .queryParam("lon", lng)
                    .queryParam("lat", lat)
                    .queryParam("apikey", apiKey)
                    .queryParam("limit", "1")
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.isArray() && response.size() > 0) {
                JsonNode first = response.get(0);
                if (first.has("xid")) {
                    String xid = first.get("xid").asText();
                    return getPlaceDetails(xid);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to fetch OpenTripMap data: {}", e.getMessage());
        }

        return data;
    }

    private Map<String, Object> getPlaceDetails(String xid) {
        Map<String, Object> data = new HashMap<>();

        try {
            JsonNode details = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/xid/" + xid)
                    .queryParam("apikey", apiKey)
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (details != null) {
                if (details.has("wikipedia_extracts") && details.get("wikipedia_extracts").has("text")) {
                    String text = details.get("wikipedia_extracts").get("text").asText();
                    if (text.length() > 1200) {
                        text = text.substring(0, 1200);
                    }
                    data.put("description", text);
                }

                if (details.has("kinds")) {
                    data.put("kinds", details.get("kinds").asText());
                }

                log.info("OpenTripMap details extracted for xid={}", xid);
            }

        } catch (Exception e) {
            log.warn("Failed to fetch OpenTripMap details for {}: {}", xid, e.getMessage());
        }

        return data;
    }
}
