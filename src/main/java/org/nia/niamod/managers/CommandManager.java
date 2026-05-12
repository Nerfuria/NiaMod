package org.nia.niamod.managers;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.nia.niamod.commands.NiamodCommand;
import org.nia.niamod.commands.ProfessionBombShareCommand;
import org.nia.niamod.commands.RadianceSyncCommand;

@UtilityClass
public class CommandManager {
    private static boolean registered;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(RadianceSyncCommand.command());
            dispatcher.register(ProfessionBombShareCommand.command());
            dispatcher.register(NiamodCommand.command());
        });
    }
}
