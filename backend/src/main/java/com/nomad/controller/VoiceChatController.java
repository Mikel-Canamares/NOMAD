package com.nomad.controller;

import com.nomad.dto.VoiceChatRequest;
import com.nomad.dto.VoiceChatResponse;
import com.nomad.service.VoiceChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
public class VoiceChatController {

    private static final Logger log = LoggerFactory.getLogger(VoiceChatController.class);
    private final VoiceChatService voiceChatService;

    public VoiceChatController(VoiceChatService voiceChatService) {
        this.voiceChatService = voiceChatService;
    }

    /**
     * Endpoint para chat de voz
     * POST /api/voice/chat
     *
     * Recibe mensaje de texto (del STT) y devuelve respuesta de texto (para TTS)
     * Mucho más barato que OpenAI Realtime API
     */
    @PostMapping("/chat")
    public ResponseEntity<VoiceChatResponse> chat(@RequestBody VoiceChatRequest request) {
        log.info("Voice chat request received: {}", request.message());

        VoiceChatResponse response = voiceChatService.chat(request);

        return ResponseEntity.ok(response);
    }
}
