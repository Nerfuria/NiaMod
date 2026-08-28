package org.nia.niamod.models.eco;

import java.util.Collection;
import java.util.List;

public sealed interface GameChange permits GameChange.Stat, GameChange.Headquarters,
        GameChange.Tax, GameChange.Borders, GameChange.Route, GameChange.Loadout {
    static String cleanTerritoryName(String value) {
        return value == null ? "" : value.trim();
    }

    record Stat(String territoryName, TerritoryUpgrade upgrade, int level) implements GameChange {
        public Stat {
            territoryName = GameChange.cleanTerritoryName(territoryName);
        }
    }

    record Headquarters(String territoryName) implements GameChange {
        public Headquarters {
            territoryName = GameChange.cleanTerritoryName(territoryName);
        }
    }

    record Tax(String territoryName, int percent, boolean global) implements GameChange {
        public Tax(String territoryName, int percent) {
            this(territoryName, percent, false);
        }

        public static Tax global(int percent) {
            return new Tax("", percent, true);
        }

        public Tax {
            territoryName = global ? "" : GameChange.cleanTerritoryName(territoryName);
        }
    }

    record Borders(String territoryName, boolean open, boolean global) implements GameChange {
        public Borders(String territoryName, boolean open) {
            this(territoryName, open, false);
        }

        public static Borders global(boolean open) {
            return new Borders("", open, true);
        }

        public Borders {
            territoryName = global ? "" : GameChange.cleanTerritoryName(territoryName);
        }
    }

    record Route(String territoryName, TerritoryRoute route, boolean global) implements GameChange {
        public Route(String territoryName, TerritoryRoute route) {
            this(territoryName, route, false);
        }

        public static Route global(TerritoryRoute route) {
            return new Route("", route, true);
        }

        public Route {
            territoryName = global ? "" : GameChange.cleanTerritoryName(territoryName);
        }
    }

    record Loadout(TerritoryLoadout loadout, List<String> territoryNames) implements GameChange {
        public Loadout(TerritoryLoadout loadout, Collection<String> territoryNames) {
            this(loadout, territoryNames == null
                    ? List.of()
                    : territoryNames.stream()
                    .map(GameChange::cleanTerritoryName)
                    .filter(territoryName -> !territoryName.isEmpty())
                    .toList());
        }

        public Loadout {
            territoryNames = territoryNames == null ? List.of() : List.copyOf(territoryNames);
        }
    }
}
