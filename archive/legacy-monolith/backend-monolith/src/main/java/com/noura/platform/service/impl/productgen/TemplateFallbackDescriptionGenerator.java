package com.noura.platform.service.impl.productgen;

import org.springframework.stereotype.Component;

@Component
public class TemplateFallbackDescriptionGenerator implements ProductDescriptionGenerator {

    @Override
    public String generate(ProductDescriptionPrompt prompt) {
        String name = defaultText(prompt.productName(), "This product");
        String category = defaultText(prompt.category(), "everyday essentials");
        String brand = defaultText(prompt.brand(), "Noura");
        String audience = defaultText(prompt.targetAudience(), "online shoppers");

        return "Meet " + name + ", a thoughtfully crafted " + category + " from " + brand + " designed for "
                + audience + ". Built with practical, day-to-day performance in mind, it combines dependable quality "
                + "with a clean, versatile design that fits seamlessly into modern routines. The materials and finish "
                + "are selected for durability, while the user-focused details help make each interaction simple and "
                + "efficient. Whether you are upgrading your current setup or buying for the first time, " + name
                + " delivers a balanced mix of value, comfort, and reliability. It is easy to integrate into both "
                + "home and professional environments, making it a smart choice for customers who want consistent "
                + "results and long-term confidence in their purchase.";
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
