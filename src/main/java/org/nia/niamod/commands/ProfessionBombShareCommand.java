package org.nia.niamod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.core.components.Models;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@UtilityClass
public class ProfessionBombShareCommand {
    private static final Set<BombType> PROFESSION_BOMBS = EnumSet.of(
            BombType.PROFESSION_SPEED,
            BombType.PROFESSION_XP
    );
    private static final int MAX_GUILD_MESSAGE_LENGTH = 240;

    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("activeprofs").executes(ctx -> share(ctx.getSource()));
    }

    private static int share(FabricClientCommandSource source) {
        List<BombInfo> bombs = professionBombs();
        if (bombs.isEmpty()) {
            source.sendError(Component.literal("No active profession speed/xp bombs currently tracked"));
            return 0;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();

        guildMessages(bombs).forEach(message -> connection.sendCommand("g " + message));
        return 1;
    }

    private static List<BombInfo> professionBombs() {
        return Models.Bomb.getBombBells().stream()
                .filter(BombInfo::isActive)
                .filter(bomb -> PROFESSION_BOMBS.contains(bomb.bomb()))
                .sorted(Comparator.comparing(ProfessionBombShareCommand::serverName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(bomb -> bomb.bomb().ordinal())
                        .thenComparingLong(BombInfo::endTime))
                .toList();

    }

    private static List<String> guildMessages(List<BombInfo> bombs) {
        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder("Active Bombs: ");

        for (BombInfo bomb : bombs) {
            String entry = formatBomb(bomb);
            String separator = current.toString().equals("Active Bombs: ") ? "" : " | ";
            if (!current.toString().equals("Active Bombs: ") && current.length() + separator.length() + entry.length() > MAX_GUILD_MESSAGE_LENGTH) {
                messages.add(current.toString());
                current = new StringBuilder();
                separator = "";
            }
            current.append(separator).append(entry);
        }

        if (!current.toString().equals("Active Bombs: ")) {
            messages.add(current.toString());
        }
        return messages;
    }

    private static String formatBomb(BombInfo bomb) {
        return "%s %s %s".formatted(serverName(bomb), bomb.bomb().getDisplayName(), bomb.getRemainingString());
    }

    private static String serverName(BombInfo bomb) {
        return bomb.server() == null ? "Unknown" : bomb.server();
    }
}
