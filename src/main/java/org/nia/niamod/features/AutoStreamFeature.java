package org.nia.niamod.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.BossBarNameEvent;
import org.nia.niamod.models.events.CommandSentEvent;

@SuppressWarnings("unused")
public class AutoStreamFeature extends Feature {

    private long lastSeen;
    private long lastStreamed;
    private boolean streamEnabled = false;

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                runSafe("onTick", () -> onTick(client)));
    }

    private void onTick(Minecraft mc) {
        if (isDisabled() || mc.getConnection() == null || !streamEnabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean isStreamCooldownOver = (currentTime - lastSeen) >= NyahConfig.getData().getStreamCooldown();
        boolean isCommandCooldownOver = (currentTime - lastStreamed) >= 1000;

        if (isStreamCooldownOver && isCommandCooldownOver) {
            mc.getConnection().sendCommand("stream");
            lastStreamed = currentTime;
        }
    }

    @Subscribe
    private void onBossBar(BossBarNameEvent event) {
        if (event.getTitle().getString().contains("Streamer mode enabled")) {
            lastSeen = System.currentTimeMillis();
        }
    }

    @Subscribe
    private void onCommand(CommandSentEvent event) {
        if (event.command().startsWith("stream")) {
            streamEnabled = !streamEnabled;
        }
    }
}
