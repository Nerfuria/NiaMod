package org.nia.niamod.util;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class WebUtils {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static CompletableFuture<String> queryAPIAsync(String url) {
        return queryAPIAsyncH(url).thenApply(WebUtils::body);
    }

    public static CompletableFuture<HttpResponse<String>> queryAPIAsyncH(String url) {
        try {
            return CLIENT.sendAsync(request(url), HttpResponse.BodyHandlers.ofString())
                    .thenApply(WebUtils::responseOrThrow);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RuntimeException("API request failed", e));
        }
    }

    public static CompletableFuture<byte[]> queryBytesAsync(String url) {
        try {
            return CLIENT.sendAsync(byteRequest(url), HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(WebUtils::bytesOrThrow);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RuntimeException("API request failed", e));
        }
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .header("Accept", "application/json")
                .build();
    }

    private static HttpRequest byteRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .header("Accept", "*/*")
                .build();
    }

    private static HttpResponse<String> responseOrThrow(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response;
        }
        throw new RuntimeException("HTTP error: " + response.statusCode());
    }

    private static String body(HttpResponse<String> response) {
        return response.body();
    }

    private static byte[] bytesOrThrow(HttpResponse<byte[]> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new RuntimeException("HTTP error: " + response.statusCode());
    }
}
