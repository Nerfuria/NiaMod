package org.nia.niamod.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.nia.niamod.gui.payload.PartyPacket;

public class PartyScreen extends Screen {
    private PartyPacket packet;

    public PartyScreen() {
        super(Component.literal("Party Screen"));
    }

    public PartyScreen(PartyPacket packet) {
        super(Component.literal("Party Screen"));
        this.packet = packet;
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

        System.out.println(packet.toString());

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
}
