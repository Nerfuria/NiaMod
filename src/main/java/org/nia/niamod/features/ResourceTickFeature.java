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

@SuppressWarnings("unused")
public class ResourceTickFeature extends Feature {
    private static final int RESOURCE_TICK_OFFSET_SECONDS = 5;
    private Integer lastMapTick = null;
    private Instant lastResTick = null;
    @Getter
    private ResourceTickOverlay resTickOverlay;

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
        resTickOverlay = new ResourceTickOverlay(this::getTimeUntilResTick);
        OverlayManager.registerOverlay(resTickOverlay);
    }

    @Subscribe
    public void onGuildMapResourcesUpdate(GuildMapResourcesUpdateEvent event) {
        Instant territoryLastTick = Instant.ofEpochMilli(FeatureManager.getTerritoryApiFeature().getTerritoryLastTick());

        int currentMapTick = calcMapTick();
        lastMapTick = currentMapTick;
        lastResTick = territoryLastTick.minusSeconds(currentMapTick + RESOURCE_TICK_OFFSET_SECONDS);
    }

    private int calcMapTick() {
        List<TerritoryPoi> territoryPois = Models.Territory.getTerritoryPoisFromAdvancement();
        List<Integer> mapTicks = new ArrayList<>();
        for (TerritoryPoi poi : territoryPois) {
            TerritoryInfo territoryInfo = poi.getTerritoryInfo();
            if (poi.getTerritoryProfile().getAcquired().isAfter(Instant.now().minusSeconds(60))) continue;
            if (territoryInfo == null) continue;
            if (territoryInfo.isHeadquarters()) continue;
            int emeraldGeneration = territoryInfo.getGeneration(GuildResource.EMERALDS);
            if (emeraldGeneration < 250000) continue;
            boolean hasResourceProduction = false;
            for (GuildResource resource : TerritoryUtils.RESOURCES) {
                if (!resource.isMaterialResource()) continue;
                if (territoryInfo.getGeneration(resource) >= 4800) {
                    hasResourceProduction = true;
                    break;
                }
            }
            if (hasResourceProduction) continue;
            CappedValue emeraldStorage = territoryInfo.getStorage(GuildResource.EMERALDS);
            if (emeraldStorage == null || emeraldStorage.max() < 6000) continue;
            int resourceStorageLevel = TerritoryUtils.getResStorageLevel(territoryInfo);
            if (resourceStorageLevel < 1) continue;
            int resourceStorageCost = TerritoryUtils.resStorageLevelToCost(resourceStorageLevel);
            float emeraldsMax = (emeraldGeneration - resourceStorageCost) / 60f;
            mapTicks.add(Math.round((emeraldStorage.current() / emeraldsMax) * 60));
        }
        return MathUtils.mode(mapTicks);
    }

    public int getTimeUntilResTick() {
        if (lastResTick == null) return -1;
        int secondsSinceResTick =
                (int) java.time.Duration.between(lastResTick, Instant.now()).getSeconds();
        return 60 - (secondsSinceResTick % 60);
    }

    public int getMapTick() {
        if (lastMapTick == null) return -1;
        return lastMapTick;
    }
}
