package com.nomad.dto;

import java.util.List;
import java.util.Map;

public record VoiceChatRequest(
    String message,
    Double userLat,
    Double userLng,
    String selectedCategory,
    List<Map<String, Object>> nearbyPois,
    List<Map<String, String>> conversationHistory  // Para mantener contexto
) {}
