package com.nomad.dto.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tool call from OpenAI Realtime API")
public record ToolCall(

    @Schema(description = "Tool call ID", example = "call_abc123")
    String id,

    @Schema(description = "Tool name", example = "poi_nearby")
    String name,

    @Schema(description = "Tool arguments as JSON")
    JsonNode arguments
) {
}
