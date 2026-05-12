package org.nia.niamod.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.choices.SettingCategory;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.features.Feature;
import org.nia.niamod.gui.screen.ConfigScreen;
import org.nia.niamod.gui.theme.FontOption;
import org.nia.niamod.gui.theme.ThemeOption;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.KeybindManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
public class NyahConfig {
    private static final Gson GSON = new Gson();
    private static final List<SettingSection> SECTIONS = List.copyOf(NyahConfigSections.createSections());

    @Getter
    private static NyahConfigData data = new NyahConfigData();

    public static void init() {
        loadConfig();
        KeybindManager.registerKeybinding(
                "Open NiaMod Click GUI",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                () -> minecraft().setScreen(getConfigScreen())
        );
    }

    public static void onFeaturesInitialized() {
        applyFeatureStates();
    }

    public static void reset() {
        data = new NyahConfigData();
        data.normalise();
        save();
        applyFeatureStates();
    }

    public static Screen getConfigScreen() {
        return getConfigScreen(minecraft().screen);
    }

    public static Screen getConfigScreen(Screen currentScreen) {
        return new ConfigScreen(currentScreen);
    }

    public static List<SettingSection> getSections(SettingCategory category) {
        return SECTIONS.stream()
                .filter(section -> section.category() == category)
                .toList();
    }

    public static ThemeOption getClickGuiThemeOption() {
        return ThemeOption.resolve(getData().getClickGuiTheme());
    }

    public static FontOption getClickGuiFontOption() {
        return FontOption.resolve(getData().getClickGuiFont());
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            try (Writer writer = Files.newBufferedWriter(configFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Failed to save config", exception);
        }
    }

    public static void applyFeatureStates() {
        NyahConfigData config = getData();
        applyEnabledState(FeatureManager.getResTickFeature(), config.isResourceTickFeatureEnabled());
        applyEnabledState(FeatureManager.getChatEncryptionFeature(), config.isChatEncryptionFeatureEnabled());
        applyEnabledState(FeatureManager.getWarTimersFeature(), config.isWarTimersFeatureEnabled());
        applyEnabledState(FeatureManager.getWarTowerEHPFeature(), config.isWarTowerEhpFeatureEnabled());
        applyEnabledState(FeatureManager.getConsuTextFeature(), config.isConsuTextFeatureEnabled());
        applyEnabledState(FeatureManager.getShoutFilterFeature(), config.isShoutFilterFeatureEnabled());
        applyEnabledState(FeatureManager.getViewModelTransformationFeature(), config.isViewModelFeatureEnabled());
        applyEnabledState(FeatureManager.getRadianceSyncFeature(), config.isRadianceSyncEnabled());
        applyEnabledState(FeatureManager.getAutoStreamFeature(), config.isAutoStreamFeatureEnabled());
    }

    private static void applyEnabledState(Feature feature, boolean enabled) {
        if (feature != null) {
            feature.setEnabled(enabled);
        }
    }

    private static void loadConfig() {
        if (!Files.exists(configFile())) {
            data = new NyahConfigData();
            data.normalise();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile())) {
            data = GSON.fromJson(reader, NyahConfigData.class);
        } catch (IOException exception) {
            NiamodClient.LOGGER.error("Error loading config file!", exception);
            data = new NyahConfigData();
        }

        if (data == null) {
            data = new NyahConfigData();
        }
        data.normalise();
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    private static Path configDir() {
        return minecraft().gameDirectory.toPath().resolve("config");
    }

    private static Path configFile() {
        return configDir().resolve("nyah-mod.json");
    }
}
