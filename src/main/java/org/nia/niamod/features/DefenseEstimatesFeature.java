package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.util.TerritoryUtils;

import java.util.*;

public class DefenseEstimatesFeature extends Feature {

    private final static Map<Integer, List<EmeraldProdUpgrade>> EMERALD_MODIFIER_OPTIONS = getEmeraldModifierOptions();
    private final static Map<CacheKey, CachedDefenseEstimate> DEFENSE_CACHE = new HashMap<>();

    private static Map<Integer, List<EmeraldProdUpgrade>> getEmeraldModifierOptions() {
        Map<Integer, List<EmeraldProdUpgrade>> result = new HashMap<>();
        for (int effLvl = 0; effLvl < TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels().length; effLvl++) {
            for (int rateLvl = 0; rateLvl < TerritoryUpgrade.EMERALD_RATE.getLevels().length; rateLvl++) {
                double eff = TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].bonus();
                double rate = TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].bonus();
                int modifier = (int) ((100 + eff) * (4.0 / rate));
                if (!result.containsKey(modifier))
                    result.put(modifier, new ArrayList<>());
                result.get(modifier).add(new EmeraldProdUpgrade(
                        effLvl,
                        rateLvl,
                        Math.toIntExact(TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels()[effLvl].cost()),
                        Math.toIntExact(TerritoryUpgrade.EMERALD_RATE.getLevels()[rateLvl].cost())
                ));
            }
        }
        return Map.copyOf(result);
    }

    private static EmeraldProdUpgrade getMostLikelyEmProdUpgrade(String territoryName, TerritoryInfo territoryInfo) {
        double treasuryBonus = TerritoryUtils.getTreasuryBonus(territoryName);
        double modifier = TerritoryUtils.getProductionModifier(territoryName, GuildResource.EMERALDS);
        modifier = Math.round(modifier / (1.0 + treasuryBonus) * 100);
        List<EmeraldProdUpgrade> options = EMERALD_MODIFIER_OPTIONS.get((int) modifier);
        if (options == null)
            return null;

        int baseCrop = TerritoryUtils.getResCost(territoryInfo, GuildResource.CROPS);
        int baseOre = TerritoryUtils.getResCost(territoryInfo, GuildResource.ORE);

        int bestOption = -1;
        float bestDiff = Integer.MAX_VALUE;

        for (int i = 0; i < options.size(); i++) {
            int restCrop = baseCrop - options.get(i).cropCost;
            int restOre = baseOre - options.get(i).oreCost;

            if (restCrop <= 0 || restOre <= 0)
                continue;

            int diff = Math.abs(restCrop - restOre);
            if (diff < bestDiff) {
                bestOption = i;
                bestDiff = diff;
            }
        }
        if (bestOption == -1)
            return null;
        return options.get(bestOption);
    }

    private static Pair<Integer, Integer> findUpgradeLevels(TerritoryUpgrade upgrade, TerritoryUpgrade bonus, int targetCost, boolean forceBonus) {
        Pair<Integer, Integer> best = new Pair<>(0, 0);
        long bestCost = 0;

        for (int bonusLevel = forceBonus ? 1 : 0; bonusLevel < bonus.getLevels().length; bonusLevel++) {
            long bonusCost = bonus.getLevels()[bonusLevel].cost();
            for (int upgradeLevel = 0; upgradeLevel < upgrade.getLevels().length; upgradeLevel++) {
                long upgradeCost = upgrade.getLevels()[upgradeLevel].cost();

                long totalCost = upgradeCost + bonusCost;
                if (totalCost <= targetCost + 99 && totalCost > bestCost) {
                    best = new Pair<>(upgradeLevel, bonusLevel);
                    bestCost = upgradeCost + bonusCost;
                } else if (totalCost > targetCost + 50)
                    break;
            }
        }
        return best;
    }

    @Safe
    public static Map<TerritoryUpgrade, Integer> estimateDefenses(String territoryName) {
        TerritoryInfo territoryInfo = Models.Territory.getTerritoryPoiFromAdvancement(territoryName).getTerritoryInfo();
        CacheKey cacheKey = CacheKey.from(territoryInfo);
        CachedDefenseEstimate cached = DEFENSE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached.defenses();
        }

        Map<TerritoryUpgrade, Integer> result = new HashMap<>();

        if (territoryInfo.isHeadquarters()) {
            return cache(cacheKey, result);  // TODO deal with HQs separately
        }

        boolean hasAuraVolley = territoryInfo.getDefences().getLevel() >= 3;

        GuildResource[] resources = {GuildResource.ORE, GuildResource.CROPS, GuildResource.WOOD, GuildResource.FISH};
        TerritoryUpgrade[] upgrades = {TerritoryUpgrade.DAMAGE, TerritoryUpgrade.ATTACK, TerritoryUpgrade.HEALTH, TerritoryUpgrade.DEFENCE};
        TerritoryUpgrade[] bonuses = {TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.TOWER_MULTI_ATTACKS};

        int[] otherCosts = {0, 0, 0, 0};
        // Emerald storage
        int emStorageLvl = TerritoryUtils.getEmeraldStorageLevel(territoryInfo);
        if (emStorageLvl > 0)
            otherCosts[2] = TerritoryUtils.emeraldStorageLevelToCost(emStorageLvl);
        // Emerald prod
        EmeraldProdUpgrade emeraldProdUpgrade = getMostLikelyEmProdUpgrade(territoryName, territoryInfo);
        if (emeraldProdUpgrade != null) {
            otherCosts[0] = emeraldProdUpgrade.oreCost;
            otherCosts[1] = emeraldProdUpgrade.cropCost;
        }

        for (int i = 0; i < resources.length; i++) {
            int cost = TerritoryUtils.getResCost(territoryInfo, resources[i]);
            cost -= otherCosts[i];

            boolean forceBonus = hasAuraVolley && (i == 0 || i == 1);

            Pair<Integer, Integer> levels = findUpgradeLevels(upgrades[i], bonuses[i], cost, forceBonus);

            result.put(upgrades[i], levels.a());
            result.put(bonuses[i], levels.b());
        }

        return cache(cacheKey, result);
    }

    private static Map<TerritoryUpgrade, Integer> cache(
            CacheKey cacheKey,
            Map<TerritoryUpgrade, Integer> defenses) {
        Map<TerritoryUpgrade, Integer> cachedDefenses = Map.copyOf(defenses);
        DEFENSE_CACHE.put(cacheKey, new CachedDefenseEstimate(cachedDefenses));
        return cachedDefenses;
    }

    @Safe
    public void init() {
        DEFENSE_CACHE.clear();
    }

    private record EmeraldProdUpgrade(int efficientEmeralds, int emeraldRate, int oreCost, int cropCost) {}

    private record CachedDefenseEstimate(Map<TerritoryUpgrade, Integer> defenses) {}

    private record CacheKey(int emeralds, int ore, int crops, int fish, int wood, boolean hq) {
        public static CacheKey from(TerritoryInfo info) {
            return new CacheKey(info.getGeneration(GuildResource.EMERALDS), info.getGeneration(GuildResource.ORE), info.getGeneration(GuildResource.CROPS), info.getGeneration(GuildResource.FISH), info.getGeneration(GuildResource.WOOD), info.isHeadquarters());
        }
    }
}
