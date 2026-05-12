package org.nia.niamod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.*;
import org.slf4j.Logger;

public class NiamodClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        KeybindManager.init();
        NyahConfig.init();
        SchedulerManager.init();
        OverlayManager.init();
        TerritoryBaseManager.init();
        FeatureManager.init();
        CommandManager.init();
    }
}
