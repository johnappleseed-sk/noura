package com.noura.platform.service.impl.productgen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDescriptionGenerationService {

    private final ConfiguredLlmDescriptionGenerator configuredLlmDescriptionGenerator;
    private final TemplateFallbackDescriptionGenerator templateFallbackDescriptionGenerator;

    public String generate(ProductDescriptionPrompt prompt) {
        if (configuredLlmDescriptionGenerator.isAvailable()) {
            try {
                return configuredLlmDescriptionGenerator.generate(prompt);
            } catch (Exception ex) {
                log.warn("LLM description generation failed; falling back to template text.", ex);
            }
        }
        return templateFallbackDescriptionGenerator.generate(prompt);
    }
}
