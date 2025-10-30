package com.nomad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nomad.dto.RealtimeSessionResponse;
import com.nomad.dto.realtime.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class RealtimeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeService.class);
    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public RealtimeService(WebClient.Builder webClientBuilder,
                           @Value("${api.openai.key:PLACEHOLDER_OPENAI_KEY}") String apiKey,
                           ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
            .baseUrl("https://api.openai.com/v1/realtime")
            .build();
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    public RealtimeSessionResponse createSession() {
        if ("PLACEHOLDER_OPENAI_KEY".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key not configured");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI API key not configured");
        }

        try {
            log.info("Creating OpenAI Realtime session");

            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "gpt-4o-realtime-preview-2024-12-17");
            requestBody.put("voice", "alloy");

            // Modalities
            ArrayNode modalities = objectMapper.createArrayNode();
            modalities.add("audio");
            modalities.add("text");
            requestBody.set("modalities", modalities);

            // Instructions
            String instructions = """
                Eres un asistente de viaje experto que ayuda a los usuarios a descubrir lugares de interés.

                IMPORTANTE:
                - Siempre cita las fuentes de información (OSM, Wikidata, Wikipedia, Google Places, etc.)
                - Si necesitas información sobre un lugar específico, usa las herramientas disponibles
                - Cuando falte contexto sobre ubicación o detalles de un POI, pregunta al usuario o usa las tools
                - Sé conciso pero informativo
                - Responde en el idioma del usuario

                HERRAMIENTAS DISPONIBLES:
                1. poi_nearby: Busca POIs cercanos a unas coordenadas (requiere lat, lng, radius)
                2. poi_context: Obtiene información detallada de un POI específico (requiere name, lat, lng)
                """;
            requestBody.put("instructions", instructions);

            // Tools
            ArrayNode tools = objectMapper.createArrayNode();
            tools.add(objectMapper.valueToTree(ToolDefinition.poiNearby()));
            tools.add(objectMapper.valueToTree(ToolDefinition.poiContext()));
            requestBody.set("tools", tools);

            // Call OpenAI API
            JsonNode response = webClient.post()
                .uri("/sessions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                    .filter(this::shouldRetry)
                    .doBeforeRetry(retrySignal ->
                        log.warn("Retrying OpenAI Realtime API, attempt: {}", retrySignal.totalRetries() + 1)))
                .block();

            if (response == null || !response.has("client_secret")) {
                log.error("Invalid response from OpenAI Realtime API: missing client_secret");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid response from OpenAI");
            }

            JsonNode clientSecret = response.get("client_secret");
            if (!clientSecret.has("value")) {
                log.error("Invalid client_secret structure from OpenAI Realtime API");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid client_secret structure");
            }

            String token = clientSecret.get("value").asText();
            log.info("OpenAI Realtime session created successfully, token length: {}", token.length());

            return new RealtimeSessionResponse(token);

        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("OpenAI API rate limit exceeded: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, please try again later");

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("OpenAI API authentication failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OpenAI API key");

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI API error: " + e.getMessage());

        } catch (Exception e) {
            log.error("Failed to create OpenAI Realtime session: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create session");
        }
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            int status = webEx.getStatusCode().value();
            // Retry on server errors (5xx) but not on client errors (4xx) except 429
            return status == 429 || status >= 500;
        }
        // Retry on timeout
        return throwable instanceof java.util.concurrent.TimeoutException;
    }
}
