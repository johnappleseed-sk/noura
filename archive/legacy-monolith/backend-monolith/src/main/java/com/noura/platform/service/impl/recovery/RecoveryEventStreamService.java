package com.noura.platform.service.impl.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal server-sent event hub for the admin recovery center.
 */
@Service
public class RecoveryEventStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryEventStreamService.class);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ignored) -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(String type, Object payload) {
        if (emitters.isEmpty() || payload == null) {
            return;
        }

        List<SseEmitter> snapshot = List.copyOf(emitters);
        Map<String, Object> envelope = Map.of(
                "type", type,
                "occurredAt", Instant.now().toString(),
                "payload", payload
        );

        for (SseEmitter emitter : snapshot) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(envelope, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
                LOGGER.debug("Removing failed recovery SSE emitter: {}", exception.getMessage());
            }
        }
    }
}
