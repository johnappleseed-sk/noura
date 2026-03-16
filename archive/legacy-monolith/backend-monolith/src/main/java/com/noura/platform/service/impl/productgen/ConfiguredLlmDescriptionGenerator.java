package com.noura.platform.service.impl.productgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConfiguredLlmDescriptionGenerator implements ProductDescriptionGenerator {

    private final AppProperties appProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    public boolean isAvailable() {
        AppProperties.ProductGenerator.Llm llm = appProperties.getProductGenerator().getLlm();
        return llm.isEnabled()
                && llm.getEndpoint() != null
                && !llm.getEndpoint().isBlank()
                && llm.getApiKey() != null
                && !llm.getApiKey().isBlank()
                && llm.getModel() != null
                && !llm.getModel().isBlank();
    }

    @Override
    public String generate(ProductDescriptionPrompt prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("LLM description generator is not configured.");
        }

        AppProperties.ProductGenerator.Llm llm = appProperties.getProductGenerator().getLlm();
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(Math.max(llm.getTimeoutMs(), 1000)))
                .setReadTimeout(Duration.ofMillis(Math.max(llm.getTimeoutMs(), 1000)))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llm.getApiKey());

        Map<String, Object> body = Map.of(
                "model", llm.getModel(),
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a professional copywriter."),
                        Map.of("role", "user", "content", buildPrompt(prompt))
                )
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                llm.getEndpoint(),
                new HttpEntity<>(body, headers),
                String.class
        );
        String payload = response.getBody();
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("LLM response is empty.");
        }
        return extractContent(payload);
    }

    private String extractContent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = contentNode.asText(null);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("LLM response missing message content.");
            }
            return content.trim();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse LLM response.", ex);
        }
    }

    private String buildPrompt(ProductDescriptionPrompt prompt) {
        String name = defaultText(prompt.productName(), "Unknown product");
        String category = defaultText(prompt.category(), "General");
        String brand = defaultText(prompt.brand(), "Noura");
        String audience = defaultText(prompt.targetAudience(), "online shoppers");

        return """
                Generate a compelling e-commerce product description.

                Product Information:
                Name: %s
                Category: %s
                Brand: %s
                Target Audience: %s

                Requirements:
                - 100-200 words
                - Highlight benefits and features
                - Friendly yet professional tone
                - Suitable for an online store product page.
                """.formatted(name, category, brand, audience);
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
