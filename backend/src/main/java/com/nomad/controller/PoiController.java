package com.nomad.controller;

import com.nomad.dto.PoiResponse;
import com.nomad.service.PoiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/poi")
@Validated
@Tag(name = "POI", description = "Points of Interest API")
public class PoiController {

    // Categorías según especificación del documento
    private static final Set<String> VALID_CATEGORIES = Set.of(
        "history",      // Historia (Amarillo)
        "food",         // Gastronomía (Morado)
        "art",          // Arte y arquitectura (Rosa)
        "sports",       // Deportes y ocio (Naranja)
        "geography",    // Geografía (Azul)
        "industry",     // Industria y agricultura (Verde)
        // Categorías legacy para compatibilidad temporal
        "restaurant", "monument", "museum", "viewpoint", "heritage", "park"
    );

    private final PoiService poiService;

    @Value("${googleplaces.language-code}")
    private String defaultLanguageCode;

    @Value("${googleplaces.region-code}")
    private String defaultRegionCode;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby POIs", description = "Returns points of interest near the given coordinates")
    public ResponseEntity<List<PoiResponse>> getNearbyPois(
            @Parameter(description = "Latitude", required = true)
            @RequestParam @NotNull Double lat,

            @Parameter(description = "Longitude", required = true)
            @RequestParam @NotNull Double lng,

            @Parameter(description = "Search radius in meters", required = true)
            @RequestParam @NotNull Double radius,

            @Parameter(description = "Category filter: history | food | art | sports | geography | industry")
            @RequestParam(required = false) String cat,

            @Parameter(description = "Maximum number of results (default: 15, max: 50)")
            @RequestParam(required = false, defaultValue = "15")
            @Min(1) @Max(50) Integer limit,

            @Parameter(description = "Locale (e.g., 'es' or 'es-ES'). Defaults to 'es'")
            @RequestParam(required = false) String locale,

            @Parameter(description = "Region code (e.g., 'ES'). Defaults to 'ES'")
            @RequestParam(required = false) String region
    ) {
        // Validar categoría si se proporciona
        if (cat != null && !VALID_CATEGORIES.contains(cat.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid category. Must be one of: " + String.join(", ", VALID_CATEGORIES)
            );
        }

        // Normalizar locale y region
        String[] normalized = normalizeLocaleAndRegion(locale, region);
        String languageCode = normalized[0];
        String regionCode = normalized[1];

        List<PoiResponse> pois = poiService.getNearbyPois(lat, lng, radius, cat, limit, languageCode, regionCode);
        return ResponseEntity.ok(pois);
    }

    private String[] normalizeLocaleAndRegion(String locale, String region) {
        String languageCode = defaultLanguageCode;
        String regionCode = defaultRegionCode;

        if (locale != null && !locale.isBlank()) {
            // Parse "es-ES" → lang="es", region="ES"
            if (locale.contains("-")) {
                String[] parts = locale.split("-");
                languageCode = parts[0].toLowerCase();
                if (region == null && parts.length > 1) {
                    regionCode = parts[1].toUpperCase();
                }
            } else {
                languageCode = locale.toLowerCase();
            }
        }

        if (region != null && !region.isBlank()) {
            regionCode = region.toUpperCase();
        }

        return new String[]{languageCode, regionCode};
    }
}
