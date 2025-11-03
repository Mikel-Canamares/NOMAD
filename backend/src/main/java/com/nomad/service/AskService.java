package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomad.dto.*;
import com.nomad.service.aggregator.*;
import com.nomad.util.GeoHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);
    private static final int MARKDOWN_MAX_LENGTH = 1000;

    private final PoiService poiService;
    private final GooglePlacesDetailsClient placesDetailsClient;
    private final OsmAggregator osmAggregator;
    private final WikidataAggregator wikidataAggregator;
    private final WikipediaAggregator wikipediaAggregator;
    private final OpenTripMapAggregator openTripMapAggregator;
    private final FoursquareAggregator foursquareAggregator;
    private final GooglePlacesAggregator googlePlacesAggregator;

    @Value("${aggregator.enable.osm:false}")
    private boolean enableOsm;

    @Value("${aggregator.enable.wikidata:false}")
    private boolean enableWikidata;

    @Value("${aggregator.enable.wikipedia:false}")
    private boolean enableWikipedia;

    @Value("${aggregator.enable.opentripmap:false}")
    private boolean enableOpenTripMap;

    @Value("${aggregator.enable.foursquare:false}")
    private boolean enableFoursquare;

    @Value("${googleplaces.language-code}")
    private String defaultLanguageCode;

    @Value("${googleplaces.region-code}")
    private String defaultRegionCode;

    public AskService(PoiService poiService,
                      GooglePlacesDetailsClient placesDetailsClient,
                      OsmAggregator osmAggregator,
                      WikidataAggregator wikidataAggregator,
                      WikipediaAggregator wikipediaAggregator,
                      OpenTripMapAggregator openTripMapAggregator,
                      FoursquareAggregator foursquareAggregator,
                      GooglePlacesAggregator googlePlacesAggregator) {
        this.poiService = poiService;
        this.placesDetailsClient = placesDetailsClient;
        this.osmAggregator = osmAggregator;
        this.wikidataAggregator = wikidataAggregator;
        this.wikipediaAggregator = wikipediaAggregator;
        this.openTripMapAggregator = openTripMapAggregator;
        this.foursquareAggregator = foursquareAggregator;
        this.googlePlacesAggregator = googlePlacesAggregator;
    }

    @Cacheable(value = "askCache", key = "#request.poiId() != null ? #request.poiId() + ':' + #request.locale() : T(com.nomad.util.GeoHashUtil).generateCacheKey(#lat, #lng, #request.text() + ':' + #request.locale())")
    public AskResponse aggregateInformation(AskRequest request, Double lat, Double lng) {
        log.info("Ask request: poiId={}, text={}, locale={}, lat={}, lng={}",
                request.poiId(), request.text(), request.locale(), lat, lng);

        String placeId = null;
        String poiName = request.text();
        Double poiLat = lat;
        Double poiLng = lng;
        String category = null;

        // Step 1: Handle poiId if provided (format: "gplaces:<place_id>")
        if (request.poiId() != null && !request.poiId().isBlank()) {
            if (request.poiId().startsWith("gplaces:")) {
                placeId = request.poiId().substring(8); // Remove "gplaces:" prefix
                log.info("Using provided Google Places ID: {}", placeId);
            } else {
                log.warn("Invalid poiId format: {}. Expected 'gplaces:<place_id>'", request.poiId());
            }
        }

        // Step 2: If no placeId, try to resolve from /poi/nearby
        if (placeId == null && poiLat != null && poiLng != null) {
            log.debug("No poiId provided, resolving candidate from /poi/nearby");
            String[] normalized = normalizeLocale(request.locale());
            List<PoiResponse> nearbyPois = poiService.getNearbyPois(poiLat, poiLng, 100.0, null, 1, normalized[0], normalized[1]);

            if (!nearbyPois.isEmpty()) {
                PoiResponse candidate = nearbyPois.get(0);
                if (candidate.id() != null && candidate.id().startsWith("gplaces:")) {
                    placeId = candidate.id().substring(8);
                    poiName = candidate.name();
                    poiLat = candidate.lat();
                    poiLng = candidate.lng();
                    category = candidate.category();
                    log.info("Resolved candidate: name={}, placeId={}", poiName, placeId);
                }
            }
        }

        // Step 3: Fetch Google Places Details
        if (placeId != null) {
            return buildResponseFromPlacesDetails(placeId, poiName, poiLat, poiLng, category, request.locale());
        }

        // Step 4: Fallback to legacy aggregators if enabled (should be disabled)
        if (enableOsm || enableWikidata || enableWikipedia || enableOpenTripMap || enableFoursquare) {
            return legacyAggregation(request, lat, lng);
        }

        // Step 5: No data available
        log.warn("No data available for request");
        return createNoDataResponse(poiName, poiLat, poiLng, category, request.poiId());
    }

    private AskResponse buildResponseFromPlacesDetails(
            String placeId,
            String poiName,
            Double lat,
            Double lng,
            String category,
            String locale
    ) {
        String[] normalized = normalizeLocale(locale);
        String languageCode = normalized[0];
        String regionCode = normalized[1];

        JsonNode details = placesDetailsClient.getPlaceDetails(placeId, languageCode, regionCode);

        if (details == null) {
            log.warn("Failed to fetch Places Details for placeId={}", placeId);
            return createNoDataResponse(poiName, lat, lng, category, "gplaces:" + placeId);
        }

        // Extract fields from Google Places API (New)
        String displayName = extractDisplayName(details);
        JsonNode location = details.has("location") ? details.get("location") : null;
        String websiteUri = details.has("websiteUri") ? details.get("websiteUri").asText() : null;
        String googleMapsUri = details.has("googleMapsUri") ? details.get("googleMapsUri").asText() : null;
        String primaryType = details.has("primaryType") ? details.get("primaryType").asText() : null;

        // Update name and location if available
        if (displayName != null) {
            poiName = displayName;
        }
        if (location != null) {
            lat = location.has("latitude") ? location.get("latitude").asDouble() : lat;
            lng = location.has("longitude") ? location.get("longitude").asDouble() : lng;
        }

        // Build facts
        Map<String, Object> facts = new HashMap<>();
        if (websiteUri != null) {
            facts.put("official_site", websiteUri);
        }

        // Build sources
        List<SourceReference> sources = new ArrayList<>();
        if (googleMapsUri != null) {
            sources.add(new SourceReference("Google Maps", googleMapsUri));
        }
        if (websiteUri != null) {
            sources.add(new SourceReference("Sitio oficial", websiteUri));
        }

        // Build POI info
        PoiInfo poi = new PoiInfo(
                "gplaces:" + placeId,
                poiName,
                lat,
                lng,
                category != null ? category : primaryType,
                0.95
        );

        // Build markdown (≤800-1000 chars, factual and sober)
        String markdown = buildMinimalMarkdown(poiName, facts, sources);

        return new AskResponse(
                markdown,
                facts,
                poi,
                sources,
                List.of("GooglePlaces"),
                false
        );
    }

    private String extractDisplayName(JsonNode details) {
        if (details.has("displayName")) {
            JsonNode displayName = details.get("displayName");
            if (displayName.has("text")) {
                return displayName.get("text").asText();
            }
        }
        return null;
    }

    private String buildMinimalMarkdown(String name, Map<String, Object> facts, List<SourceReference> sources) {
        StringBuilder md = new StringBuilder();

        md.append("## ").append(name != null ? name : "Point of Interest").append("\n\n");

        // Keep it minimal and factual (≤800-1000 chars)
        if (facts.containsKey("official_site")) {
            md.append("**Website:** ").append(facts.get("official_site")).append("\n\n");
        }

        if (!sources.isEmpty()) {
            md.append("### Sources\n\n");
            for (SourceReference source : sources) {
                md.append("- [").append(source.title()).append("](").append(source.url()).append(")\n");
            }
        }

        String result = md.toString();

        // Truncate if exceeds 1000 chars
        if (result.length() > MARKDOWN_MAX_LENGTH) {
            result = result.substring(0, MARKDOWN_MAX_LENGTH - 3) + "...";
        }

        return result;
    }

    private AskResponse createNoDataResponse(String name, Double lat, Double lng, String category, String poiId) {
        PoiInfo poi = null;
        if (lat != null && lng != null) {
            poi = new PoiInfo(
                    poiId != null ? poiId : "unknown",
                    name != null ? name : "Unknown location",
                    lat,
                    lng,
                    category,
                    0.5
            );
        }

        String markdown = "No detailed information available for this location.";

        return new AskResponse(
                markdown,
                new HashMap<>(),
                poi,
                new ArrayList<>(),
                List.of("GooglePlaces"),
                true
        );
    }

    // Legacy aggregation method (should not be used when all flags are false)
    private AskResponse legacyAggregation(AskRequest request, Double lat, Double lng) {
        log.warn("Using legacy aggregation (should be disabled!)");

        Map<String, Object> facts = new HashMap<>();
        List<SourceReference> sources = new ArrayList<>();
        List<String> usedSources = new ArrayList<>();

        String poiName = request.text();
        Double poiLat = lat;
        Double poiLng = lng;

        if (poiLat == null || poiLng == null) {
            return createNoDataResponse(poiName, null, null, null, request.poiId());
        }

        // OSM
        if (enableOsm) {
            Map<String, Object> osmData = osmAggregator.searchByCoordinates(poiLat, poiLng, 100);
            if (!osmData.isEmpty()) {
                usedSources.add("OSM");
                sources.add(osmAggregator.getSourceReference());
                facts.putAll(osmData);
                if (osmData.containsKey("name")) {
                    poiName = (String) osmData.get("name");
                }
            }
        }

        // Wikidata
        if (enableWikidata) {
            String wikidataQid = (String) facts.get("wikidata_qid");
            if (wikidataQid != null) {
                Map<String, Object> wikidataData = wikidataAggregator.getEntityData(wikidataQid);
                if (!wikidataData.isEmpty()) {
                    usedSources.add("Wikidata");
                    sources.add(wikidataAggregator.getSourceReference(wikidataQid));
                    facts.putAll(wikidataData);
                }
            }
        }

        // Wikipedia
        if (enableWikipedia) {
            String wikipediaTitle = (String) facts.get("wikipedia_title");
            if (wikipediaTitle != null) {
                String extract = wikipediaAggregator.getExtract(wikipediaTitle, request.locale());
                if (extract != null && !extract.isBlank()) {
                    usedSources.add("Wikipedia");
                    sources.add(wikipediaAggregator.getSourceReference(wikipediaTitle, request.locale()));
                    facts.put("wikipedia_extract", extract);
                }
            }
        }

        // OpenTripMap
        if (enableOpenTripMap) {
            Map<String, Object> otmData = openTripMapAggregator.searchByCoordinates(poiLat, poiLng, 100);
            if (!otmData.isEmpty()) {
                usedSources.add("OpenTripMap");
                facts.putAll(otmData);
            }
        }

        // Foursquare
        if (enableFoursquare && poiName != null) {
            Map<String, Object> foursquareData = foursquareAggregator.searchPlace(poiName, poiLat, poiLng);
            if (!foursquareData.isEmpty()) {
                usedSources.add("Foursquare");
                facts.putAll(foursquareData);
            }
        }

        String markdown = buildMinimalMarkdown(poiName, facts, sources);

        PoiInfo poi = new PoiInfo(
                request.poiId() != null ? request.poiId() : "unknown",
                poiName,
                poiLat,
                poiLng,
                null,
                0.7
        );

        return new AskResponse(markdown, facts, poi, sources, usedSources, facts.isEmpty());
    }

    private String[] normalizeLocale(String locale) {
        String languageCode = defaultLanguageCode;
        String regionCode = defaultRegionCode;

        if (locale != null && !locale.isBlank()) {
            // Parse "es-ES" → lang="es", region="ES"
            if (locale.contains("-")) {
                String[] parts = locale.split("-");
                languageCode = parts[0].toLowerCase();
                if (parts.length > 1) {
                    regionCode = parts[1].toUpperCase();
                }
            } else {
                languageCode = locale.toLowerCase();
            }
        }

        return new String[]{languageCode, regionCode};
    }
}
