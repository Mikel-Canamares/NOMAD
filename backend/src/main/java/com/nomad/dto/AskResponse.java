package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Aggregated response from multiple sources")
public record AskResponse(

    @Schema(description = "Markdown-formatted answer (max 800-1000 chars)", example = "This is a **formatted** answer")
    String markdown,

    @Schema(description = "Structured facts about the POI")
    Map<String, Object> facts,

    @Schema(description = "Basic POI information")
    PoiInfo poi,

    @Schema(description = "List of source references with title and URL")
    List<SourceReference> sources,

    @Schema(description = "Names of sources that were successfully used", example = "[\"GooglePlaces\"]")
    List<String> used_sources,

    @Schema(description = "True if no sufficient data was found", example = "false")
    boolean no_data
) {
}
