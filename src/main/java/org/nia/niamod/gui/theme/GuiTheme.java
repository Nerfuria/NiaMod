package org.nia.niamod.gui.theme;

import lombok.Builder;

@Builder
public record GuiTheme(int background, int secondary, int textColor, int secondaryText, int trinaryText,
                       int overlay, int accentColor, int shadowColor, int sliderTrack, int scrollbarColor) {
    public static GuiTheme defaultTheme() {
        return GuiTheme.builder()
                .background(0xFF171A21)
                .secondary(0xFF11141B)
                .textColor(0xFFFFFFFF)
                .secondaryText(0xDCFFFFFF)
                .trinaryText(0x82FFFFFF)
                .overlay(0x26000000)
                .accentColor(0xFF4794FD)
                .shadowColor(0x18000000)
                .sliderTrack(0xFF171A21)
                .scrollbarColor(0x30FFFFFF)
                .build();
    }
}
