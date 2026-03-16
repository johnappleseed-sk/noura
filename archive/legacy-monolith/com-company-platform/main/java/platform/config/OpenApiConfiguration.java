package com.company.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean(name = "companyPlatformOpenApi")
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI companyPlatformOpenApi(PlatformProperties properties) {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getDocs().getTitle())
                        .description(properties.getDocs().getDescription())
                        .version(properties.getDocs().getVersion())
                        .contact(new Contact().name("Platform Engineering")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(
                                schemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
