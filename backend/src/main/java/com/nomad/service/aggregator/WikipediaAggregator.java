package com.nomad.service.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.SourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class WikipediaAggregator {

    private static final Logger log = LoggerFactory.getLogger(WikipediaAggregator.class);
    private final WebClient webClient;

    public WikipediaAggregator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://{lang}.wikipedia.org")
            .build();
    }

    public String getExtract(String title, String locale) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String lang = locale != null ? locale : "en";

        try {
            log.info("Fetching Wikipedia extract: title={}, lang={}", title, lang);

            JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("format", "json")
                    .queryParam("prop", "extracts")
                    .queryParam("exintro", "true")
                    .queryParam("explaintext", "true")
                    .queryParam("exchars", "1200")
                    .queryParam("titles", title)
                    .build(lang))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && response.has("query") && response.get("query").has("pages")) {
                JsonNode pages = response.get("query").get("pages");
                JsonNode firstPage = pages.elements().next();

                if (firstPage.has("extract")) {
                    String extract = firstPage.get("extract").asText();
                    log.info("Wikipedia extract found: {} chars", extract.length());
                    return extract;
                }
            }

            // Fallback to English if locale failed
            if (!"en".equals(lang)) {
                log.info("Trying English fallback for Wikipedia");
                return getExtract(title, "en");
            }

        } catch (Exception e) {
            log.warn("Failed to fetch Wikipedia extract: {}", e.getMessage());
        }

        return null;
    }

    public SourceReference getSourceReference(String title, String locale) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String lang = locale != null ? locale : "en";
        String url = String.format("https://%s.wikipedia.org/wiki/%s",
            lang, title.replace(" ", "_"));
        return new SourceReference("Wikipedia", url);
    }
}
