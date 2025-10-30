package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Point of Interest")
public record PoiResponse(

    @Schema(description = "Unique identifier", example = "node/123456")
    String id,

    @Schema(description = "POI name", example = "Eiffel Tower")
    String name,

    @Schema(description = "Category", example = "monument")
    String category,

    @Schema(description = "Latitude", example = "48.8584")
    Double lat,

    @Schema(description = "Longitude", example = "2.2945")
    Double lng,

    @Schema(description = "Data source", example = "OSM")
    String source,

    @Schema(description = "License", example = "ODbL")
    String license,

    @Schema(description = "Relevance score", example = "0.95")
    Double relevance
) {
}
