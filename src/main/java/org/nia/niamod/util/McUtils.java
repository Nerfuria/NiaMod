package org.nia.niamod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class McUtils {
    public static void sendMessageToClient(String message) {
        com.wynntils.utils.mc.McUtils.sendMessageToClient(Component.literal(message));
    }

    public static void logMessageToClient(String message) {
        com.wynntils.utils.mc.McUtils.sendMessageToClient(
                Component.literal("[Nia Mod] ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(message))
        );
    }
}
