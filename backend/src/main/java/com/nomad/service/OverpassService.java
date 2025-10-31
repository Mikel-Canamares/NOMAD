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
    private static final String USER_AGENT = "NomadApp/1.0 (Educational Project)";

    private final WebClient webClient;

    public OverpassService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(OVERPASS_URL)
            .defaultHeader("User-Agent", USER_AGENT)
            .build();
    }

    public List<JsonNode> queryPois(double lat, double lng, double radiusMeters, String category) {
        String query = buildOverpassQuery(lat, lng, radiusMeters, category);

        log.debug("Overpass query: {}", query);

        try {
            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("data", query)
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(25))
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

    private String buildOverpassQuery(double lat, double lng, double radius, String category) {
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:25];(");

        if (category == null) {
            // Sin categoría: buscar todo tourism
            query.append(String.format(Locale.US,
                "nwr[\"tourism\"](around:%.1f,%.7f,%.7f);",
                radius, lat, lng));
        } else {
            // Con categoría: construir UNION de tags según mapeo
            List<String> tagFilters = getTagFiltersForCategory(category.toLowerCase());

            for (int i = 0; i < tagFilters.size(); i++) {
                if (i > 0) query.append("\n  ");
                query.append(String.format(Locale.US,
                    "nwr[%s](around:%.1f,%.7f,%.7f);",
                    tagFilters.get(i), radius, lat, lng));
            }
        }

        query.append(");out center;");
        return query.toString();
    }

    private List<String> getTagFiltersForCategory(String category) {
        return switch (category) {
            case "monument" -> List.of(
                "\"historic\"=\"monument\"",
                "\"historic\"=\"memorial\"",
                "\"tourism\"=\"artwork\"",
                "\"man_made\"=\"obelisk\""
            );
            case "museum" -> List.of(
                "\"tourism\"=\"museum\""
            );
            case "viewpoint" -> List.of(
                "\"tourism\"=\"viewpoint\""
            );
            case "heritage" -> List.of(
                "\"historic\"=\"castle\"",
                "\"historic\"=\"fort\"",
                "\"historic\"=\"palace\"",
                "\"historic\"=\"city_gate\"",
                "\"historic\"=\"archaeological_site\"",
                "\"historic\"=\"ruins\""
            );
            case "park" -> List.of(
                "\"leisure\"=\"park\"",
                "\"leisure\"=\"garden\""
            );
            default -> List.of("\"tourism\"");
        };
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
