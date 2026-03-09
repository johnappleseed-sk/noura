package com.noura.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.service.impl.RedisNotificationSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    private final AppProperties appProperties;

    /**
     * Executes redis template.
     *
     * @param connectionFactory The connection factory value.
     * @param objectMapper The object mapper value.
     * @return The result of redis template.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        /**
         * Executes string redis serializer.
         *
         * @param param1 The param1 value.
         * @return The result of string redis serializer.
         */
        template.setKeySerializer(new StringRedisSerializer());
        /**
         * Executes generic jackson2 json redis serializer.
         *
         * @param param1 The param1 value.
         * @return The result of generic jackson2 json redis serializer.
         */
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    /**
     * Executes notification topic.
     *
     * @return The result of notification topic.
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic(appProperties.getNotifications().getRedisChannel());
    }

    /**
     * Lists listener adapter.
     *
     * @param subscriber The subscriber value.
     * @return The result of listener adapter.
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisNotificationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * Executes redis container.
     *
     * @param connectionFactory The connection factory value.
     * @param listenerAdapter The listener adapter value.
     * @param topic The topic value.
     * @return The result of redis container.
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic topic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic);
        return container;
    }
}
