package org.nia.niamod.features.radiance;

import java.net.http.WebSocket;

@FunctionalInterface
public interface WebSocketCloseHandler {
    void handle(WebSocket webSocket, int statusCode, String reason);
}
