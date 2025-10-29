package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for asking a question")
public record AskRequest(

    @Schema(description = "Question text", example = "What is the history of this place?", required = true)
    @NotBlank(message = "Text cannot be blank")
    String text,

    @Schema(description = "Locale for response (e.g., en, es)", example = "en")
    String locale,

    @Schema(description = "Optional POI ID for context", example = "poi123")
    String poiId
) {
}
