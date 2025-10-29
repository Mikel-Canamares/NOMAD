package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response from AI assistant")
public record AskResponse(

    @Schema(description = "Markdown-formatted answer", example = "This is a **formatted** answer")
    String markdown,

    @Schema(description = "List of source references")
    List<String> sources
) {
}
