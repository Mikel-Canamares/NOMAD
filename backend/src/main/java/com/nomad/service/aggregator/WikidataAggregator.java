package com.nomad.service.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.SourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class WikidataAggregator {

    private static final Logger log = LoggerFactory.getLogger(WikidataAggregator.class);
    private final WebClient webClient;

    public WikidataAggregator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://www.wikidata.org")
            .build();
    }

    public Map<String, Object> getEntityData(String qid) {
        if (qid == null || qid.isBlank()) {
            return new HashMap<>();
        }

        try {
            log.info("Fetching Wikidata entity: {}", qid);

            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/w/api.php")
                    .queryParam("action", "wbgetentities")
                    .queryParam("ids", qid)
                    .queryParam("format", "json")
                    .queryParam("props", "claims|sitelinks")
                    .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.has("entities")) {
                JsonNode entity = response.get("entities").get(qid);
                return extractClaims(entity);
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Wikidata entity {}: {}", qid, e.getMessage());
        }

        return new HashMap<>();
    }

    private Map<String, Object> extractClaims(JsonNode entity) {
        Map<String, Object> data = new HashMap<>();

        if (entity == null || !entity.has("claims")) {
            return data;
        }

        JsonNode claims = entity.get("claims");

        // P856: official website
        extractStringClaim(claims, "P856", "official_site", data);

        // P18: image
        extractStringClaim(claims, "P18", "image_url", data);

        // P571: inception (year)
        extractYearClaim(claims, "P571", "inception_year", data);

        // P149: architectural style
        extractStringClaim(claims, "P149", "style", data);

        // P1435: heritage designation
        extractStringClaim(claims, "P1435", "heritage", data);

        // Sitelinks (Wikipedia articles)
        if (entity.has("sitelinks")) {
            JsonNode sitelinks = entity.get("sitelinks");
            if (sitelinks.has("enwiki")) {
                data.put("wikipedia_title", sitelinks.get("enwiki").get("title").asText());
            }
        }

        log.info("Extracted {} Wikidata claims", data.size());
        return data;
    }

    private void extractStringClaim(JsonNode claims, String property, String key, Map<String, Object> data) {
        if (claims.has(property)) {
            JsonNode claim = claims.get(property).get(0);
            if (claim.has("mainsnak") && claim.get("mainsnak").has("datavalue")) {
                JsonNode datavalue = claim.get("mainsnak").get("datavalue");
                if (datavalue.has("value")) {
                    String value = datavalue.get("value").asText();
                    data.put(key, value);
                }
            }
        }
    }

    private void extractYearClaim(JsonNode claims, String property, String key, Map<String, Object> data) {
        if (claims.has(property)) {
            JsonNode claim = claims.get(property).get(0);
            if (claim.has("mainsnak") && claim.get("mainsnak").has("datavalue")) {
                JsonNode datavalue = claim.get("mainsnak").get("datavalue");
                if (datavalue.has("value") && datavalue.get("value").has("time")) {
                    String time = datavalue.get("value").get("time").asText();
                    // Extract year from format: +1889-05-15T00:00:00Z
                    if (time.length() > 5) {
                        String year = time.substring(1, 5);
                        data.put(key, year);
                    }
                }
            }
        }
    }

    public SourceReference getSourceReference(String qid) {
        if (qid == null || qid.isBlank()) {
            return null;
        }
        return new SourceReference("Wikidata", "https://www.wikidata.org/wiki/" + qid);
    }
}
