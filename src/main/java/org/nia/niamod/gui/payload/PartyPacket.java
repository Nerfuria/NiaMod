package org.nia.niamod.gui.payload;

import org.nia.niamod.commands.choices.RaidType;
import org.nia.niamod.commands.choices.SpeedType;
import org.nia.niamod.commands.choices.WorldType;

public record PartyPacket(
        RaidType raidType,
        SpeedType speedType,
        WorldType worldType,
        String note
) { }