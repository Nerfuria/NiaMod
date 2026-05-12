package org.nia.niamod.features.radiance;

import lombok.RequiredArgsConstructor;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class RadianceWebSocketListener implements WebSocket.Listener {
    private final StringBuilder textBuffer = new StringBuilder();
    private final Consumer<WebSocket> openHandler;
    private final BiConsumer<WebSocket, String> messageHandler;
    private final WebSocketCloseHandler closeHandler;
    private final BiConsumer<WebSocket, Throwable> errorHandler;

    @Override
    public void onOpen(WebSocket webSocket) {
        openHandler.accept(webSocket);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (last) {
            messageHandler.accept(webSocket, completePayload(data));
        } else {
            textBuffer.append(data);
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        closeHandler.handle(webSocket, statusCode, reason);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        errorHandler.accept(webSocket, error);
    }

    private String completePayload(CharSequence data) {
        if (textBuffer.isEmpty()) {
            return data.toString();
        }

        textBuffer.append(data);
        String payload = textBuffer.toString();
        textBuffer.setLength(0);
        return payload;
    }
}
