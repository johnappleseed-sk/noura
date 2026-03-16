package com.noura.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class MediaStorageWebConfig implements WebMvcConfigurer {

    private final MediaStorageProperties mediaStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = normalizePublicPrefix(mediaStorageProperties.getPublicPathPrefix());
        Path uploadRoot = Path.of(mediaStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(uploadRoot.toUri().toString());
    }

    private String normalizePublicPrefix(String rawPrefix) {
        String normalized = rawPrefix == null ? "/uploads" : rawPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
