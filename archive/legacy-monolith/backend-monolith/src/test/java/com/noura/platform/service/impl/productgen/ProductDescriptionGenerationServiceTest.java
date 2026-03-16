package com.noura.platform.service.impl.productgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductDescriptionGenerationServiceTest {

    private ConfiguredLlmDescriptionGenerator configuredGenerator;
    private TemplateFallbackDescriptionGenerator fallbackGenerator;
    private ProductDescriptionGenerationService service;

    @BeforeEach
    void setUp() {
        configuredGenerator = mock(ConfiguredLlmDescriptionGenerator.class);
        fallbackGenerator = mock(TemplateFallbackDescriptionGenerator.class);
        service = new ProductDescriptionGenerationService(configuredGenerator, fallbackGenerator);
    }

    @Test
    void generate_shouldUseConfiguredGeneratorWhenAvailable() {
        ProductDescriptionPrompt prompt = new ProductDescriptionPrompt("Phone", "Electronics", "Noura", "Shoppers");
        when(configuredGenerator.isAvailable()).thenReturn(true);
        when(configuredGenerator.generate(prompt)).thenReturn("Configured output");

        String result = service.generate(prompt);

        assertEquals("Configured output", result);
        verify(configuredGenerator).generate(prompt);
        verifyNoInteractions(fallbackGenerator);
    }

    @Test
    void generate_shouldFallbackWhenConfiguredGeneratorFails() {
        ProductDescriptionPrompt prompt = new ProductDescriptionPrompt("Phone", "Electronics", "Noura", "Shoppers");
        when(configuredGenerator.isAvailable()).thenReturn(true);
        when(configuredGenerator.generate(prompt)).thenThrow(new IllegalStateException("LLM down"));
        when(fallbackGenerator.generate(prompt)).thenReturn("Fallback output");

        String result = service.generate(prompt);

        assertEquals("Fallback output", result);
        verify(fallbackGenerator).generate(prompt);
    }

    @Test
    void generate_shouldFallbackWhenConfiguredGeneratorNotAvailable() {
        ProductDescriptionPrompt prompt = new ProductDescriptionPrompt("Phone", "Electronics", "Noura", "Shoppers");
        when(configuredGenerator.isAvailable()).thenReturn(false);
        when(fallbackGenerator.generate(prompt)).thenReturn("Fallback output");

        String result = service.generate(prompt);

        assertEquals("Fallback output", result);
        verify(fallbackGenerator).generate(prompt);
    }
}
