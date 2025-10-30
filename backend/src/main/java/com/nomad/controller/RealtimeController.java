package com.nomad.controller;

import com.nomad.dto.RealtimeSessionResponse;
import com.nomad.dto.realtime.ToolCall;
import com.nomad.dto.realtime.ToolResponse;
import com.nomad.service.RealtimeService;
import com.nomad.service.ToolExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/realtime")
@Tag(name = "Realtime", description = "OpenAI Realtime API integration")
public class RealtimeController {

    private final RealtimeService realtimeService;
    private final ToolExecutor toolExecutor;

    public RealtimeController(RealtimeService realtimeService, ToolExecutor toolExecutor) {
        this.realtimeService = realtimeService;
        this.toolExecutor = toolExecutor;
    }

    @PostMapping("/session")
    @Operation(
        summary = "Create OpenAI Realtime session",
        description = "Creates an ephemeral session token for OpenAI Realtime API (audio/text). Token is valid for a limited time."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session created successfully"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "401", description = "Invalid API key"),
        @ApiResponse(responseCode = "500", description = "Internal server error"),
        @ApiResponse(responseCode = "502", description = "OpenAI API error")
    })
    public ResponseEntity<RealtimeSessionResponse> createSession() {
        RealtimeSessionResponse response = realtimeService.createSession();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tool-call")
    @Operation(
        summary = "Execute tool call from OpenAI Realtime",
        description = "Handles tool calls from OpenAI Realtime API. Executes poi_nearby or poi_context and returns sanitized results."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tool executed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid tool call"),
        @ApiResponse(responseCode = "500", description = "Tool execution failed")
    })
    public ResponseEntity<ToolResponse> executeToolCall(@Valid @RequestBody ToolCall toolCall) {
        ToolResponse response = toolExecutor.execute(toolCall);
        return ResponseEntity.ok(response);
    }
}
