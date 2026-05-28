package org.nia.niamod.features;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.CappedValue;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.defense.DefenseEstimate;
import org.nia.niamod.models.defense.DefenseEstimateCacheKey;
import org.nia.niamod.models.events.GuildMapUpdateEvent;
import org.nia.niamod.models.events.TerritoryTooltipHeightEvent;
import org.nia.niamod.models.events.TerritoryTooltipRenderEvent;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.util.DefenseEstimateUtils;
import org.nia.niamod.util.TerritoryUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefenseEstimatesFeature extends Feature {
    private static final int MAX_CACHE_SIZE = 512;
    private final Map<DefenseEstimateCacheKey, DefenseEstimate> estimateCache =
            new LinkedHashMap<>(16, 0.75f, true);

    private DefenseEstimate estimate(String territoryName, TerritoryInfo territoryInfo) {
        if (territoryInfo == null) {
            return DefenseEstimate.EMPTY;
        }
        DefenseEstimateCacheKey storedResources = cacheKey(territoryInfo, territoryName);
        DefenseEstimate cached = estimateCache.get(storedResources);
        if (cached != null) {
            return cached;
        }
        DefenseEstimate estimate = DefenseEstimateUtils.estimate(territoryName, territoryInfo);
        estimateCache.put(storedResources, estimate);
        trimEstimateCache();
        return estimate;
    }

    private DefenseEstimateCacheKey cacheKey(TerritoryInfo territoryInfo, String territoryName) {
        return new DefenseEstimateCacheKey(
                currentStorage(territoryInfo, GuildResource.EMERALDS),
                currentStorage(territoryInfo, GuildResource.ORE),
                currentStorage(territoryInfo, GuildResource.CROPS),
                currentStorage(territoryInfo, GuildResource.FISH),
                currentStorage(territoryInfo, GuildResource.WOOD),
                TerritoryUtils.getMapTick(),
                territoryName);
    }

    private int currentStorage(TerritoryInfo territoryInfo, GuildResource resource) {
        CappedValue storage = territoryInfo.getStorage(resource);
        return storage == null ? 0 : storage.current();
    }

    private void trimEstimateCache() {
        while (estimateCache.size() > MAX_CACHE_SIZE) {
            DefenseEstimateCacheKey oldestKey = estimateCache.keySet().iterator().next();
            estimateCache.remove(oldestKey);
        }
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
    public void onGuildMapUpdate(GuildMapUpdateEvent event) {
        this.clearCache();
    }
}
