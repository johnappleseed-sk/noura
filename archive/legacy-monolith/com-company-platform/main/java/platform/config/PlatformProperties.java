package com.company.platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "company.platform")
public class PlatformProperties {

    @NotBlank
    private String name = "Enterprise Commerce Platform";

    @NotBlank
    private String environment = "dev";

    @Valid
    private Api api = new Api();

    @Valid
    private Docs docs = new Docs();

    @Valid
    private Security security = new Security();

    @Getter
    @Setter
    public static class Api {

        @NotBlank
        private String versionPrefix = "/api/v1";
    }

    @Getter
    @Setter
    public static class Docs {

        @NotBlank
        private String title = "Enterprise Commerce Platform API";

        @NotBlank
        private String description = "Base Spring Boot foundation for the enterprise commerce platform.";

        @NotBlank
        private String version = "v0";
    }

    @Getter
    @Setter
    public static class Security {

        private boolean enabled;

        private boolean permitAll = true;

        @NotEmpty
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3001"
        ));
    }
}
