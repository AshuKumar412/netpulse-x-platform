package com.netpulse.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
public class WebSocketTestController {

    @MessageMapping("/ping")
    @SendTo("/topic/status")
    public Map<String, Object> handlePing(Map<String, String> payload) {
        String clientMessage = payload != null ? payload.getOrDefault("message", "ping") : "ping";
        return Map.of(
                "status", "CONNECTED",
                "echo", clientMessage,
                "timestamp", Instant.now().toString(),
                "service", "NetPulse X Real-Time Gateway"
        );
    }
}
