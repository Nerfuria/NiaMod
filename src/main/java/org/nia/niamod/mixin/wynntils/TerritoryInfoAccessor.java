package org.nia.niamod.mixin.wynntils;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResourceValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TerritoryInfo.class)
public interface TerritoryInfoAccessor {
    @Accessor("guildName")
    void setGuildName(String value);

    @Accessor("guildPrefix")
    void setGuildPrefix(String value);

    @Accessor("treasury")
    void setTreasury(GuildResourceValues value);

    @Accessor("defences")
    void setDefences(GuildResourceValues value);
}
