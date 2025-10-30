package com.nomad.controller;

import com.nomad.dto.AskRequest;
import com.nomad.dto.AskResponse;
import com.nomad.service.AskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ask")
@Validated
@Tag(name = "Ask", description = "Information aggregator from multiple sources")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    @Operation(summary = "Get aggregated information", description = "Aggregates POI information from OSM, Wikidata, Wikipedia, OpenTripMap, Foursquare, and Google Places")
    public ResponseEntity<AskResponse> ask(
            @Valid @RequestBody AskRequest request,

            @Parameter(description = "Latitude (required if no poiId)")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Longitude (required if no poiId)")
            @RequestParam(required = false) Double lng
    ) {
        AskResponse response = askService.aggregateInformation(request, lat, lng);
        return ResponseEntity.ok(response);
    }
}
