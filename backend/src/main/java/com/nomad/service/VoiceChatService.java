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
            requestBody.put("max_tokens", 200);  // Respuestas MUY concisas para modo conducción
            requestBody.put("frequency_penalty", 0.3);  // Evitar repeticiones
            requestBody.put("presence_penalty", 0.3);  // Fomentar variedad

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

        // Identidad y rol
        context.append("Eres NOMAD, un asistente turístico especializado en España. ");
        context.append("Ayudas a viajeros mientras conducen.\n\n");

        // Instrucciones principales
        context.append("INSTRUCCIONES IMPORTANTES:\n");
        context.append("1. Responde en español de forma CONVERSACIONAL y NATURAL\n");
        context.append("2. Sé CONCISO - respuestas cortas (máximo 3-4 frases)\n");
        context.append("3. El usuario está CONDUCIENDO, evita información que distraiga\n");
        context.append("4. Enfócate en lo MÁS INTERESANTE e IMPORTANTE\n");
        context.append("5. Usa un tono AMIGABLE y ENTUSIASTA, como un copiloto\n");
        context.append("6. Si te preguntan por un POI específico, da DATOS CLAVE (año, arquitecto, curiosidades)\n");
        context.append("7. Si te preguntan \"qué hay cerca\", menciona los 2-3 lugares MÁS RELEVANTES\n");
        context.append("8. Para gastronomía, recomienda PLATOS TÍPICOS de la zona\n");
        context.append("9. NO uses listas numeradas ni formato complejo, habla naturalmente\n");
        context.append("10. Si no sabes algo, sé honesto pero ofrece información relacionada\n\n");

        // Contexto de ubicación
        if (request.userLat() != null && request.userLng() != null) {
            context.append(String.format("UBICACIÓN ACTUAL: Latitud %.4f, Longitud %.4f\n",
                request.userLat(), request.userLng()));

            // Aproximar región (simplificado)
            String location = approximateLocation(request.userLat(), request.userLng());
            if (location != null) {
                context.append(String.format("Estás cerca de: %s\n", location));
            }
        }

        // Contexto de categoría seleccionada
        if (request.selectedCategory() != null && !request.selectedCategory().isBlank()) {
            String categoryContext = switch (request.selectedCategory().toLowerCase()) {
                case "history" -> "El usuario está interesado en HISTORIA. Enfócate en eventos históricos, personajes, batallas, etc.";
                case "food" -> "El usuario está interesado en GASTRONOMÍA. Recomienda platos típicos, restaurantes, productos locales.";
                case "art" -> "El usuario está interesado en ARTE Y ARQUITECTURA. Habla de estilos, artistas, obras importantes.";
                case "sports" -> "El usuario está interesado en DEPORTES Y OCIO. Sugiere actividades, parques, eventos deportivos.";
                case "geography" -> "El usuario está interesado en GEOGRAFÍA. Describe paisajes, formaciones naturales, ecosistemas.";
                case "industry" -> "El usuario está interesado en INDUSTRIA Y AGRICULTURA. Menciona productos locales, tradiciones artesanales.";
                default -> "";
            };

            if (!categoryContext.isEmpty()) {
                context.append(categoryContext).append("\n");
            }
        }

        // POIs cercanos
        if (request.nearbyPois() != null && !request.nearbyPois().isEmpty()) {
            context.append("\nPUNTOS DE INTERÉS CERCANOS:\n");

            int count = 0;
            for (Map<String, Object> poi : request.nearbyPois()) {
                if (count >= 10) break; // Máximo 10 POIs

                String name = (String) poi.get("name");
                String category = (String) poi.get("category");
                Object distanceObj = poi.get("distance");
                Double distance = distanceObj instanceof Number
                    ? ((Number) distanceObj).doubleValue()
                    : null;

                context.append(String.format("- %s (%s)%s\n",
                    name,
                    category,
                    distance != null ? String.format(" - %.0f metros", distance) : ""
                ));

                count++;
            }
        }

        context.append("\nRECUERDA: Responde como si hablaras con un amigo en el coche. ");
        context.append("Breve, interesante y sin tecnicismos innecesarios.");

        return context.toString();
    }

    /**
     * Aproxima la ubicación a ciudades principales de España
     */
    private String approximateLocation(double lat, double lng) {
        if (lat >= 40.3 && lat <= 40.5 && lng >= -3.8 && lng <= -3.6) {
            return "Madrid";
        } else if (lat >= 39.4 && lat <= 39.5 && lng >= -0.4 && lng <= -0.3) {
            return "Valencia";
        } else if (lat >= 41.3 && lat <= 41.5 && lng >= 2.0 && lng <= 2.2) {
            return "Barcelona";
        } else if (lat >= 37.3 && lat <= 37.5 && lng >= -6.0 && lng <= -5.9) {
            return "Sevilla";
        } else if (lat >= 39.8 && lat <= 40.0 && lng >= -4.1 && lng <= -4.0) {
            return "Toledo";
        } else if (lat >= 43.2 && lat <= 43.4 && lng >= -3.0 && lng <= -2.8) {
            return "Bilbao";
        } else if (lat >= 36.7 && lat <= 36.8 && lng >= -4.5 && lng <= -4.3) {
            return "Málaga";
        }
        return null;
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
