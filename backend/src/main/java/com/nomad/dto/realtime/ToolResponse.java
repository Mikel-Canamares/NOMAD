package com.nomad.dto.realtime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tool execution result")
public record ToolResponse(

    @Schema(description = "Tool call ID (same as request)", example = "call_abc123")
    String id,

    @Schema(description = "Execution result (success/error)")
    String result
) {
}
