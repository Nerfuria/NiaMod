package org.nia.niamod.features;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.gui.screen.PartyScreen;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.gparty.RaidMode;
import org.nia.niamod.models.gparty.SpeedMode;
import org.nia.niamod.models.gparty.WorldMode;

import javax.annotation.Nullable;

public class PartyFeature extends Feature {
    private final Minecraft client = Minecraft.getInstance();

    private @Nullable @Getter RaidMode currentRaid = null;
    private @Nullable @Getter SpeedMode currentSpeed = null;
    private @Nullable @Getter WorldMode currentWorld = null;
    private @Nullable @Getter String raidNote = null;

    public PartyFeature() { }

    @Override
    public void init() {
        KeybindManager.registerKeybinding("Create a guild party", GLFW.GLFW_KEY_P,
            safeRunnable("open_party_screen", this::openScreen)
        );
    }

    public void setNewRaidParty(
            @NonNull RaidMode raid,
            @NonNull SpeedMode speed,
            @NonNull WorldMode world,
            @NonNull String note
    ) {
        currentRaid = raid;
        currentSpeed = speed;
        currentWorld = world;
        raidNote = note;
    }

    public void clearRaidParty() {
        currentRaid = null;
        currentSpeed = null;
        currentWorld = null;
        raidNote = null;
    }

    public void openScreen() {
        client.setScreen(new PartyScreen(client.screen, this));
    }
}
