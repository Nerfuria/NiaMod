package org.nia.niamod.gui;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.gui.theme.FontOption;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.gui.theme.ThemeOption;
import org.nia.niamod.mixin.EditBoxAccessor;

@UtilityClass
public class GuiStyle {
    public static Component styled(String text) {
        return Component.literal(text).withStyle(currentFontStyle());
    }

    public static int styledWidth(Font font, String text) {
        return font.width(styled(text));
    }

    public static void applyFont(EditBox editBox, String hintText) {
        editBox.addFormatter((text, cursor) -> styled(text).getVisualOrderText());
        if (hintText != null) {
            editBox.setHint(styled(hintText));
        }
    }

    public static void layoutBorderlessEditBox(EditBox editBox, Font font, int x, int y, int width, int height) {
        editBox.setX(x);
        editBox.setY(y);
        editBox.setWidth(width);
        editBox.setHeight(height);
        if (editBox instanceof EditBoxAccessor accessor) {
            accessor.niamod$setTextY(y + Math.max(0, (height - font.lineHeight) / 2));
        }
    }

    public static GuiTheme configuredTheme() {
        return configuredTheme(1.0);
    }

    public static GuiTheme configuredTheme(double opacityMultiplier) {
        try {
            GuiTheme base = baseTheme();
            int alpha = (int) (NyahConfig.getData().getGuiOpacity() * 255 * opacityMultiplier);
            alpha = Math.max(0, Math.min(255, alpha));

            return GuiTheme.builder()
                    .background((base.background() & 0x00FFFFFF) | (alpha << 24))
                    .secondary((base.secondary() & 0x00FFFFFF) | (alpha << 24))
                    .textColor(base.textColor())
                    .secondaryText(base.secondaryText())
                    .trinaryText(base.trinaryText())
                    .overlay(base.overlay())
                    .accentColor((base.accentColor() & 0x00FFFFFF) | 0xFF000000)
                    .shadowColor(base.shadowColor())
                    .sliderTrack((base.sliderTrack() & 0x00FFFFFF) | (alpha << 24))
                    .scrollbarColor(base.scrollbarColor())
                    .build();
        } catch (Exception e) {
            return GuiTheme.defaultTheme();
        }
    }

    private static GuiTheme baseTheme() {
        ThemeOption option = NyahConfig.getClickGuiThemeOption();
        if (option != ThemeOption.CUSTOM) {
            return option.getTheme();
        }

        return GuiTheme.builder()
                .background(NyahConfig.getData().getCustomGuiBackground())
                .secondary(NyahConfig.getData().getCustomGuiSecondary())
                .textColor(0xFFFFFFFF)
                .secondaryText(0xDCFFFFFF)
                .trinaryText(0x82FFFFFF)
                .overlay(0x26000000)
                .accentColor(NyahConfig.getData().getCustomGuiAccent())
                .shadowColor(0x18000000)
                .sliderTrack(NyahConfig.getData().getCustomGuiBackground())
                .scrollbarColor(0x30FFFFFF)
                .build();
    }

    private static Style currentFontStyle() {
        try {
            FontOption option = NyahConfig.getClickGuiFontOption();
            return Style.EMPTY.withFont(new FontDescription.Resource(option.fontDescriptionId()));
        } catch (Exception e) {
            return Style.EMPTY;
        }
    }
}
