package com.nomad.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/poi")
@Validated
@Tag(name = "POI", description = "Points of Interest API")
public class PoiController {

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby POIs", description = "Returns points of interest near the given coordinates")
    public ResponseEntity<List<Object>> getNearbyPois(
            @Parameter(description = "Latitude", required = true)
            @RequestParam @NotNull Double lat,

            @Parameter(description = "Longitude", required = true)
            @RequestParam @NotNull Double lng,

            @Parameter(description = "Search radius in meters", required = true)
            @RequestParam @NotNull Double radius,

            @Parameter(description = "Category filter")
            @RequestParam(required = false) String cat
    ) {
        // TODO: Implement POI search logic
        return ResponseEntity.ok(Collections.emptyList());
    }
}
