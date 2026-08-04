package org.nia.niamod.features;

import com.wynntils.utils.type.Pair;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.config.NyahConfigData;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.GuildMapUpdateEvent;
import org.nia.niamod.models.events.HoveredTerritoryInfoRenderEvent;
import org.nia.niamod.render.TerritoryRouteRenderer;
import org.nia.niamod.render.TerritoryRouteStyle;
import org.nia.niamod.util.TerritoryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerritoryRouteFlowFeature extends Feature {
    private final Map<String, Pair<List<String>, List<String>>> routeCache = new HashMap<>();
    private final TerritoryRouteRenderer renderer = new TerritoryRouteRenderer();

    @Override
    public void init() {
        routeCache.clear();
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    public void renderRoutes(HoveredTerritoryInfoRenderEvent event) {
        NyahConfigData config = NyahConfig.getData();
        boolean headquartersToTerritoryEnabled = config.isHqToTerritoryRouteEnabled();
        boolean territoryToHeadquartersEnabled = config.isTerritoryToHqRouteEnabled();
        if (!headquartersToTerritoryEnabled && !territoryToHeadquartersEnabled) {
            return;
        }

        String territoryName = event.territoryPoi().getName();
        Pair<List<String>, List<String>> routes = routeCache.computeIfAbsent(
                territoryName,
                TerritoryUtils::getResPaths
        );
        TerritoryRouteStyle headquartersToTerritory = routeStyle(config, headquartersToTerritoryEnabled, config.getHqToTerritoryLineColor());
        TerritoryRouteStyle territoryToHeadquarters = routeStyle(config, territoryToHeadquartersEnabled, config.getTerritoryToHqLineColor());
        renderer.render(
                event,
                territoryName,
                routes.a(),
                headquartersToTerritory,
                routes.b(),
                territoryToHeadquarters
        );
    }

    @Subscribe
    public void onGuildMapUpdate(GuildMapUpdateEvent event) {
        routeCache.clear();
    }

    private TerritoryRouteStyle routeStyle(NyahConfigData config, boolean enabled, int color) {
        return new TerritoryRouteStyle(
                enabled,
                color,
                config.getTerritoryRouteLineWidth(),
                config.getTerritoryRouteGlowStrength(),
                config.getTerritoryRouteLightLength(),
                config.getTerritoryRouteLightSpacing(),
                config.getTerritoryRouteLightSpeed()
        );
    }
}
