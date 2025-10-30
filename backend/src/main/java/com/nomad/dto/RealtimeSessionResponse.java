package com.nomad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Realtime session ephemeral token")
public record RealtimeSessionResponse(

    @Schema(description = "Ephemeral client secret for WebRTC connection", example = "eph_xxx...")
    String client_secret
) {
}
