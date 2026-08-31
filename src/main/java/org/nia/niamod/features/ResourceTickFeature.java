package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.type.CappedValue;
import lombok.Getter;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.OverlayManager;
import org.nia.niamod.models.events.GuildMapResourcesUpdateEvent;
import org.nia.niamod.overlays.ResourceTickOverlay;
import org.nia.niamod.util.MathUtils;
import org.nia.niamod.util.TerritoryUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ResourceTickFeature extends Feature {
    private static final int RESOURCE_TICK_OFFSET_SECONDS = 2;
    @Getter
    private int currentMapTick = -1;
    private Instant lastResTick = null;
    @Getter
    private ResourceTickOverlay resTickOverlay;

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
        resTickOverlay = new ResourceTickOverlay(this::getSecondsUntilResTick);
        OverlayManager.registerOverlay(resTickOverlay);
    }

    @Subscribe
    public void onGuildMapResourcesUpdate(GuildMapResourcesUpdateEvent event) {
        Instant territoryLastTick = Instant.ofEpochMilli(FeatureManager.getTerritoryApiFeature().getTerritoryLastTick());

        currentMapTick = calcMapTick();
        lastResTick = territoryLastTick.minusSeconds(currentMapTick + RESOURCE_TICK_OFFSET_SECONDS);
    }

    private int calcMapTick() {
        List<TerritoryPoi> territoryPois = Models.Territory.getTerritoryPoisFromAdvancement();
        List<Integer> mapTicks = new ArrayList<>();
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);

        for (TerritoryPoi poi : territoryPois) {
            TerritoryInfo territoryInfo = poi.getTerritoryInfo();
            if (territoryInfo == null)
                continue;
            if (poi.getTerritoryProfile().getAcquired().isAfter(oneMinuteAgo))
                continue;
            if (territoryInfo.isHeadquarters())
                continue;

            int emeraldGeneration = territoryInfo.getGeneration(GuildResource.EMERALDS);
            if (emeraldGeneration < 250000)
                continue;

            boolean hasResourceProductionBuff = false;
            for (GuildResource resource : TerritoryUtils.RESOURCES) {
                if (!resource.isMaterialResource())
                    continue;
                if (territoryInfo.getGeneration(resource) >= 4800) {
                    hasResourceProductionBuff = true;
                    break;
                }
            }
            if (hasResourceProductionBuff)
                continue;

            CappedValue emeraldStorage = territoryInfo.getStorage(GuildResource.EMERALDS);
            if (emeraldStorage == null || emeraldStorage.max() < 6000)
                continue;
            int resourceStorageLevel = TerritoryUtils.getResStorageLevel(territoryInfo);
            if (resourceStorageLevel < 1)
                continue;

            int resourceStorageCost = TerritoryUtils.resStorageLevelToCost(resourceStorageLevel);
            float emeraldsPerMin = (emeraldGeneration - resourceStorageCost) / 60f;
            mapTicks.add(Math.round((emeraldStorage.current() / emeraldsPerMin) * 60));
        }
        return MathUtils.mode(mapTicks);
    }

    public int getSecondsUntilResTick() {
        if (lastResTick == null)
            return -1;
        int secondsSinceResTick = (int) java.time.Duration.between(lastResTick, Instant.now()).getSeconds();
        return 60 - (secondsSinceResTick % 60);
    }
}
