package com.nomad.service;

import com.nomad.dto.AskRequest;
import com.nomad.dto.AskResponse;
import com.nomad.dto.PoiResponse;
import com.nomad.dto.SourceReference;
import com.nomad.service.aggregator.*;
import com.nomad.util.GeoHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final PoiService poiService;
    private final OsmAggregator osmAggregator;
    private final WikidataAggregator wikidataAggregator;
    private final WikipediaAggregator wikipediaAggregator;
    private final OpenTripMapAggregator openTripMapAggregator;
    private final FoursquareAggregator foursquareAggregator;
    private final GooglePlacesAggregator googlePlacesAggregator;

    public AskService(PoiService poiService,
                      OsmAggregator osmAggregator,
                      WikidataAggregator wikidataAggregator,
                      WikipediaAggregator wikipediaAggregator,
                      OpenTripMapAggregator openTripMapAggregator,
                      FoursquareAggregator foursquareAggregator,
                      GooglePlacesAggregator googlePlacesAggregator) {
        this.poiService = poiService;
        this.osmAggregator = osmAggregator;
        this.wikidataAggregator = wikidataAggregator;
        this.wikipediaAggregator = wikipediaAggregator;
        this.openTripMapAggregator = openTripMapAggregator;
        this.foursquareAggregator = foursquareAggregator;
        this.googlePlacesAggregator = googlePlacesAggregator;
    }

    @Cacheable(value = "askCache", key = "#request.poiId() != null ? #request.poiId() + ':' + #request.locale() : T(com.nomad.util.GeoHashUtil).generateCacheKey(#lat, #lng, #request.text() + ':' + #request.locale())")
    public AskResponse aggregateInformation(AskRequest request, Double lat, Double lng) {
        log.info("Aggregating information for request: {}", request);

        Map<String, Object> facts = new HashMap<>();
        List<SourceReference> sources = new ArrayList<>();
        List<String> usedSources = new ArrayList<>();

        String poiName = request.text();
        Double poiLat = lat;
        Double poiLng = lng;

        // Step 1: If poiId is provided, get POI details from cache
        if (request.poiId() != null && !request.poiId().isBlank()) {
            log.info("Source: Looking up POI from cache with id={}", request.poiId());
            // TODO: Implement actual POI lookup from cache
            // For now, we'll use the provided coordinates
        }

        if (poiLat == null || poiLng == null) {
            log.warn("No coordinates available for aggregation");
            return createMinimalResponse(facts, sources, usedSources);
        }

        // Step 2a: OSM data
        Map<String, Object> osmData = osmAggregator.searchByCoordinates(poiLat, poiLng, 100);
        if (!osmData.isEmpty()) {
            usedSources.add("OSM");
            sources.add(osmAggregator.getSourceReference());
            facts.putAll(osmData);

            // Use OSM name if available
            if (osmData.containsKey("name")) {
                poiName = (String) osmData.get("name");
            }
        }

        // Step 2b: Wikidata
        String wikidataQid = (String) osmData.get("wikidata_qid");
        if (wikidataQid != null) {
            log.info("Source: Wikidata with QID={}", wikidataQid);
            Map<String, Object> wikidataData = wikidataAggregator.getEntityData(wikidataQid);
            if (!wikidataData.isEmpty()) {
                usedSources.add("Wikidata");
                sources.add(wikidataAggregator.getSourceReference(wikidataQid));
                facts.putAll(wikidataData);
            }
        }

        // Step 2c: Wikipedia
        String wikipediaTitle = (String) osmData.getOrDefault("wikipedia_title",
                                facts.get("wikipedia_title"));
        if (wikipediaTitle != null) {
            log.info("Source: Wikipedia with title={}", wikipediaTitle);
            String extract = wikipediaAggregator.getExtract(wikipediaTitle, request.locale());
            if (extract != null && !extract.isBlank()) {
                usedSources.add("Wikipedia");
                sources.add(wikipediaAggregator.getSourceReference(wikipediaTitle, request.locale()));
                facts.put("wikipedia_extract", extract);
            }
        }

        // Step 2d: OpenTripMap
        Map<String, Object> otmData = openTripMapAggregator.searchByCoordinates(poiLat, poiLng, 100);
        if (!otmData.isEmpty()) {
            usedSources.add("OpenTripMap");
            facts.putAll(otmData);
        }

        // Step 2e: Foursquare
        if (poiName != null) {
            Map<String, Object> foursquareData = foursquareAggregator.searchPlace(poiName, poiLat, poiLng);
            if (!foursquareData.isEmpty()) {
                usedSources.add("Foursquare");
                facts.putAll(foursquareData);
            }
        }

        // Step 2f: Google Places
        if (poiName != null) {
            Map<String, Object> googleData = googlePlacesAggregator.searchPlace(poiName, poiLat, poiLng);
            if (!googleData.isEmpty()) {
                usedSources.add("GooglePlaces");
                facts.putAll(googleData);
            }
        }

        // Step 3: Build markdown response
        String markdown = buildMarkdownResponse(poiName, facts, usedSources);

        log.info("Aggregation complete: {} sources used, {} facts collected", usedSources.size(), facts.size());

        return new AskResponse(markdown, facts, sources, usedSources);
    }

    private String buildMarkdownResponse(String poiName, Map<String, Object> facts, List<String> usedSources) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(poiName != null ? poiName : "Point of Interest").append("\n\n");

        // Wikipedia extract
        if (facts.containsKey("wikipedia_extract")) {
            md.append(facts.get("wikipedia_extract")).append("\n\n");
        }

        // Key facts
        md.append("## Information\n\n");

        if (facts.containsKey("inception_year")) {
            md.append("**Year:** ").append(facts.get("inception_year")).append("\n\n");
        }

        if (facts.containsKey("style")) {
            md.append("**Style:** ").append(facts.get("style")).append("\n\n");
        }

        if (facts.containsKey("heritage")) {
            md.append("**Heritage:** ").append(facts.get("heritage")).append("\n\n");
        }

        // Contact & Practical Info
        if (facts.containsKey("website") || facts.containsKey("official_site") ||
            facts.containsKey("phone") || facts.containsKey("address")) {
            md.append("## Contact & Location\n\n");

            if (facts.containsKey("address")) {
                md.append("**Address:** ").append(facts.get("address")).append("\n\n");
            }

            if (facts.containsKey("phone")) {
                md.append("**Phone:** ").append(facts.get("phone")).append("\n\n");
            }

            String website = (String) facts.getOrDefault("official_site", facts.get("website"));
            if (website != null) {
                md.append("**Website:** ").append(website).append("\n\n");
            }
        }

        // Rating
        if (facts.containsKey("rating")) {
            md.append("**Rating:** ").append(facts.get("rating"));
            if (facts.containsKey("user_ratings_total")) {
                md.append(" (").append(facts.get("user_ratings_total")).append(" reviews)");
            }
            md.append("\n\n");
        }

        // Opening hours
        if (facts.containsKey("opening_hours")) {
            md.append("**Opening Hours:** ").append(facts.get("opening_hours")).append("\n\n");
        }

        md.append("---\n\n");
        md.append("*Data aggregated from: ").append(String.join(", ", usedSources)).append("*");

        return md.toString();
    }

    private AskResponse createMinimalResponse(Map<String, Object> facts,
                                               List<SourceReference> sources,
                                               List<String> usedSources) {
        String markdown = "No detailed information available for this location.";
        return new AskResponse(markdown, facts, sources, usedSources);
    }
}
