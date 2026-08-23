package org.nia.niamod.functions;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.util.TerritoryUtils;

public final class WarFunctions {

    public static class ResTickFunction extends NiaFunction<Integer> {
        @Override
        public Integer getValue(FunctionArguments arguments) {
            return FeatureManager.getResTickFeature().getTimeUntilResTick();
        }
    }

    public static class HqTimerFunction extends NiaFunction<String> {
        @Override
        public String getValue(FunctionArguments arguments) {
            var hq = TerritoryUtils.getHQ(Models.Guild.getGuildName());
            if (hq.isEmpty())
                return "";
            var poi = Models.Territory.getTerritoryPoiFromAdvancement(hq.get());
            return poi.getTerritoryProfile().getReadableRelativeTimeAcquired();
        }
    }
}
