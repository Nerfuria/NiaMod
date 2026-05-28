package org.nia.niamod.models.events;

import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public record GuildMapUpdateEvent() {
}
