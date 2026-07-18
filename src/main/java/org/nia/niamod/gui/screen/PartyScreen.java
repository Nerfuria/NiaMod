package org.nia.niamod.gui.screen;

import lombok.NonNull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.nia.niamod.features.PartyFeature;

import javax.annotation.Nullable;

public class PartyScreen extends Screen {
    @NonNull private final PartyFeature feature;
    @Nullable private final Screen parent;

    public PartyScreen(@Nullable Screen parent, @NonNull PartyFeature feature) {
        super(Component.literal("Party Screen"));
        this.feature = feature;
        this.parent = parent;

        System.out.println("Raid mode: " + feature.getCurrentRaid());
        System.out.println("Speed mode: " + feature.getCurrentSpeed());
        System.out.println("World mode: " + feature.getCurrentWorld());
        System.out.println("Note: " + feature.getRaidNote());
    }

    @Override
    protected void init() {
        // TODO: Change sample button later
        Button buttonWidget = Button.builder(Component.literal("Hello!"), (it) -> {
            minecraft.getToastManager().addToast(
                    SystemToast.multiline(
                            minecraft,
                            SystemToast.SystemToastId.NARRATOR_TOGGLE,
                            Component.nullToEmpty("Hello!"),
                            Component.nullToEmpty("This is a toast.0")
                    )
            );
        }).build();

        addRenderableWidget(buttonWidget);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        // TODO: Change render later
        guiGraphics.drawString(
                font,
                "Special button",
                40,
                40 - font.lineHeight - 10,
                0xFFFFFFFF,
                true
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
