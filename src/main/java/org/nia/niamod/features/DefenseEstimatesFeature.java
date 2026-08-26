package org.nia.niamod.features;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.services.map.pois.TerritoryPoi;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.defense.DefenseEstimate;
import org.nia.niamod.models.events.GuildMapResourcesUpdateEvent;
import org.nia.niamod.models.events.TerritoryTooltipHeightEvent;
import org.nia.niamod.models.events.TerritoryTooltipRenderEvent;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.util.DefenseEstimateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefenseEstimatesFeature extends Feature {
    private final Map<String, DefenseEstimate> estimateCache = new HashMap<>(16);

    private DefenseEstimate estimate(String territoryName, TerritoryInfo territoryInfo) {
        if (territoryInfo == null) {
            return DefenseEstimate.EMPTY;
        }
        DefenseEstimate cached = estimateCache.get(territoryName);
        if (cached != null) {
            return cached;
        }

        DefenseEstimate estimate = DefenseEstimateUtils.estimate(territoryName, territoryInfo);
        estimateCache.put(territoryName, estimate);
        return estimate;
    }

    @Subscribe
    public void renderTooltip(TerritoryTooltipRenderEvent event) {
        for (String line : tooltipLines(event.territoryPoi())) {
            Render2D.tooltipLine(event.guiGraphics(), event.xOffset(), event.renderYOffset(), line);
        }
    }

    @Subscribe
    public void increaseTooltipHeight(TerritoryTooltipHeightEvent event) {
        try {
            event.addHeight(tooltipLineCount(event.getTerritoryPoi()) * 10.0F);
        } catch (RuntimeException exception) {
            NiamodClient.LOGGER.warn("Failed to calculate defense estimate tooltip height", exception);
        }
    }

    private List<String> tooltipLines(TerritoryPoi territoryPoi) {
        return DefenseEstimateUtils.tooltipLines(
                territoryPoi.getName(),
                estimate(territoryPoi.getName(), territoryPoi.getTerritoryInfo()));
    }

    private int tooltipLineCount(TerritoryPoi territoryPoi) {
        return tooltipLines(territoryPoi).size();
    }

    public void init() {
        estimateCache.clear();
        NiaEventBus.subscribe(this);
    }

    public void clearCache() {
        estimateCache.clear();
    }

    @Subscribe
    public void onGuildMapResourcesUpdate(GuildMapResourcesUpdateEvent event) {
        this.clearCache();
    }
}
