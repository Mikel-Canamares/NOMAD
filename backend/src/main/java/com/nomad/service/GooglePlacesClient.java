package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GooglePlacesClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);
    private static final String NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String USER_AGENT = "NomadApp/1.0 (Educational Project)";
    private static final String FIELD_MASK = "places.id,places.displayName,places.location,places.primaryType,places.primaryTypeDisplayName,places.googleMapsUri";

    private final WebClient nearbyClient;
    private final WebClient textSearchClient;
    private final String apiKey;

    public GooglePlacesClient(
            WebClient.Builder webClientBuilder,
            @Value("${googleplaces.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;
        log.info("GooglePlacesClient initialized with API key: {}...",
                 apiKey.length() > 10 ? apiKey.substring(0, 10) : "INVALID");

        this.nearbyClient = webClientBuilder
                .baseUrl(NEARBY_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .defaultHeader("X-Goog-FieldMask", FIELD_MASK)
                .build();

        this.textSearchClient = webClientBuilder
                .baseUrl(TEXT_SEARCH_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .defaultHeader("X-Goog-FieldMask", FIELD_MASK)
                .build();
    }

    public List<JsonNode> searchNearby(
            double lat,
            double lng,
            double radius,
            List<String> includedTypes,
            List<String> excludedTypes,
            int limit,
            String languageCode,
            String regionCode
    ) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("includedTypes", includedTypes);
        requestBody.put("excludedTypes", excludedTypes);
        requestBody.put("maxResultCount", Math.min(20, limit));  // Max 20 for Nearby
        requestBody.put("rankPreference", "POPULARITY");
        requestBody.put("languageCode", languageCode);
        requestBody.put("regionCode", regionCode);

        Map<String, Object> circle = new HashMap<>();
        Map<String, Double> center = new HashMap<>();
        center.put("latitude", lat);
        center.put("longitude", lng);
        circle.put("center", center);
        circle.put("radius", radius);

        Map<String, Object> locationRestriction = new HashMap<>();
        locationRestriction.put("circle", circle);
        requestBody.put("locationRestriction", locationRestriction);

        log.debug("Nearby Search: lat={}, lng={}, radius={}, types={}, limit={}",
                lat, lng, radius, includedTypes, limit);

        try {
            JsonNode response = nearbyClient.post()
                    .uri("")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::shouldRetry)
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying Nearby Search, attempt: {}", retrySignal.totalRetries() + 1)))
                    .block();

            if (response != null && response.has("places")) {
                List<JsonNode> places = new java.util.ArrayList<>();
                response.get("places").forEach(places::add);
                log.debug("Nearby Search returned {} places", places.size());
                return places;
            }

            return List.of();

        } catch (Exception e) {
            log.warn("Nearby Search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<JsonNode> searchText(
            double lat,
            double lng,
            double radius,
            String textQuery,
            String includedType,
            int pageSize,
            String languageCode,
            String regionCode
    ) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("textQuery", textQuery);
        requestBody.put("includedType", includedType);
        requestBody.put("strictTypeFiltering", true);
        requestBody.put("rankPreference", "RELEVANCE");
        requestBody.put("pageSize", pageSize);
        requestBody.put("languageCode", languageCode);
        requestBody.put("regionCode", regionCode);

        // Create bounding box from circle
        double latDelta = radius / 111000.0;  // ~111km per degree
        double lngDelta = radius / (111000.0 * Math.cos(Math.toRadians(lat)));

        Map<String, Object> rectangle = new HashMap<>();
        Map<String, Object> low = new HashMap<>();
        low.put("latitude", lat - latDelta);
        low.put("longitude", lng - lngDelta);
        Map<String, Object> high = new HashMap<>();
        high.put("latitude", lat + latDelta);
        high.put("longitude", lng + lngDelta);
        rectangle.put("low", low);
        rectangle.put("high", high);

        Map<String, Object> locationRestriction = new HashMap<>();
        locationRestriction.put("rectangle", rectangle);
        requestBody.put("locationRestriction", locationRestriction);

        log.debug("Text Search: query='{}', type={}, lat={}, lng={}, radius={}",
                textQuery, includedType, lat, lng, radius);

        try {
            JsonNode response = textSearchClient.post()
                    .uri("")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::shouldRetry)
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying Text Search, attempt: {}", retrySignal.totalRetries() + 1)))
                    .block();

            if (response != null && response.has("places")) {
                List<JsonNode> places = new java.util.ArrayList<>();
                response.get("places").forEach(places::add);
                log.debug("Text Search returned {} places", places.size());
                return places;
            }

            return List.of();

        } catch (Exception e) {
            log.warn("Text Search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            int status = webEx.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return throwable instanceof java.util.concurrent.TimeoutException;
    }
}
