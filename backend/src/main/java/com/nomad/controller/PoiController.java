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

    private static final Set<String> VALID_CATEGORIES = Set.of(
        "monument", "museum", "viewpoint", "heritage", "park"
    );

    private final PoiService poiService;

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

            @Parameter(description = "Category filter: monument | museum | viewpoint | heritage | park")
            @RequestParam(required = false) String cat,

            @Parameter(description = "Maximum number of results (default: 25, max: 50)")
            @RequestParam(required = false, defaultValue = "25")
            @Min(1) @Max(50) Integer limit
    ) {
        // Validar categoría si se proporciona
        if (cat != null && !VALID_CATEGORIES.contains(cat.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid category. Must be one of: " + String.join(", ", VALID_CATEGORIES)
            );
        }

        List<PoiResponse> pois = poiService.getNearbyPois(lat, lng, radius, cat, limit);
        return ResponseEntity.ok(pois);
    }
}
