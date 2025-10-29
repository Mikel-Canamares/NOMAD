package com.nomad.controller;

import com.nomad.dto.AskRequest;
import com.nomad.dto.AskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/ask")
@Validated
@Tag(name = "Ask", description = "AI-powered Q&A API")
public class AskController {

    @PostMapping
    @Operation(summary = "Ask a question", description = "Submit a question to the AI assistant")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        // TODO: Implement AI Q&A logic
        AskResponse response = new AskResponse("WIP", Collections.emptyList());
        return ResponseEntity.ok(response);
    }
}
