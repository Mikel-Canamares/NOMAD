package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class GooglePlacesDetailsClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesDetailsClient.class);
    private static final String DETAILS_URL = "https://places.googleapis.com/v1/places";
    private static final String USER_AGENT = "NomadApp/1.0 (Educational Project)";
    private static final String FIELD_MASK = "id,displayName,location,websiteUri,googleMapsUri,primaryType,primaryTypeDisplayName";

    private final WebClient webClient;
    private final String apiKey;

    public GooglePlacesDetailsClient(
            WebClient.Builder webClientBuilder,
            @Value("${googleplaces.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;
        log.info("GooglePlacesDetailsClient initialized with API key: {}...",
                apiKey.length() > 10 ? apiKey.substring(0, 10) : "INVALID");

        this.webClient = webClientBuilder
                .baseUrl(DETAILS_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .defaultHeader("X-Goog-FieldMask", FIELD_MASK)
                .build();
    }

    // TODO: Re-enable cache once placeDetailsCache is properly configured
    // @Cacheable(value = "placeDetailsCache", key = "#placeId + ':' + #languageCode + ':' + #regionCode")
    public JsonNode getPlaceDetails(String placeId, String languageCode, String regionCode) {
        if (placeId == null || placeId.isBlank()) {
            log.warn("Cannot fetch details: placeId is null or empty");
            return null;
        }

        log.debug("Fetching Place Details for placeId={}, lang={}, region={}", placeId, languageCode, regionCode);

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{placeId}")
                            .queryParam("languageCode", languageCode)
                            .queryParam("regionCode", regionCode)
                            .build(placeId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::shouldRetry)
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Retrying Place Details, attempt: {}", retrySignal.totalRetries() + 1)))
                    .block();

            if (response != null) {
                log.debug("Place Details fetched successfully for placeId={}", placeId);
                return response;
            }

            log.warn("Empty response from Place Details API for placeId={}", placeId);
            return null;

        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("Place Details API error: status={}, placeId={}, message={}",
                    status, placeId, e.getMessage());
            return null;

        } catch (Exception e) {
            log.warn("Failed to fetch Place Details for placeId={}: {}",
                    placeId, e.getMessage());
            return null;
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
