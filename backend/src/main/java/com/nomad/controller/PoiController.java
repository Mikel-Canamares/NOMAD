package com.nomad.controller;

import com.nomad.dto.PoiResponse;
import com.nomad.service.PoiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/poi")
@Validated
@Tag(name = "POI", description = "Points of Interest API")
public class PoiController {

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

            @Parameter(description = "Category filter (monument|museum|viewpoint|restaurant)")
            @RequestParam(required = false) String cat
    ) {
        List<PoiResponse> pois = poiService.getNearbyPois(lat, lng, radius, cat);
        return ResponseEntity.ok(pois);
    }
}
