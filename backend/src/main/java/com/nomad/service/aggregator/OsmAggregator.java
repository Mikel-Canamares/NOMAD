package com.nomad.service.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.SourceReference;
import com.nomad.service.OverpassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OsmAggregator {

    private static final Logger log = LoggerFactory.getLogger(OsmAggregator.class);
    private final OverpassService overpassService;

    public OsmAggregator(OverpassService overpassService) {
        this.overpassService = overpassService;
    }

    public Map<String, Object> searchByCoordinates(double lat, double lng, int radius) {
        Map<String, Object> data = new HashMap<>();

        try {
            log.info("Fetching OSM data: lat={}, lng={}, radius={}", lat, lng, radius);

            List<JsonNode> elements = overpassService.queryPois(lat, lng, radius, null);

            if (!elements.isEmpty()) {
                JsonNode first = elements.get(0);

                if (first.has("tags")) {
                    JsonNode tags = first.get("tags");

                    // Extract basic info
                    if (tags.has("name")) {
                        data.put("name", tags.get("name").asText());
                    }

                    // Wikidata QID
                    if (tags.has("wikidata")) {
                        data.put("wikidata_qid", tags.get("wikidata").asText());
                    }

                    // Wikipedia article
                    if (tags.has("wikipedia")) {
                        String wikipedia = tags.get("wikipedia").asText();
                        // Format: "en:Article_Name"
                        if (wikipedia.contains(":")) {
                            String[] parts = wikipedia.split(":", 2);
                            data.put("wikipedia_lang", parts[0]);
                            data.put("wikipedia_title", parts[1]);
                        }
                    }

                    // Contact info
                    if (tags.has("website")) {
                        data.put("website", tags.get("website").asText());
                    }
                    if (tags.has("phone")) {
                        data.put("phone", tags.get("phone").asText());
                    }
                    if (tags.has("opening_hours")) {
                        data.put("opening_hours", tags.get("opening_hours").asText());
                    }

                    // Address
                    if (tags.has("addr:street")) {
                        String street = tags.get("addr:street").asText();
                        String number = tags.has("addr:housenumber") ? tags.get("addr:housenumber").asText() : "";
                        data.put("address", number + " " + street);
                    }

                    log.info("OSM data extracted: {} fields", data.size());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to fetch OSM data: {}", e.getMessage());
        }

        return data;
    }

    public SourceReference getSourceReference() {
        return new SourceReference("OpenStreetMap", "https://www.openstreetmap.org/");
    }
}
