package org.nia.niamod.config;

import lombok.experimental.UtilityClass;
import org.nia.niamod.config.choices.GuiAnimationMode;
import org.nia.niamod.config.choices.RadianceOverlayMode;
import org.nia.niamod.config.choices.SettingCategory;
import org.nia.niamod.config.choices.ShoutReplacement;
import org.nia.niamod.config.setting.*;
import org.nia.niamod.features.ConsuTextFeature;
import org.nia.niamod.gui.theme.FontOption;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.gui.theme.ThemeOption;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.OverlayManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
final class NyahConfigSections {
    static List<SettingSection> createSections() {
        List<SettingSection> sections = new ArrayList<>();
        sections.addAll(generalSections());
        sections.addAll(warSections());
        sections.addAll(socialSections());
        return sections;
    }

    private static List<SettingSection> generalSections() {
        return categorySections(
                SettingCategory.GENERAL,
                NyahConfigSections::clientSection,
                NyahConfigSections::guiSection,
                NyahConfigSections::viewModelSection
        );
    }

    private static List<SettingSection> warSections() {
        return categorySections(
                SettingCategory.WAR,
                NyahConfigSections::resourceTickSection,
                NyahConfigSections::territoryBoxesSection,
                NyahConfigSections::consumableLabelsSection,
                NyahConfigSections::towerEhpSection,
                NyahConfigSections::radianceSyncSection
        );
    }

    private static List<SettingSection> socialSections() {
        return categorySections(
                SettingCategory.SOCIAL,
                NyahConfigSections::chatEncryptionSection,
                NyahConfigSections::autoStreamSection,
                NyahConfigSections::shoutFilterSection
        );
    }

    private static List<SettingSection> categorySections(SettingCategory category, SectionFactory... factories) {
        return Arrays.stream(factories)
                .map(factory -> factory.create(category))
                .toList();
    }

    private static SettingSection clientSection(SettingCategory category) {
        return SettingSection.standard(
                "client",
                "Client",
                "Core Wynncraft data and shared client preferences.",
                category,
                null,
                null,
                List.of(
                        string("api_base", "API URL", "Base Wynncraft API URL.", NyahConfigData::getApiBase, NyahConfigData::setApiBase),
                        string("guild_name", "Guild Name", "Guild to load players from.", NyahConfigData::getGuildName, NyahConfigData::setGuildName)
                )
        );
    }

    private static SettingSection guiSection(SettingCategory category) {
        return SettingSection.standard(
                "gui",
                "GUI Settings",
                "Visual customizations for the Click GUI.",
                category,
                null,
                null,
                List.of(
                        choice("click_gui_theme", "GUI Theme", "Color scheme for the Click GUI.", NyahConfigData::getClickGuiTheme, NyahConfigSections::setClickGuiTheme, ThemeOption.keys(), ThemeOption::labelFor),
                        choice("click_gui_font", "GUI Font", "Font used in the Click GUI.", NyahConfigData::getClickGuiFont, NyahConfigData::setClickGuiFont, FontOption.keys(), FontOption::labelFor),
                        choice("click_gui_animation", "GUI Animation", "Opening and closing effect for the Click GUI.", NyahConfigData::getClickGuiAnimation, NyahConfigData::setClickGuiAnimation, GuiAnimationMode.class),
                        integer("animation_time", "Animation Time", "Time for config animation.", 100, 2000, NyahConfigData::getAnimationTime, NyahConfigData::setAnimationTime),
                        floating("gui_opacity", "GUI Opacity", "Overall background transparency.", 0.1f, 1.0f, NyahConfigData::getGuiOpacity, NyahConfigData::setGuiOpacity),
                        color("custom_gui_background", "Custom Background", "Background color for the Custom theme.", NyahConfigSections::getVisibleCustomGuiBackground, NyahConfigSections::setCustomGuiBackground),
                        color("custom_gui_secondary", "Custom Secondary", "Secondary color for the Custom theme.", NyahConfigSections::getVisibleCustomGuiSecondary, NyahConfigSections::setCustomGuiSecondary),
                        color("custom_gui_accent", "Custom Accent", "Accent color for the Custom theme.", NyahConfigSections::getVisibleCustomGuiAccent, NyahConfigSections::setCustomGuiAccent)
                )
        );
    }

    private static SettingSection chatEncryptionSection(SettingCategory category) {
        return featureSection(
                "chat_encryption",
                "Chat Encryption",
                "Encrypt and decode guild messages with a shared key.",
                category,
                NyahConfigData::isChatEncryptionFeatureEnabled,
                NyahConfigData::setChatEncryptionFeatureEnabled,
                List.of(
                        string("encryption_prefix", "Encryption Prefix", "Messages starting with this prefix are encrypted.", NyahConfigData::getEncryptionPrefix, NyahConfigData::setEncryptionPrefix),
                        string("encryption_key", "Encryption Key", "Shared AES key material.", NyahConfigData::getEncryptionKey, NyahConfigData::setEncryptionKey)
                )
        );
    }

    private static SettingSection autoStreamSection(SettingCategory category) {
        return featureSection(
                "auto_stream",
                "Auto Stream",
                "Automatically stream when you changed worlds",
                category,
                NyahConfigData::isAutoStreamFeatureEnabled,
                NyahConfigData::setAutoStreamFeatureEnabled,
                List.of(
                        integer("timeout", "Timeout", "Time to wait between stream boss bars before attempting to re-stream", 100, 10000, NyahConfigData::getStreamCooldown, NyahConfigData::setStreamCooldown)
                )
        );
    }

    private static SettingSection viewModelSection(SettingCategory category) {
        return featureSection(
                "view_model",
                "View Model",
                "Hand offsets, rotations",
                category,
                NyahConfigData::isViewModelFeatureEnabled,
                NyahConfigData::setViewModelFeatureEnabled,
                List.of(
                        integer("x_offset", "X Offset", "Horizontal offset in hundredths.", -150, 150, NyahConfigData::getXOffset, NyahConfigData::setXOffset),
                        integer("y_offset", "Y Offset", "Vertical offset in hundredths.", -150, 150, NyahConfigData::getYOffset, NyahConfigData::setYOffset),
                        integer("z_offset", "Z Offset", "Depth offset in hundredths.", -150, 50, NyahConfigData::getZOffset, NyahConfigData::setZOffset),
                        integer("x_rotation", "X Rotation", "Pitch rotation.", -180, 180, NyahConfigData::getXRotation, NyahConfigData::setXRotation),
                        integer("y_rotation", "Y Rotation", "Yaw rotation.", -180, 180, NyahConfigData::getYRotation, NyahConfigData::setYRotation),
                        integer("z_rotation", "Z Rotation", "Roll rotation.", -180, 180, NyahConfigData::getZRotation, NyahConfigData::setZRotation),
                        floating("item_scale", "Item Scale", "Held item scale multiplier.", 0.1f, 3.0f, NyahConfigData::getItemScale, NyahConfigData::setItemScale),
                        bool("disable_held_bobbing", "Disable Bobbing", "Renders hands without vanilla bobbing.", NyahConfigData::isDisableHeldBobbing, NyahConfigData::setDisableHeldBobbing)
                )
        );
    }

    private static SettingSection resourceTickSection(SettingCategory category) {
        return featureSection(
                "resource_tick",
                "Resource Tick",
                "Tracks the current guild resource tick from territory data.",
                category,
                NyahConfigData::isResourceTickFeatureEnabled,
                NyahConfigData::setResourceTickFeatureEnabled,
                List.of(
                        button("res_overlay", "Move Overlay", "Move the resource tick overlay", () -> OverlayManager.openConfig(List.of(FeatureManager.getResTickFeature().getResTickOverlay())), "Open Editor")
                )
        );
    }

    private static SettingSection territoryBoxesSection(SettingCategory category) {
        return featureSection(
                "territory_boxes",
                "Territory Boxes",
                "Draws upcoming attack territories directly in-world.",
                category,
                NyahConfigData::isWarTimersFeatureEnabled,
                NyahConfigData::setWarTimersFeatureEnabled,
                List.of(
                        color("territory_color", "Box Color", "Color used for queued territory outlines.", NyahConfigData::getColor, NyahConfigData::setColor),
                        color("territory_color_inside", "Inside Box Color", "Color used for queued territory outlines that you are inside.", NyahConfigData::getColorInside, NyahConfigData::setColorInside),
                        color("unqueued_territory_color", "Unqueued Box Color", "Color used for unqueued territory outlines.", NyahConfigData::getNotQColor, NyahConfigData::setNotQColor),
                        color("unqueued_territory_inside_color", "Inside Unqueued Box Color", "Color used for unqueued territory outlines that you are inside.", NyahConfigData::getNotQInsideColor, NyahConfigData::setNotQInsideColor),
                        integer("maximum_territories", "Max Territories", "Maximum queued territories to render.", 1, 30, NyahConfigData::getMaximumTerritories, NyahConfigData::setMaximumTerritories),
                        integer("maximum_distance", "Max Distance", "Max squared-distance filter input.", 50, 5000, NyahConfigData::getMaximumDistance, NyahConfigData::setMaximumDistance),
                        integer("maximum_terr_warn", "Max Territory Warning", "How many upcoming attack timers to warn you about (set 0 to disable)", 0, 10, NyahConfigData::getTerritoryWarningCount, NyahConfigData::setTerritoryWarningCount),
                        integer("max_time_warn", "Max Queue Time", "Maximum time of an attack timer to be included (min)", 1, 10, NyahConfigData::getMaxTimeTerr, NyahConfigData::setMaxTimeTerr),
                        integer("often_warn", "Warn Speed", "How many seconds between warns", 10, 120, NyahConfigData::getWarnTime, NyahConfigData::setWarnTime)
                )
        );
    }

    private static SettingSection consumableLabelsSection(SettingCategory category) {
        return featureSection(
                "consumable_labels",
                "Consumable Labels",
                "Compact crafted-consumable identifiers in inventories.",
                category,
                NyahConfigData::isConsuTextFeatureEnabled,
                NyahConfigSections::setConsuTextFeatureEnabled,
                List.of(
                        choice("texture_category", "Texture Category", "Model texture pack for consumable item icons.", NyahConfigData::getConsumableTextureCategory, NyahConfigSections::setConsumableTextureCategory, ConsuTextFeature::textureCategoryOptions, ConsuTextFeature::textureCategoryLabel),
                        floating("label_scale", "Label Scale", "Overlay text scale.", 0.25f, 2.5f, NyahConfigData::getIdScale, NyahConfigData::setIdScale),
                        integer("label_x_offset", "Label X Offset", "Horizontal label offset.", -16, 16, NyahConfigData::getIdXOffset, NyahConfigData::setIdXOffset),
                        integer("label_y_offset", "Label Y Offset", "Vertical label offset.", -16, 16, NyahConfigData::getIdYOffset, NyahConfigData::setIdYOffset)
                )
        );
    }

    private static SettingSection towerEhpSection(SettingCategory category) {
        return featureSection(
                "tower_ehp",
                "Tower EHP",
                "Replaces tower boss-bar HP with effective HP.",
                category,
                NyahConfigData::isWarTowerEhpFeatureEnabled,
                NyahConfigData::setWarTowerEhpFeatureEnabled,
                List.of()
        );
    }

    private static SettingSection shoutFilterSection(SettingCategory category) {
        return featureSection(
                "shout_filter",
                "Shout Filter",
                "Modify shouts to be less obstructive.",
                category,
                NyahConfigData::isShoutFilterFeatureEnabled,
                NyahConfigData::setShoutFilterFeatureEnabled,
                List.of(
                        choice("filter_mode", "Shout Filter Mode", "What to replace shouts with", NyahConfigData::getShoutFilterMode, NyahConfigData::setShoutFilterMode, ShoutReplacement.class)
                )
        );
    }

    private static SettingSection radianceSyncSection(SettingCategory category) {
        return featureSection(
                "radiance_sync",
                "Radiance Sync",
                "Tracks Radiance with Guard Partners in War",
                category,
                NyahConfigData::isRadianceSyncEnabled,
                NyahConfigData::setRadianceSyncEnabled,
                List.of(
                        string("group_key", "Group Key", "Shared sync group key (shared with party members).", NyahConfigData::getRadianceSyncGroupKey, NyahConfigData::setRadianceSyncGroupKey),
                        integer("self_aspect_tier", "Self Aspect Tier", "Your Radiance aspect tier (0 = none, 1-3).", 0, 3, NyahConfigData::getRadianceSyncSelfTier, NyahConfigData::setRadianceSyncSelfTier),
                        bool("require_war", "Require War", "Only show overlay while in war.", NyahConfigData::isRadianceSyncRequireWar, NyahConfigData::setRadianceSyncRequireWar),
                        choice("overlay_mode", "Overlay Mode", "cast = cast prompt mode; status = radiance remaining.", NyahConfigData::getRadianceSyncOverlayMode, NyahConfigData::setRadianceSyncOverlayMode, RadianceOverlayMode.class),
                        string("worker_url", "Worker URL", "WebSocket worker URL for Radiance sync.", NyahConfigData::getRadianceSyncWorkerUrl, NyahConfigData::setRadianceSyncWorkerUrl),
                        floating("cast_prompt_seconds", "Cast Prompt Seconds", "How long the cast prompt stays visible.", 0.5f, 10.0f, NyahConfigData::getRadianceSyncCastPromptSeconds, NyahConfigData::setRadianceSyncCastPromptSeconds),
                        bool("ping_sound", "Ping Sound", "Play a sound when Radiance is ready.", NyahConfigData::isRadianceSyncPingEnabled, NyahConfigData::setRadianceSyncPingEnabled),
                        floating("ping_volume", "Ping Volume", "Volume of the ping sound.", 0.1f, 2.0f, NyahConfigData::getRadianceSyncPingVolume, NyahConfigData::setRadianceSyncPingVolume),
                        floating("ping_pitch", "Ping Pitch", "Pitch of the ping sound.", 0.5f, 2.0f, NyahConfigData::getRadianceSyncPingPitch, NyahConfigData::setRadianceSyncPingPitch),
                        button("move_overlay", "Move Overlay", "Move the sync overlay", () -> OverlayManager.openConfig(List.of(FeatureManager.getRadianceSyncFeature().getOverlay())), "Open Editor")
                )
        );
    }

    private static SettingSection featureSection(
            String id,
            String title,
            String description,
            SettingCategory category,
            Function<NyahConfigData, Boolean> enabledGetter,
            BiConsumer<NyahConfigData, Boolean> enabledSetter,
            List<ConfigSetting<?>> settings
    ) {
        return SettingSection.standard(
                id,
                title,
                description,
                category,
                enabled(enabledGetter),
                sectionToggle(enabledSetter),
                settings
        );
    }

    private static NyahConfigData config() {
        return NyahConfig.getData();
    }

    private static java.util.function.Supplier<Boolean> enabled(Function<NyahConfigData, Boolean> getter) {
        return () -> getter.apply(config());
    }

    private static java.util.function.Consumer<Boolean> sectionToggle(BiConsumer<NyahConfigData, Boolean> setter) {
        return value -> {
            setter.accept(config(), value);
            NyahConfig.applyFeatureStates();
            NyahConfig.save();
        };
    }

    private static StringSetting string(String id, String title, String description, Function<NyahConfigData, String> getter, BiConsumer<NyahConfigData, String> setter) {
        return new StringSetting(id, title, description, () -> getter.apply(config()), persist(setter));
    }

    private static IntSetting integer(
            String id,
            String title,
            String description,
            int min,
            int max,
            Function<NyahConfigData, Integer> getter,
            BiConsumer<NyahConfigData, Integer> setter
    ) {
        return new IntSetting(id, title, description, min, max, () -> getter.apply(config()), persist(setter));
    }

    private static FloatSetting floating(
            String id,
            String title,
            String description,
            float min,
            float max,
            Function<NyahConfigData, Float> getter,
            BiConsumer<NyahConfigData, Float> setter
    ) {
        return new FloatSetting(id, title, description, min, max, () -> getter.apply(config()), persist(setter));
    }

    private static BooleanSetting bool(
            String id,
            String title,
            String description,
            Function<NyahConfigData, Boolean> getter,
            BiConsumer<NyahConfigData, Boolean> setter
    ) {
        return new BooleanSetting(id, title, description, () -> getter.apply(config()), persist(setter));
    }

    private static ColorSetting color(
            String id,
            String title,
            String description,
            Function<NyahConfigData, Integer> getter,
            BiConsumer<NyahConfigData, Integer> setter
    ) {
        return new ColorSetting(id, title, description, () -> getter.apply(config()), persist(setter));
    }

    private static ButtonSetting button(String id, String title, String description, Runnable action, String buttonText) {
        return new ButtonSetting(id, title, description, action, buttonText);
    }

    private static <T extends Enum<T>> ChoiceSetting choice(
            String id,
            String title,
            String description,
            Function<NyahConfigData, T> getter,
            BiConsumer<NyahConfigData, T> setter,
            Class<T> type
    ) {
        Function<String, String> labelResolver = raw -> {
            String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        };

        return new ChoiceSetting(
                id,
                title,
                description,
                () -> {
                    T value = getter.apply(config());
                    if (value == null) {
                        return type.getEnumConstants()[0].name();
                    }
                    return value.name();
                },
                value -> {
                    setter.accept(config(), T.valueOf(type, value));
                    NyahConfig.save();
                },
                Arrays.stream(type.getEnumConstants()).map(Enum::name).toList(),
                labelResolver
        );
    }

    private static ChoiceSetting choice(
            String id,
            String title,
            String description,
            Function<NyahConfigData, String> getter,
            BiConsumer<NyahConfigData, String> setter,
            List<String> options,
            Function<String, String> labelResolver
    ) {
        return new ChoiceSetting(
                id,
                title,
                description,
                () -> getter.apply(config()),
                value -> {
                    setter.accept(config(), value);
                    NyahConfig.save();
                },
                options,
                labelResolver
        );
    }

    private static ChoiceSetting choice(
            String id,
            String title,
            String description,
            Function<NyahConfigData, String> getter,
            BiConsumer<NyahConfigData, String> setter,
            Supplier<List<String>> options,
            Function<String, String> labelResolver
    ) {
        return new ChoiceSetting(
                id,
                title,
                description,
                () -> getter.apply(config()),
                value -> {
                    setter.accept(config(), value);
                    NyahConfig.save();
                },
                options,
                labelResolver
        );
    }

    private static <T> java.util.function.Consumer<T> persist(BiConsumer<NyahConfigData, T> setter) {
        return value -> {
            setter.accept(config(), value);
            NyahConfig.save();
        };
    }

    private static void setClickGuiTheme(NyahConfigData data, String value) {
        ThemeOption nextTheme = ThemeOption.resolve(value);
        keepCustomTheme(data);
        data.setClickGuiTheme(nextTheme.getKey());
    }

    private static void setConsumableTextureCategory(NyahConfigData data, String value) {
        data.setConsumableTextureCategory(ConsuTextFeature.resolveTextureCategory(value));
        ConsuTextFeature.refreshLoadedItemModels();
    }

    private static void setConsuTextFeatureEnabled(NyahConfigData data, Boolean value) {
        data.setConsuTextFeatureEnabled(value);
        ConsuTextFeature.refreshLoadedItemModels();
    }

    private static void setCustomGuiBackground(NyahConfigData data, Integer value) {
        updateCustomThemeColor(data, NyahConfigSections::getVisibleCustomGuiBackground, NyahConfigData::setCustomGuiBackground, value);
    }

    private static void setCustomGuiSecondary(NyahConfigData data, Integer value) {
        updateCustomThemeColor(data, NyahConfigSections::getVisibleCustomGuiSecondary, NyahConfigData::setCustomGuiSecondary, value);
    }

    private static void setCustomGuiAccent(NyahConfigData data, Integer value) {
        updateCustomThemeColor(data, NyahConfigSections::getVisibleCustomGuiAccent, NyahConfigData::setCustomGuiAccent, value);
    }

    private static void updateCustomThemeColor(
            NyahConfigData data,
            Function<NyahConfigData, Integer> visibleGetter,
            BiConsumer<NyahConfigData, Integer> setter,
            int value
    ) {
        int normalisedValue = value & 0xFFFFFF;
        int previousVisibleValue = visibleGetter.apply(data) & 0xFFFFFF;
        if (NyahConfig.getClickGuiThemeOption() != ThemeOption.CUSTOM && normalisedValue != previousVisibleValue) {
            copyThemeToCustom(data, NyahConfig.getClickGuiThemeOption().getTheme());
            data.setClickGuiTheme(ThemeOption.CUSTOM.getKey());
        }
        setter.accept(data, normalisedValue);
        NyahConfig.save();
    }

    private static int getVisibleCustomGuiBackground(NyahConfigData data) {
        if (NyahConfig.getClickGuiThemeOption() == ThemeOption.CUSTOM) {
            return data.getCustomGuiBackground() & 0xFFFFFF;
        }
        return NyahConfig.getClickGuiThemeOption().getTheme().background() & 0xFFFFFF;
    }

    private static int getVisibleCustomGuiSecondary(NyahConfigData data) {
        if (NyahConfig.getClickGuiThemeOption() == ThemeOption.CUSTOM) {
            return data.getCustomGuiSecondary() & 0xFFFFFF;
        }
        return NyahConfig.getClickGuiThemeOption().getTheme().secondary() & 0xFFFFFF;
    }

    private static int getVisibleCustomGuiAccent(NyahConfigData data) {
        if (NyahConfig.getClickGuiThemeOption() == ThemeOption.CUSTOM) {
            return data.getCustomGuiAccent() & 0xFFFFFF;
        }
        return NyahConfig.getClickGuiThemeOption().getTheme().accentColor() & 0xFFFFFF;
    }

    private static void keepCustomTheme(NyahConfigData data) {
        data.setCustomGuiBackground(data.getCustomGuiBackground() & 0xFFFFFF);
        data.setCustomGuiSecondary(data.getCustomGuiSecondary() & 0xFFFFFF);
        data.setCustomGuiAccent(data.getCustomGuiAccent() & 0xFFFFFF);
    }

    private static void copyThemeToCustom(NyahConfigData data, GuiTheme theme) {
        data.setCustomGuiBackground(theme.background() & 0xFFFFFF);
        data.setCustomGuiSecondary(theme.secondary() & 0xFFFFFF);
        data.setCustomGuiAccent(theme.accentColor() & 0xFFFFFF);
    }

    @FunctionalInterface
    private interface SectionFactory {
        SettingSection create(SettingCategory category);
    }
}
