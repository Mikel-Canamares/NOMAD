package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OverpassService {

    private static final Logger log = LoggerFactory.getLogger(OverpassService.class);
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private final WebClient webClient;

    public OverpassService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(OVERPASS_URL)
            .build();
    }

    public List<JsonNode> queryPois(double lat, double lng, double radiusMeters, String category) {
        String osmTags = mapCategoryToOsmTags(category);
        String query = buildOverpassQuery(lat, lng, radiusMeters, osmTags);

        log.debug("Overpass query: {}", query);

        try {
            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("data", query)
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(20))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(this::shouldRetry)
                    .doBeforeRetry(retrySignal ->
                        log.warn("Retrying Overpass API call, attempt: {}", retrySignal.totalRetries() + 1)))
                .block();

            return extractElements(response);

        } catch (Exception e) {
            log.warn("Overpass API call failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String mapCategoryToOsmTags(String category) {
        if (category == null) {
            return "tourism";
        }
        return switch (category.toLowerCase()) {
            case "monument" -> "tourism=monument";
            case "museum" -> "tourism=museum";
            case "viewpoint" -> "tourism=viewpoint";
            case "restaurant" -> "amenity=restaurant";
            default -> "tourism";
        };
    }

    private String buildOverpassQuery(double lat, double lng, double radius, String tags) {
        // Si tags no tiene "=", es una búsqueda genérica por clave (ej: "tourism")
        if (!tags.contains("=")) {
            return String.format(Locale.US,
                "[out:json][timeout:20];(node[\"%s\"](around:%.1f,%.7f,%.7f);way[\"%s\"](around:%.1f,%.7f,%.7f););out center;",
                tags, radius, lat, lng, tags, radius, lat, lng
            );
        }
        // Si tags tiene "=", es una búsqueda específica (ej: "tourism=monument")
        return String.format(Locale.US,
            "[out:json][timeout:20];(node[%s](around:%.1f,%.7f,%.7f);way[%s](around:%.1f,%.7f,%.7f););out center;",
            tags, radius, lat, lng, tags, radius, lat, lng
        );
    }

    private List<JsonNode> extractElements(JsonNode response) {
        List<JsonNode> elements = new ArrayList<>();
        if (response != null && response.has("elements")) {
            response.get("elements").forEach(elements::add);
        }
        return elements;
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            int status = webEx.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return throwable instanceof java.util.concurrent.TimeoutException;
    }
}
