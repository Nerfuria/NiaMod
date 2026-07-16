package org.nia.niamod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.nia.niamod.commands.choices.EnumHelper;
import org.nia.niamod.commands.choices.RaidType;
import org.nia.niamod.commands.choices.SpeedType;
import org.nia.niamod.commands.choices.WorldType;

import java.util.List;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@UtilityClass
public class PartyCommand {
    private static final Minecraft client = Minecraft.getInstance();
    private static final String RAID = "raid_type";
    private static final String SPEED = "speed_type";
    private static final String WORLD = "world_type";
    private static final String NOTE = "note";

    List<String> raidList = List.of(EnumHelper.enumNames(RaidType.class));
    List<String> speedList = List.of(EnumHelper.enumNames(SpeedType.class));
    List<String> worldList = List.of(EnumHelper.enumNames(WorldType.class));

    private static final Map<String, String> exceptionMessages = Map.of(
            "BASE", "Guild party command invalid!\n",
            RAID, "Raid must be in the list " + raidList + "\n",
            SPEED, "Speed must be in the list " + speedList + "\n",
            WORLD, "World must be in the list " + worldList
    );

    // imagine how easier it could be to wrangle values with scope functions in kotlin? >_<
    public static LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("gparty")
                .executes(PartyCommand::launchPartyScreen)
                .then(argument(RAID, StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(raidList, builder))
                        .then(argument(SPEED, StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(speedList, builder))
                                .then(argument(WORLD, StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(worldList, builder))
                                        .executes(ctx -> launchParty(ctx, ""))
                                        .then(argument(NOTE, StringArgumentType.greedyString())
                                                .executes(ctx ->
                                                        launchParty(ctx, StringArgumentType.getString(ctx, NOTE))
                                                )
                                        )
                                )
                        )
                );
    }

    private static int launchPartyScreen(CommandContext<FabricClientCommandSource> ctx) {
        client.execute(() -> client.setScreen(null));
        return Command.SINGLE_SUCCESS;
    }

    private static int launchParty(
            CommandContext<FabricClientCommandSource> ctx,
            String note
    ) throws CommandSyntaxException {
        MutableComponent errorString = Component.literal(exceptionMessages.get("BASE"));

        String raidType = StringArgumentType.getString(ctx, RAID).toUpperCase();
        String speedType = StringArgumentType.getString(ctx, SPEED).toLowerCase();
        String worldType = StringArgumentType.getString(ctx, WORLD).toUpperCase();

        if (!raidList.contains(raidType)) errorString.append(exceptionMessages.get(RAID));
        if (!speedList.contains(speedType)) errorString.append(exceptionMessages.get(SPEED));
        if (!worldList.contains(worldType)) errorString.append(exceptionMessages.get(WORLD));

        if (!errorString.getString().equals(exceptionMessages.get("BASE")))
            throw new SimpleCommandExceptionType(errorString).create();

        client.execute(() -> {
            client.setScreen(null);
        });

        return Command.SINGLE_SUCCESS;
    }

    //        client.execute {
    //            client.setScreen(PartyScreen(
    //                packet = PartyPacket(
    //                    RaidSuggestions.RaidType.valueOf(raidType),
    //                    SpeedSuggestions.SpeedType.valueOf(speedType),
    //                    WorldSuggestions.WorldType.valueOf(worldType),
    //                    note
    //                )
    //            ))
    //        }
}
