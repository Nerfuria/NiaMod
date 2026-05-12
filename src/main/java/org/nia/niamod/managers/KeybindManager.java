package org.nia.niamod.managers;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class KeybindManager {
    private static KeyMapping.Category CATEGORY;
    private static HashMap<KeyMapping, Runnable> keybinds;

    public static void init() {
        CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("niamod", "config"));

        keybinds = new HashMap<>();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                for (Map.Entry<KeyMapping, Runnable> entry : keybinds.entrySet()) {
                    if (entry.getKey().consumeClick()) {
                        entry.getValue().run();
                    }
                }
            }
        });
    }

    public static void registerKeybinding(String name, int key, Runnable action) {
        keybinds.put(
                KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY)),
                action
        );
    }
}
