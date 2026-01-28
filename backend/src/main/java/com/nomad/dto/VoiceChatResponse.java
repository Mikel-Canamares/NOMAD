package com.nomad.dto;

public record VoiceChatResponse(
    String response,
    String conversationId  // Para tracking de la conversación
) {}
