package org.nia.niamod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.wynntils.core.components.Models;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BombsCommand {
    private static final int MAX_GUILD_MESSAGE_LENGTH = 240;

    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("activebombs")
                .then(argument("type", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining();
                            int i = remaining.lastIndexOf(' ');

                            String current = remaining.substring(i + 1);
                            SuggestionsBuilder currentBuilder =
                                    builder.createOffset(builder.getStart() + i + 1);

                            for (BombType type : BombType.values()) {
                                if (type.name().toLowerCase()
                                        .startsWith(current.toLowerCase())) {
                                    currentBuilder.suggest(type.name());
                                }
                            }

                            return currentBuilder.buildFuture();
                        })
                        .executes(ctx -> share(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "type")
                        )));
    }

    private static int share(FabricClientCommandSource source, String input) {
        Set<BombType> types = parseBombTypes(input);
        if (types.isEmpty()) {
            source.sendError(Component.literal("No bombs said >:("));
            return 0;
        }
        List<BombInfo> bombs = bombs(types);

        if (bombs.isEmpty()) {
            source.sendError(Component.literal("No active bombs of those type"));
            return 0;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        guildMessages(bombs).forEach(message -> connection.sendCommand("g " + message));

        return 1;
    }

    private static Set<BombType> parseBombTypes(String input) {
        Set<BombType> types = EnumSet.noneOf(BombType.class);

        for (String value : input.trim().split("\\s+")) {
            try {
                types.add(BombType.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return types;
    }

    private static List<BombInfo> bombs(Set<BombType> types) {
        return Models.Bomb.getBombBells().stream()
                .filter(BombInfo::isActive)
                .filter(bomb -> types.contains(bomb.bomb()))
                .sorted(Comparator
                        .comparing(BombsCommand::serverName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(bomb -> bomb.bomb().ordinal())
                        .thenComparingLong(BombInfo::endTime))
                .toList();
    }

    private static List<String> guildMessages(List<BombInfo> bombs) {
        List<String> messages = new ArrayList<>();

        if (bombs.isEmpty()) {
            return messages;
        }

        StringBuilder current = new StringBuilder("Active Bombs: ");
        String separator = "";

        for (BombInfo bomb : bombs) {
            String entry = formatBomb(bomb);

            if (current.length() + separator.length() + entry.length()
                    > MAX_GUILD_MESSAGE_LENGTH) {
                messages.add(current.toString());
                current = new StringBuilder();
                separator = "";
            }

            current.append(separator).append(entry);
            separator = " | ";
        }

        if (!current.isEmpty()) {
            messages.add(current.toString());
        }

        return messages;
    }

    private static String formatBomb(BombInfo bomb) {
        return "%s %s %s".formatted(
                serverName(bomb),
                bomb.bomb().getDisplayName(),
                bomb.getRemainingString()
        );
    }

    private static String serverName(BombInfo bomb) {
        return bomb.server() == null ? "Unknown" : bomb.server();
    }
}