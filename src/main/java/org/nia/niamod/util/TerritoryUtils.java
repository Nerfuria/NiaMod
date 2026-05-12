package org.nia.niamod.util;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import lombok.experimental.UtilityClass;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.TerritoryBaseManager;
import org.nia.niamod.models.records.TerritoryCombatStats;
import org.nia.niamod.models.records.TerritoryInfoDepth;

import java.util.*;

@UtilityClass
public class TerritoryUtils {
    public static final GuildResource[] RESOURCES = GuildResource.values();

    public static int resStorageCapToLevel(int maxResourceStorage) {
        return switch (maxResourceStorage) {
            case 300 -> 0;
            case 600 -> 1;
            case 1200 -> 2;
            case 2400 -> 3;
            case 4500 -> 4;
            case 10200 -> 5;
            case 24000 -> 6;
            default -> -1;
        };
    }

    public static int resStorageLevelToCost(int resStorageLevel) {
        return Math.toIntExact(TerritoryUpgrade.RESOURCE_STORAGE.getLevels()[resStorageLevel].cost());
    }

    public static int getResStorageLevel(TerritoryInfo territoryInfo) {
        int hqModifier = territoryInfo.isHeadquarters() ? 5 : 1;
        for (GuildResource resource : GuildResource.values()) {
            if (!resource.isMaterialResource())
                continue;

            CappedValue storage = territoryInfo.getStorage(resource);
            if (storage != null)
                return TerritoryUtils.resStorageCapToLevel(storage.max() / hqModifier);
        }
        return 0;
    }

    public static int emeraldStorageLevelToCost(int emeraldStorageLevel) {
        return Math.toIntExact(TerritoryUpgrade.EMERALD_STORAGE.getLevels()[emeraldStorageLevel].cost());
    }

    public static int getEmeraldStorageLevel(TerritoryInfo territoryInfo) {
        int base = territoryInfo.isHeadquarters() ? 5000 : 3000;
        CappedValue storage = territoryInfo.getStorage(GuildResource.EMERALDS);
        if (storage == null)
            return 0;

        int bonus = (storage.max() * 100) / base - 100;
        TerritoryUpgrade.Level[] levels = TerritoryUpgrade.EMERALD_STORAGE.getLevels();
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].bonus() == bonus)
                return i;
        }
        return -1;
    }

    public static int getResCost(TerritoryInfo territoryInfo, GuildResource resource) {
        int mapTick = getMapTick();
        if (mapTick <= 0)
            return 0;

        CappedValue storage = territoryInfo.getStorage(resource);
        if (storage == null)
            return 0;

        int prod = territoryInfo.getGeneration(resource);

        return (storage.current() * 60 * 60 - (prod * mapTick)) / (60 - mapTick);
    }

    public static int getMapTick() {
        if (FeatureManager.getResTickFeature() == null) {
            return -1;
        }

        return FeatureManager.getResTickFeature().getMapTick();
    }

    public static int getHQDistance(String territoryName) {
        TerritoryInfo startTerr = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        String guild = startTerr.getGuildName();

        if (guild == null)
            return Integer.MAX_VALUE;

        if (startTerr.isHeadquarters())
            return 0;

        ArrayDeque<Pair<TerritoryInfo, Integer>> queue = new ArrayDeque<>();
        Set<String> checked = new HashSet<>();
        queue.add(new Pair<>(startTerr, 0));
        checked.add(territoryName);

        while (!queue.isEmpty()) {
            var next = queue.poll();
            TerritoryInfo terr = next.a();
            int dist = next.b();

            List<String> conns = terr.getTradingRoutes();
            for (String connName : conns) {
                if (checked.contains(connName))
                    continue;
                checked.add(connName);

                TerritoryInfo conn = Models.Territory.getTerritoryPoiFromAdvancement(connName).getTerritoryInfo();
                if (!guild.equals(conn.getGuildName()))
                    continue;
                if (conn.isHeadquarters())
                    return dist + 1;

                queue.add(new Pair<>(conn, dist + 1));
            }
        }
        return Integer.MAX_VALUE;
    }

    public static double getTreasuryBonus(String territoryName) {
        TerritoryInfo territory = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        GuildResourceValues level = territory.getTreasury();

        if (level.getLevel() <= 1)
            return 0.0;

        double tresValue = switch (level) {
            case NONE, VERY_LOW -> 0.0;
            case LOW -> 0.1;
            case MEDIUM -> 0.2;
            case HIGH -> 0.25;
            case VERY_HIGH -> 0.3;
        };

        int distance = getHQDistance(territoryName);
        double distanceModifier = switch (distance) {
            case 0, 1, 2 -> 1.0;
            case 3 -> 0.85;
            case 4 -> 0.7;
            case 5 -> 0.55;
            default -> 0.4;
        };

        return tresValue * distanceModifier;
    }

    public static double getProductionModifier(String territoryName, GuildResource resource) {
        int baseProd = TerritoryBaseManager.getTerritory(territoryName).getProduction(resource);
        TerritoryInfo territoryInfo = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        int currProd = territoryInfo.getGeneration(resource);
        if (baseProd == 0)
            return 0;
        return ((double) currProd) / baseProd;
    }

    public static TerritoryInfo territoryInfo(String territoryName) {
        var poi = Models.Territory.getTerritoryPoiFromAdvancement(territoryName);
        return poi == null ? null : poi.getTerritoryInfo();
    }

    public static TerritoryCombatStats estimateStats(Map<TerritoryUpgrade, Integer> upgrades, TerritoryInfo info) {
        int directConnections = ownedConnections(info);
        int externalConnections = info.isHeadquarters() ? ownedExternalConnections(info) : 0;
        double multiplier = territoryMultiplier(directConnections, externalConnections, info.isHeadquarters());

        double damageMultiplier = upgradeMultiplier(upgrades, TerritoryUpgrade.DAMAGE);
        double minDamage = 1000.0 * damageMultiplier * multiplier;
        double maxDamage = 1500.0 * damageMultiplier * multiplier;
        double attackSpeed = 0.5 * upgradeMultiplier(upgrades, TerritoryUpgrade.ATTACK);
        double health = 300000.0 * upgradeMultiplier(upgrades, TerritoryUpgrade.HEALTH) * multiplier;
        double defence = 0.1 * upgradeMultiplier(upgrades, TerritoryUpgrade.DEFENCE);
        double averageDamage = (minDamage + maxDamage) / 2.0;
        double averageDps = averageDamage * attackSpeed;
        double ehp = defence >= 1.0 ? Double.POSITIVE_INFINITY : health / (1.0 - defence);

        return new TerritoryCombatStats(minDamage, maxDamage, attackSpeed, health, defence, averageDps, ehp);
    }

    private static double territoryMultiplier(int directConnections, int externalConnections, boolean headquarters) {
        double connectionMultiplier = 1.0 + (0.3 * directConnections);
        if (!headquarters) {
            return connectionMultiplier;
        }

        return (1.5 + (0.25 * externalConnections)) * connectionMultiplier;
    }

    private static double upgradeMultiplier(Map<TerritoryUpgrade, Integer> upgrades, TerritoryUpgrade upgrade) {
        int level = upgrades == null ? 0 : upgrades.getOrDefault(upgrade, 0);
        TerritoryUpgrade.Level[] levels = upgrade.getLevels();
        int clampedLevel = Math.max(0, Math.min(level, levels.length - 1));
        return 1.0 + levels[clampedLevel].bonus() / 100.0;
    }

    private static int ownedConnections(TerritoryInfo info) {
        String guild = info.getGuildName();
        if (guild == null) {
            return 0;
        }

        int count = 0;
        for (String connection : info.getTradingRoutes()) {
            TerritoryInfo connected = territoryInfo(connection);
            if (connected != null && guild.equals(connected.getGuildName())) {
                count++;
            }
        }
        return count;
    }

    private static int ownedExternalConnections(TerritoryInfo info) {
        String guild = info.getGuildName();
        if (guild == null) {
            return 0;
        }

        Set<TerritoryInfo> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<TerritoryInfoDepth> queue = new ArrayDeque<>();
        visited.add(info);
        queue.addLast(new TerritoryInfoDepth(info, 0));

        int count = 0;
        while (!queue.isEmpty()) {
            TerritoryInfoDepth current = queue.removeFirst();
            if (current.depth() >= 3) {
                continue;
            }

            for (String connection : current.info().getTradingRoutes()) {
                TerritoryInfo connected = territoryInfo(connection);
                if (connected == null || !guild.equals(connected.getGuildName()) || !visited.add(connected)) {
                    continue;
                }

                count++;
                queue.addLast(new TerritoryInfoDepth(connected, current.depth() + 1));
            }
        }

        return count;
    }

}
