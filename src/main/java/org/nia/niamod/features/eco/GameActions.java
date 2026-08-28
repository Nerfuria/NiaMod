package org.nia.niamod.features.eco;

import org.nia.niamod.models.eco.*;
import org.nia.niamod.models.eco.GameChange.Borders;
import org.nia.niamod.models.eco.GameChange.Headquarters;
import org.nia.niamod.models.eco.GameChange.Loadout;
import org.nia.niamod.models.eco.GameChange.Route;
import org.nia.niamod.models.eco.GameChange.Stat;
import org.nia.niamod.models.eco.GameChange.Tax;

import java.util.Collection;
import java.util.List;

public class GameActions {
    public void apply(GameChange change) {
        switch (change) {
            case Stat(String territoryName, TerritoryUpgrade upgrade, int level) -> {
                pushStat(territoryName, upgrade, level);
            }
            case Headquarters(String territoryName) -> {
                pushHeadquarters(territoryName);
            }
            case Tax(String territoryName, int percent, boolean global) -> {
                if (global) {
                    pushGlobalTax(percent);
                } else {
                    pushTax(territoryName, percent);
                }
            }
            case Borders(String territoryName, boolean open, boolean global) -> {
                if (global) {
                    pushGlobalBorders(open);
                } else {
                    pushBorders(territoryName, open);
                }
            }
            case Route(String territoryName, TerritoryRoute route, boolean global) -> {
                if (global) {
                    pushGlobalRoute(route);
                } else {
                    pushRoute(territoryName, route);
                }
            }
            case Loadout(TerritoryLoadout loadout, List<String> territoryNames) -> {
                applyLoadout(loadout, territoryNames);
            }
            case null, default -> {
            }
        }

    }

    public List<TerritoryLoadout> loadLoadouts() {
        // TODO
        return Loadouts.defaults();
    }

    public void pushStat(String territoryName, TerritoryUpgrade stat, int level) {
        // TODO:
    }

    public void pushHeadquarters(String territoryName) {
        // TODO
    }

    public void pushTax(String territoryName, int percent) {
        // TODO
    }

    public void pushBorders(String territoryName, boolean open) {
        // TODO
    }

    public void pushRoute(String territoryName, TerritoryRoute route) {
        // TODO
    }

    public void pushGlobalTax(int percent) {
        // TODO
    }

    public void pushGlobalBorders(boolean open) {
        // TODO
    }

    public void pushGlobalRoute(TerritoryRoute route) {
        // TODO
    }

    public void applyLoadout(TerritoryLoadout loadout, Collection<String> territoryNames) {
        // TODO
    }
}
