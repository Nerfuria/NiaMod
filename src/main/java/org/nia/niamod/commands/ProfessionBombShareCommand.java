package org.nia.niamod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@UtilityClass
public class ProfessionBombShareCommand {
    private static final Set<BombType> PROFESSION_BOMBS = EnumSet.of(
            BombType.PROFESSION_SPEED,
            BombType.PROFESSION_XP
    );

    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("activeprofs").executes(ctx -> share(ctx.getSource()));
    }

    private static int share(FabricClientCommandSource source) {
        List<BombInfo> bombs = BombsCommand.bombs(PROFESSION_BOMBS);

        if (bombs.isEmpty()) {
            source.sendError(Component.literal("No active profession speed/xp bombs currently tracked"));
            return 0;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        BombsCommand.guildMessages(bombs).forEach(message -> connection.sendCommand("g " + message));

        return 1;
    }
}
