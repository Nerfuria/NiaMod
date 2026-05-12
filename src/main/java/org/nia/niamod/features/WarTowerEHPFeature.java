package org.nia.niamod.features;

import net.minecraft.network.chat.Component;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.BossBarNameEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class WarTowerEHPFeature extends Feature {
    private static final Pattern TOWER_PATTERN =
            Pattern.compile(
                    "§3\\[([A-Za-z]{3,4})] §b([A-Za-z ]*)§7 - §4❤ ([0-9]+)§7 \\(§6([0-9.]+)%§7\\) - §c☠"
                            + " ([0-9]+)-([0-9]+)§7 \\(§b([0-9]\\.[0-9]*)x§7\\)");

    private Component replaceEHP(Component text) {
        Matcher matcher = TOWER_PATTERN.matcher(text.getString());
        if (!matcher.matches()) {
            return text;
        }
        String tag = matcher.group(1);
        String name = matcher.group(2);
        int health = Integer.parseInt(matcher.group(3));
        double defencePercent = Double.parseDouble(matcher.group(4));
        int lowDamage = Integer.parseInt(matcher.group(5));
        int highDamage = Integer.parseInt(matcher.group(6));
        double attackSpeed = Double.parseDouble(matcher.group(7));
        int effectiveHealth = (int) (health / (1 - (defencePercent / 100.0)));
        return Component.nullToEmpty(
                "§3["
                        + tag
                        + "] §b"
                        + name
                        + "§7 - §4❤ "
                        + effectiveHealth
                        + "§7 - §c☠ "
                        + lowDamage
                        + "-"
                        + highDamage
                        + "§7 (§b"
                        + attackSpeed
                        + "x§7)");
    }

    @Subscribe
    public void onBossBarName(BossBarNameEvent event) {
        event.setTitle(replaceEHP(event.getTitle()));
    }

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
    }
}
