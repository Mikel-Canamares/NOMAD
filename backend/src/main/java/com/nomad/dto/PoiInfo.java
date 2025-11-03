package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic POI information included in Ask response")
public record PoiInfo(
    @Schema(description = "POI ID (e.g., gplaces:ChIJN1t_tDeuEmsRUsoyG83frY4)", example = "gplaces:ChIJN1t_tDeuEmsRUsoyG83frY4")
    String id,

    @Schema(description = "POI name", example = "Puerta del Sol")
    String name,

    @Schema(description = "Latitude", example = "40.4168")
    Double lat,

    @Schema(description = "Longitude", example = "-3.7038")
    Double lng,

    @Schema(description = "Category", example = "monument")
    String category,

    @Schema(description = "Confidence score (0.0-1.0)", example = "0.95")
    Double confidence
) {
}
