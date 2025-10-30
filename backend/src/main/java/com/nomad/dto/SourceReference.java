package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reference to external source")
public record SourceReference(

    @Schema(description = "Source title", example = "Wikipedia")
    String title,

    @Schema(description = "Source URL", example = "https://en.wikipedia.org/wiki/Eiffel_Tower")
    String url
) {
}
