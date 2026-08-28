package org.nia.niamod.features.eco;

import org.nia.niamod.features.TerritoryManagerFeature;
import org.nia.niamod.models.eco.TerritoryUpgrade;

public record GuiActions(TerritoryManagerFeature feature, String territoryName) {
    public GuiActions(TerritoryManagerFeature feature, String territoryName) {
        this.feature = feature;
        this.territoryName = territoryName == null ? "" : territoryName.trim();
    }

    public void adjustUpgrade(TerritoryUpgrade upgrade, int delta) {
        if (feature != null) {
            feature.adjustStat(territoryName, upgrade, delta);
        }
    }

    public void adjustTax(int delta) {
        if (feature != null) {
            feature.adjustTax(territoryName, delta);
        }
    }

    public void setTax(int percent) {
        if (feature != null) {
            feature.setTax(territoryName, percent);
        }
    }

    public void setHeadquarters() {
        if (feature != null) {
            feature.setHeadquarters(territoryName);
        }
    }

    public void toggleBorders() {
        if (feature != null) {
            feature.toggleBordersOpen(territoryName);
        }
    }

    public void toggleRoute() {
        if (feature != null) {
            feature.toggleTerritoryRoute(territoryName);
        }
    }
}
