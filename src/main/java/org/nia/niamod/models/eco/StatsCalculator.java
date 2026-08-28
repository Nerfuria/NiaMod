package org.nia.niamod.models.eco;

import org.nia.niamod.features.TerritoryManagerFeature;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record StatsCalculator(TerritoryManagerFeature feature, ConnectedTerritoryLookup connectedTerritories) {
    public StatsCalculator(TerritoryManagerFeature feature, ConnectedTerritoryLookup connectedTerritories) {
        this.feature = feature;
        this.connectedTerritories = connectedTerritories == null ? ignored -> null : connectedTerritories;
    }

    public static int statLevel(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return stats == null ? 0 : stats.getOrDefault(upgrade, 0);
    }

    public TerritoryDetails details(TerritoryNode territory, String guildName) {
        if (territory == null) {
            return null;
        }

        Map<TerritoryUpgrade, Integer> stats = feature.statsFor(territory.name());
        int ownedConnections = ownedConnectionCount(territory);
        int externalConnections = externalTerritoryCount(territory);
        boolean headquarters = feature.isHeadquarters(territory.name());

        return new TerritoryDetails(
                territory,
                guildName,
                stats,
                resourceStoreFor(territory.name(), stats),
                producedResourcesPerHour(territory, stats),
                damage(territory, ownedConnections, externalConnections, headquarters, stats),
                attackSpeed(stats),
                health(territory, ownedConnections, externalConnections, headquarters, stats),
                defense(stats),
                ownedConnections,
                totalConnectionCount(territory),
                externalConnections,
                feature.tax(territory.name()),
                headquarters,
                feature.bordersOpen(territory.name()),
                feature.territoryRoute(territory.name()).label(),
                feature.loadoutFor(territory.name())
        );
    }

    public ResourceFlow summarizeResourceFlow(Collection<TerritoryNode> territories) {
        Resources stored = Resources.EMPTY;
        Resources capacity = Resources.EMPTY;
        Resources gained = Resources.EMPTY;
        Resources used = Resources.EMPTY;

        if (territories != null) {
            for (TerritoryNode territory : territories) {
                if (territory == null) {
                    continue;
                }

                Map<TerritoryUpgrade, Integer> stats = feature.statsFor(territory.name());
                if (feature.isHeadquarters(territory.name())) {
                    TerritoryResourceStore store = resourceStoreFor(territory.name(), stats);
                    stored = store.current();
                    capacity = store.max();
                }
                gained = gained.plus(producedResourcesPerHour(territory, stats));
                used = used.plus(resourceCostPerHour(stats));
            }
        }

        double usagePercent = gained.materialTotal() <= 0L ? 0.0 : used.materialTotal() / (double) gained.materialTotal() * 100.0;
        return new ResourceFlow(stored, capacity, gained, used, usagePercent);
    }

    public Resources producedResourcesPerHour(TerritoryNode territory, Map<TerritoryUpgrade, Integer> stats) {
        if (territory == null) {
            return Resources.EMPTY;
        }

        Resources base = territory.resources();
        return new Resources(
                roundProductionPerHour(base.emeralds(), stats, TerritoryUpgrade.EMERALD_RATE, TerritoryUpgrade.EFFICIENT_EMERALDS),
                roundProductionPerHour(base.ore(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.crops(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.fish(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.wood(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES)
        );
    }

    private TerritoryResourceStore resourceStoreFor(String territoryName, Map<TerritoryUpgrade, Integer> stats) {
        double materialStorageBase = feature.isHeadquarters(territoryName) ? 1500.0 : 300.0;
        long materialStorage = Math.round(materialStorageBase * storageMultiplier(stats, TerritoryUpgrade.RESOURCE_STORAGE));
        long emeraldStorage = Math.round(1500.0 * storageMultiplier(stats, TerritoryUpgrade.EMERALD_STORAGE));
        return new TerritoryResourceStore(
                Resources.EMPTY,
                new Resources(emeraldStorage, materialStorage, materialStorage, materialStorage, materialStorage)
        );
    }

    private Resources resourceCostPerHour(Map<TerritoryUpgrade, Integer> stats) {
        Resources total = Resources.EMPTY;
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            total = total.plus(Resources.of(upgrade.costResource(), upgrade.cost(statLevel(stats, upgrade))));
        }
        return total;
    }

    private TerritoryDetails.DamageRange damage(TerritoryNode territory, int directConnections, int externalConnections, boolean headquarters, Map<TerritoryUpgrade, Integer> stats) {
        double multiplier = territoryMultiplier(directConnections, externalConnections, headquarters);
        double damageMultiplier = percentMultiplier(stats, TerritoryUpgrade.DAMAGE);
        return new TerritoryDetails.DamageRange(1_000.0 * damageMultiplier * multiplier, 1_500.0 * damageMultiplier * multiplier);
    }

    private double attackSpeed(Map<TerritoryUpgrade, Integer> stats) {
        return 0.5 * percentMultiplier(stats, TerritoryUpgrade.ATTACK);
    }

    private double health(TerritoryNode territory, int directConnections, int externalConnections, boolean headquarters, Map<TerritoryUpgrade, Integer> stats) {
        if (territory == null) {
            return 0.0;
        }
        return 300_000.0 * percentMultiplier(stats, TerritoryUpgrade.HEALTH) * territoryMultiplier(directConnections, externalConnections, headquarters);
    }

    private double defense(Map<TerritoryUpgrade, Integer> stats) {
        return 0.1 * percentMultiplier(stats, TerritoryUpgrade.DEFENSE);
    }

    private double territoryMultiplier(int directConnections, int externalConnections, boolean headquarters) {
        double connectionMultiplier = 1.0 + (0.3 * directConnections);
        if (!headquarters) {
            return connectionMultiplier;
        }
        return (1.5 + (0.25 * externalConnections)) * connectionMultiplier;
    }

    private double storageMultiplier(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return 1.0 + (upgradeBonus(stats, upgrade) / 100.0);
    }

    private double percentMultiplier(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return 1.0 + (upgradeBonus(stats, upgrade) / 100.0);
    }

    private double upgradeBonus(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return upgrade.bonus(statLevel(stats, upgrade));
    }

    private double calculateProductionPerHour(long basePerHour, Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        if (basePerHour <= 0) {
            return 0.0;
        }

        double basePerProduction = basePerHour / 900.0;
        double secondsPerProduction = Math.max(1.0, upgradeBonus(stats, rateUpgrade));
        double efficientMultiplier = 1.0 + (upgradeBonus(stats, efficientUpgrade) / 100.0);
        return basePerProduction * (3600.0 / secondsPerProduction) * efficientMultiplier;
    }

    private long roundProductionPerHour(long basePerHour, Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        return Math.round(calculateProductionPerHour(basePerHour, stats, rateUpgrade, efficientUpgrade));
    }

    private int ownedConnectionCount(TerritoryNode territory) {
        int count = 0;
        for (String connection : territory.connections()) {
            if (connectedTerritories.findOwned(connection) != null) {
                count++;
            }
        }
        return count;
    }

    private int externalTerritoryCount(TerritoryNode territory) {
        if (territory == null) {
            return 0;
        }

        Set<String> visited = new HashSet<>();
        ArrayDeque<TerritoryNodeDepth> queue = new ArrayDeque<>();
        visited.add(normalize(territory.name()));
        queue.addLast(new TerritoryNodeDepth(territory, 0));

        int count = 0;
        while (!queue.isEmpty()) {
            TerritoryNodeDepth current = queue.removeFirst();
            if (current.depth() >= 3) {
                continue;
            }

            for (String connection : current.territory().connections()) {
                TerritoryNode target = connectedTerritories.findOwned(connection);
                if (target == null) {
                    continue;
                }

                String key = normalize(target.name());
                if (visited.add(key)) {
                    count++;
                    queue.addLast(new TerritoryNodeDepth(target, current.depth() + 1));
                }
            }
        }

        return count;
    }

    private int totalConnectionCount(TerritoryNode territory) {
        return (int) territory.connections().stream()
                .filter(connection -> connection != null && !connection.isBlank())
                .count();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface ConnectedTerritoryLookup {
        TerritoryNode findOwned(String territoryName);
    }

    private record TerritoryNodeDepth(TerritoryNode territory, int depth) {
    }
}
