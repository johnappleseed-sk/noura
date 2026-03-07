package com.noura.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Api api = new Api();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Auth auth = new Auth();
    private Notifications notifications = new Notifications();
    private Search search = new Search();
    private Kafka kafka = new Kafka();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Api {
        private String versionPrefix = "/api/v1";
    }

    @Getter
    @Setter
    public static class Jwt {
        private long accessTokenValidityMinutes = 30;
        private long refreshTokenValidityDays = 14;
        private String secret;
        private String issuer;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Auth {
        private String b2bEmailPattern;
    }

    @Getter
    @Setter
    public static class Notifications {
        private String redisChannel = "notifications.channel";
    }

    @Getter
    @Setter
    public static class Search {
        private boolean elasticEnabled;
    }

    @Getter
    @Setter
    public static class Kafka {
        private boolean enabled;
        private String topicOrderCreated = "orders.created";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private long capacity = 120;
        private long refillTokens = 120;
        private long refillMinutes = 1;
        private long keyTtlMinutes = 30;
        private int maxKeys = 10_000;
        private boolean trustForwardedHeaders;
        private String forwardedIpHeader = "X-Forwarded-For";
        private String trustedProxyAddresses = "127.0.0.1,::1";
    }
}
