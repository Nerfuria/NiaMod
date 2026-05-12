package org.nia.niamod.models.events;

import net.minecraft.network.chat.ClickEvent;
import org.jetbrains.annotations.NotNull;

public record ExecuteRunnableClickEvent(Runnable runnable) implements ClickEvent {
    public @NotNull Action action() {
        return Action.CUSTOM;
    }
}
