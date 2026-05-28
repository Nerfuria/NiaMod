package org.nia.niamod.util;

import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.TerritoryUpgrade;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import org.nia.niamod.managers.TerritoryBaseManager;
import org.nia.niamod.models.defense.DefenseEstimate;
import org.nia.niamod.models.defense.EmeraldProductionUpgrade;
import org.nia.niamod.models.defense.ResourceProductionUpgrade;
import org.nia.niamod.models.records.TerritoryCombatStats;

import java.util.*;

@UtilityClass
public class DefenseEstimateUtils {
    private static final Map<Integer, List<EmeraldProductionUpgrade>> EMERALD_MODIFIER_MAPPING = getEmeraldModifierMapping();
    private static final Map<Integer, ResourceProductionUpgrade> RESOURCE_MODIFIER_MAPPING = getResourceModifierMapping();
    private static final List<TerritoryUpgrade> DEFENSE_ESTIMATE_ORDER = List.of(
            TerritoryUpgrade.DAMAGE,
            TerritoryUpgrade.ATTACK,
            TerritoryUpgrade.HEALTH,
            TerritoryUpgrade.DEFENCE
    );
    private static final List<TerritoryUpgrade> BONUS_ESTIMATE_ORDER = List.of(
            TerritoryUpgrade.TOWER_AURA,
            TerritoryUpgrade.TOWER_VOLLEY,
            TerritoryUpgrade.STRONGER_MINIONS,
            TerritoryUpgrade.TOWER_MULTI_ATTACKS
    );

    private static Map<Integer, List<EmeraldProductionUpgrade>> getEmeraldModifierMapping() {
        Map<Integer, List<EmeraldProductionUpgrade>> result = new HashMap<>();
        TerritoryUpgrade.Level[] effLevels = TerritoryUpgrade.EFFICIENT_EMERALDS.getLevels();
        TerritoryUpgrade.Level[] rateLevels = TerritoryUpgrade.EMERALD_RATE.getLevels();
        for (int effLvl = 0; effLvl < effLevels.length; effLvl++) {
            for (int rateLvl = 0; rateLvl < rateLevels.length; rateLvl++) {
                double eff = effLevels[effLvl].bonus();
                double rate = rateLevels[rateLvl].bonus();
                int modifier = (int) ((100 + eff) * (4.0 / rate));
                if (!result.containsKey(modifier))
                    result.put(modifier, new ArrayList<>());
                result.get(modifier).add(new EmeraldProductionUpgrade(
                        effLvl,
                        rateLvl,
                        Math.toIntExact(effLevels[effLvl].cost()),
                        Math.toIntExact(rateLevels[rateLvl].cost())
                ));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<Integer, ResourceProductionUpgrade> getResourceModifierMapping() {
        Map<Integer, ResourceProductionUpgrade> result = new HashMap<>();
        TerritoryUpgrade.Level[] effLevels = TerritoryUpgrade.EFFICIENT_RESOURCES.getLevels();
        TerritoryUpgrade.Level[] rateLevels = TerritoryUpgrade.RESOURCE_RATE.getLevels();
        for (int effLvl = 0; effLvl < effLevels.length; effLvl++) {
            for (int rateLvl = 0; rateLvl < rateLevels.length; rateLvl++) {
                double eff = effLevels[effLvl].bonus();
                double rate = rateLevels[rateLvl].bonus();
                int modifier = (int) ((100 + eff) * (4.0 / rate));

                ResourceProductionUpgrade upgrade = new ResourceProductionUpgrade(
                        effLvl,
                        rateLvl,
                        Math.toIntExact(effLevels[effLvl].cost()) + Math.toIntExact(rateLevels[rateLvl].cost())
                );
                if (!result.containsKey(modifier) || upgrade.cost() < result.get(modifier).cost())
                    result.put(modifier, upgrade);
            }
        }
        return Map.copyOf(result);
    }

    private static int getResCost(String territoryName, TerritoryInfo territoryInfo, GuildResource resource) {
        int mapTick = TerritoryUtils.getMapTick();
        if (mapTick <= 0)
            return 0;

        CappedValue storage = territoryInfo.getStorage(resource);
        if (storage == null)
            return 0;
        int stored = storage.current();

        int prod = territoryInfo.getGeneration(resource);
        double prodPart = 0.0;
        if (prod != 0) {
            int baseProd = TerritoryBaseManager.getTerritory(territoryName).getProduction(resource);
            double treasuryModifier = TerritoryUtils.getTreasuryBonus(territoryName) + 1.0;
            double prodModifier = TerritoryUtils.getProductionModifier(territoryName, resource);
            ResourceProductionUpgrade prodUpgrade = RESOURCE_MODIFIER_MAPPING.get((int) Math.round(prodModifier / treasuryModifier * 100.0));
            double effModifier;
            double resRate;
            if (prodUpgrade != null) {
                effModifier = TerritoryUpgrade.EFFICIENT_RESOURCES.getLevels()[prodUpgrade.efficientResources()].bonus() / 100.0 + 1.0;
                resRate = TerritoryUpgrade.RESOURCE_RATE.getLevels()[prodUpgrade.resourceRate()].bonus();
            } else {
                effModifier = prod / (baseProd * treasuryModifier);
                resRate = 4.0;
            }

            prodPart = (Math.floor(baseProd * treasuryModifier * effModifier / 36.0) / 100.0) * 4.0
                     * Math.floor((mapTick) / resRate);
        }

        return (int) (36 * Math.ceil((stored + 1.0 - prodPart) / ((60.0 - mapTick) / 100.0)) - 1);
    }

    public static DefenseEstimate estimate(String territoryName, TerritoryInfo territoryInfo) {
        if (territoryInfo == null) {
            return DefenseEstimate.EMPTY;
        }

        Map<TerritoryUpgrade, Integer> defenses = estimateDefenses(territoryName, territoryInfo);
        List<String> stats = estimateStats(defenses, territoryInfo);
        return new DefenseEstimate(Map.copyOf(defenses), List.copyOf(stats));
    }

    private static Map<TerritoryUpgrade, Integer> estimateDefenses(String territoryName, TerritoryInfo territoryInfo) {
        Map<TerritoryUpgrade, Integer> result = new HashMap<>();

        if (territoryInfo.isHeadquarters()) {
            result.put(TerritoryUpgrade.DAMAGE, 11);
            result.put(TerritoryUpgrade.ATTACK, 11);
            result.put(TerritoryUpgrade.HEALTH, 11);
            result.put(TerritoryUpgrade.DEFENCE, 11);
            result.put(TerritoryUpgrade.TOWER_AURA, 3);
            result.put(TerritoryUpgrade.TOWER_VOLLEY, 3);
            result.put(TerritoryUpgrade.TOWER_MULTI_ATTACKS, 1);
            return result;
        }

        boolean hasAuraVolley = territoryInfo.getDefences().getLevel() == 3;
        GuildResource[] resources = {GuildResource.ORE, GuildResource.CROPS, GuildResource.WOOD, GuildResource.FISH};
        TerritoryUpgrade[] upgrades = {TerritoryUpgrade.DAMAGE, TerritoryUpgrade.ATTACK, TerritoryUpgrade.HEALTH, TerritoryUpgrade.DEFENCE};
        TerritoryUpgrade[] bonuses = {TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.TOWER_MULTI_ATTACKS};
        int[] otherCosts = getOtherResourceCosts(territoryName, territoryInfo);

        for (int i = 0; i < resources.length; i++) {
            int cost = getResCost(territoryName, territoryInfo, resources[i]) - otherCosts[i];
            boolean forceBonus = false;
            Pair<Integer, Integer> levels = findUpgradeLevels(upgrades[i], bonuses[i], cost, forceBonus);

            result.put(upgrades[i], levels.a());
            result.put(bonuses[i], levels.b());
        }

        return result;
    }

    private static int[] getOtherResourceCosts(String territoryName, TerritoryInfo territoryInfo) {
        int[] otherCosts = {0, 0, 0, 0};
        int emeraldStorageLevel = TerritoryUtils.getEmeraldStorageLevel(territoryInfo);
        if (emeraldStorageLevel > 0) {
            otherCosts[2] = TerritoryUtils.emeraldStorageLevelToCost(emeraldStorageLevel);
        }

        EmeraldProductionUpgrade emeraldProductionUpgrade = getMostLikelyEmeraldProductionUpgrade(territoryName, territoryInfo);
        if (emeraldProductionUpgrade != null) {
            otherCosts[0] = emeraldProductionUpgrade.oreCost();
            otherCosts[1] = emeraldProductionUpgrade.cropCost();
        }

        return otherCosts;
    }

    private static EmeraldProductionUpgrade getMostLikelyEmeraldProductionUpgrade(String territoryName, TerritoryInfo territoryInfo) {
        double treasuryBonus = TerritoryUtils.getTreasuryBonus(territoryName);
        double modifier = TerritoryUtils.getProductionModifier(territoryName, GuildResource.EMERALDS);
        List<EmeraldProductionUpgrade> options = EMERALD_MODIFIER_MAPPING.get((int) Math.round(modifier / (1.0 + treasuryBonus) * 100));
        if (options == null) {
            return null;
        }

        int baseCrop = getResCost(territoryName, territoryInfo, GuildResource.CROPS);
        int baseOre = getResCost(territoryName, territoryInfo, GuildResource.ORE);
        EmeraldProductionUpgrade bestOption = null;
        int bestDifference = Integer.MAX_VALUE;

        for (EmeraldProductionUpgrade option : options) {
            int restCrop = baseCrop - option.cropCost();
            int restOre = baseOre - option.oreCost();
            if (restCrop <= 0 || restOre <= 0) {
                continue;
            }

            int difference = Math.abs(restCrop - restOre);
            if (difference < bestDifference) {
                bestOption = option;
                bestDifference = difference;
            }
        }

        return bestOption;
    }

    private static Pair<Integer, Integer> findUpgradeLevels(TerritoryUpgrade upgrade, TerritoryUpgrade bonus, int targetCost, boolean forceBonus) {
        Pair<Integer, Integer> best = new Pair<>(0, 0);
        long bestCost = 0;

        for (int bonusLevel = forceBonus ? 1 : 0; bonusLevel < bonus.getLevels().length; bonusLevel++) {
            long bonusCost = bonus.getLevels()[bonusLevel].cost();
            for (int upgradeLevel = 0; upgradeLevel < upgrade.getLevels().length; upgradeLevel++) {
                long totalCost = upgrade.getLevels()[upgradeLevel].cost() + bonusCost;
                if (totalCost <= targetCost && totalCost > bestCost) {
                    best = new Pair<>(upgradeLevel, bonusLevel);
                    bestCost = totalCost;
                } else if (totalCost > targetCost) {
                    break;
                }
            }
        }
        return best;
    }

    private static List<String> estimateStats(Map<TerritoryUpgrade, Integer> upgrades, TerritoryInfo info) {
        TerritoryCombatStats stats = TerritoryUtils.estimateStats(upgrades, info);
        return List.of(
                formatDamageLine(stats),
                formatHealthLine(stats),
                ChatFormatting.GRAY + "Avg DPS: " + ChatFormatting.RED + formatStat(stats.averageDps()),
                ChatFormatting.GRAY + "EHP: " + ChatFormatting.GREEN + formatStat(stats.ehp())
        );
    }

    private static String formatDamageLine(TerritoryCombatStats stats) {
        return ChatFormatting.GRAY + "DMG: "
                + ChatFormatting.RED + formatStat(stats.minDamage())
                + ChatFormatting.DARK_RED + "-"
                + ChatFormatting.RED + formatStat(stats.maxDamage())
                + ChatFormatting.GRAY + " ("
                + ChatFormatting.AQUA + String.format(Locale.ROOT, "%.2f", stats.attackSpeed()) + "x"
                + ChatFormatting.GRAY + ")";
    }

    private static String formatHealthLine(TerritoryCombatStats stats) {
        return ChatFormatting.GRAY + "HP: "
                + ChatFormatting.GREEN + formatStat(stats.health())
                + ChatFormatting.GRAY + " ("
                + ChatFormatting.BLUE + formatPercent(stats.defence())
                + ChatFormatting.GRAY + ")";
    }

    private static String formatStat(double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }

    public static List<String> tooltipLines(DefenseEstimate estimate) {
        if (estimate == DefenseEstimate.EMPTY) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        Map<TerritoryUpgrade, Integer> defenses = estimate.defenses();

        lines.add(ChatFormatting.GOLD + "Predicted Defences");
        for (TerritoryUpgrade upgrade : DEFENSE_ESTIMATE_ORDER) {
            lines.add(ChatFormatting.GRAY + upgrade.getName() + ": " + ChatFormatting.WHITE + defenses.getOrDefault(upgrade, 0));
        }

        for (TerritoryUpgrade upgrade : BONUS_ESTIMATE_ORDER) {
            int level = defenses.getOrDefault(upgrade, 0);
            if (level > 0) {
                lines.add(ChatFormatting.GRAY + upgrade.getName() + ": " + ChatFormatting.WHITE + level);
            }
        }

        lines.add("");
        lines.add(ChatFormatting.GOLD + "Predicted Stats");
        lines.addAll(estimate.stats());

        return lines;
    }
}
