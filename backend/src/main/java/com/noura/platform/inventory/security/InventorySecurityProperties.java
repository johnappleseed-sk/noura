package com.noura.platform.inventory.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "inventory.security")
public class InventorySecurityProperties {

    private Jwt jwt = new Jwt();
    private boolean devHeaderAuthEnabled;
    private SeedAdmin seedAdmin = new SeedAdmin();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenMinutes = 720;
        private long allowedClockSkewSeconds = 60;
    }

    @Getter
    @Setter
    public static class SeedAdmin {
        private boolean enabled = true;
        private String username;
        private String email;
        private String password;
        private String fullName;
    }
}
