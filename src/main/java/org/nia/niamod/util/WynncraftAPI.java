package org.nia.niamod.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.api.GuildResponse;
import org.nia.niamod.models.api.TerritoryResponse;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@UtilityClass
public class WynncraftAPI {
    private static final Gson gson = new Gson();

    public static CompletableFuture<Map<String, TerritoryResponse>> territoryResponseAsync() {
        Type type = TypeToken.getParameterized(Map.class, String.class, TerritoryResponse.class).getType();
        return WebUtils.queryAPIAsync(NyahConfig.getData().getApiBase() + "guild/list/territory")
                .thenApply(json -> {
                    Map<String, TerritoryResponse> response = gson.fromJson(json, type);
                    return response == null ? Map.of() : response;
                });
    }

    public static CompletableFuture<Map<String, TerritoryResponse>> ownedTerritoryResponseAsync(String guildName) {
        String guildKey = normalize(guildName);
        if (guildKey.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        return territoryResponseAsync()
                .thenApply(response -> response.entrySet().stream()
                        .filter(entry -> isOwnedByGuild(entry.getValue(), guildKey))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public static CompletableFuture<GuildResponse> guildResponseAsync(String guildName) {
        return WebUtils.queryAPIAsync(guildUrl(guildName))
                .thenApply(json -> gson.fromJson(json, GuildResponse.class));
    }

    private static String guildUrl(String guildName) {
        String safeGuildName = guildName == null ? "" : guildName;
        String encodedGuildName = URLEncoder.encode(safeGuildName, StandardCharsets.UTF_8).replace("+", "%20");
        return NyahConfig.getData().getApiBase() + "guild/%s?identifier=username".formatted(encodedGuildName);
    }

    private static boolean isOwnedByGuild(TerritoryResponse response, String guildKey) {
        return response != null
                && response.guild() != null
                && normalize(response.guild().name()).equals(guildKey);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
