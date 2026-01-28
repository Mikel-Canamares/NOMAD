package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nomad.dto.VoiceChatRequest;
import com.nomad.dto.VoiceChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoiceChatService {

    private static final Logger log = LoggerFactory.getLogger(VoiceChatService.class);
    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public VoiceChatService(WebClient.Builder webClientBuilder,
                            @Value("${api.openai.key:PLACEHOLDER_OPENAI_KEY}") String apiKey,
                            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .build();
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Procesa un mensaje de voz usando GPT-4 (mucho más barato que Realtime API)
     * Costes aproximados:
     * - Realtime API: $0.06/min audio input + $0.24/min audio output = ~$18/hora
     * - STT + GPT-4 + TTS nativo: $0.006/min (Whisper) + $0.01/1K tokens (GPT-4) + GRATIS (TTS) = ~$0.50/hora
     * AHORRO: 97% de reducción de costes!
     */
    public VoiceChatResponse chat(VoiceChatRequest request) {
        if ("PLACEHOLDER_OPENAI_KEY".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key not configured");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI API key not configured");
        }

        try {
            log.info("Processing voice chat: '{}'", request.message());

            // Construir contexto del sistema
            String systemContext = buildSystemContext(request);

            // Construir mensajes de conversación
            ArrayNode messages = buildMessages(systemContext, request);

            // Crear request body para GPT-4
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "gpt-4-turbo-preview");
            requestBody.set("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 300);  // Respuestas concisas

            // Llamar a OpenAI Chat API
            JsonNode response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response == null || !response.has("choices")) {
                log.error("Invalid response from OpenAI Chat API");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid response from OpenAI");
            }

            String assistantResponse = response.get("choices").get(0).get("message").get("content").asText();
            log.info("GPT-4 response: {}", assistantResponse);

            // Generar ID de conversación para tracking
            String conversationId = UUID.randomUUID().toString();

            return new VoiceChatResponse(assistantResponse, conversationId);

        } catch (Exception e) {
            log.error("Error processing voice chat: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing chat: " + e.getMessage());
        }
    }

    /**
     * Construye el contexto del sistema con ubicación y POIs
     */
    private String buildSystemContext(VoiceChatRequest request) {
        StringBuilder context = new StringBuilder();
        context.append("Eres un asistente de viaje experto. ");

        // Ubicación
        if (request.userLat() != null && request.userLng() != null) {
            context.append(String.format("El usuario está en las coordenadas %.6f, %.6f. ",
                request.userLat(), request.userLng()));
        }

        // Categoría activa
        if (request.selectedCategory() != null && !request.selectedCategory().isBlank()) {
            context.append(String.format("Está viendo la categoría: %s. ", request.selectedCategory()));
        }

        // POIs cercanos
        if (request.nearbyPois() != null && !request.nearbyPois().isEmpty()) {
            String poisList = request.nearbyPois().stream()
                .limit(10)
                .map(poi -> (String) poi.get("name"))
                .collect(Collectors.joining(", "));

            context.append(String.format("Hay %d POIs cercanos: %s. ",
                request.nearbyPois().size(), poisList));
        }

        context.append("Responde de forma concisa y natural (máximo 2-3 frases). ");
        context.append("Si necesitas información específica de un POI, usa los datos proporcionados. ");
        context.append("Responde en español con tono amigable.");

        return context.toString();
    }

    /**
     * Construye el array de mensajes incluyendo historial
     */
    private ArrayNode buildMessages(String systemContext, VoiceChatRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();

        // Mensaje del sistema con contexto
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemContext);
        messages.add(systemMessage);

        // Historial de conversación
        if (request.conversationHistory() != null) {
            for (Map<String, String> historyItem : request.conversationHistory()) {
                ObjectNode historyMessage = objectMapper.createObjectNode();
                historyMessage.put("role", historyItem.get("role"));
                historyMessage.put("content", historyItem.get("content"));
                messages.add(historyMessage);
            }
        }

        // Mensaje actual del usuario
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", request.message());
        messages.add(userMessage);

        return messages;
    }
}
