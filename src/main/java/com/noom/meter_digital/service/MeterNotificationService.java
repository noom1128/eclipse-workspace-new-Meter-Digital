package com.noom.meter_digital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MeterNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MeterNotificationService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE Connection Established"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastTelemetryUpdate(Object telemetryData) {
        // Send via SSE
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("telemetry").data(telemetryData));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);

        // Send via WebSocket STOMP topic if configured
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/meter-telemetry", telemetryData);
            } catch (Exception e) {
                log.warn("Failed to send WebSocket message: {}", e.getMessage());
            }
        }
    }
}
