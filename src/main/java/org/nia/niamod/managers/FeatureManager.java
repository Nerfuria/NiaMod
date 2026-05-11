package org.nia.niamod.managers;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.*;

@SuppressWarnings("unused")
@UtilityClass
public class FeatureManager {
    @Getter
    private static ResourceTickFeature resTickFeature;
    @Getter
    private static ChatEncryptionFeature chatEncryptionFeature;
    @Getter
    private static WarTimersFeature warTimersFeature;
    @Getter
    private static WarTowerEHPFeature warTowerEHPFeature;
    @Getter
    private static ConsuTextFeature consuTextFeature;
    @Getter
    private static ShoutFilterFeature shoutFilterFeature;
    @Getter
    private static ViewModelTransformationFeature viewModelTransformationFeature;
    @Getter
    private static RadianceSyncFeature radianceSyncFeature;
    @Getter
    private static AutoStreamFeature autoStreamFeature;
    @Getter
    private static IgnoreFeature ignoreFeature;
    @Getter
    private static DefenseEstimatesFeature defenseEstimatesFeature;

    public static void init() {
        resTickFeature = new ResourceTickFeature();
        chatEncryptionFeature = new ChatEncryptionFeature();
        warTimersFeature = new WarTimersFeature();
        warTowerEHPFeature = new WarTowerEHPFeature();
        consuTextFeature = new ConsuTextFeature();
        shoutFilterFeature = new ShoutFilterFeature();
        viewModelTransformationFeature = new ViewModelTransformationFeature();
        radianceSyncFeature = new RadianceSyncFeature();
        autoStreamFeature = new AutoStreamFeature();
        ignoreFeature = new IgnoreFeature();
        defenseEstimatesFeature = new DefenseEstimatesFeature();

        resTickFeature.runSafe("init", resTickFeature::init);
        chatEncryptionFeature.runSafe("init", chatEncryptionFeature::init);
        warTimersFeature.runSafe("init", warTimersFeature::init);
        warTowerEHPFeature.runSafe("init", warTowerEHPFeature::init);
        consuTextFeature.runSafe("init", consuTextFeature::init);
        shoutFilterFeature.runSafe("init", shoutFilterFeature::init);
        viewModelTransformationFeature.runSafe("init", viewModelTransformationFeature::init);
        radianceSyncFeature.runSafe("init", radianceSyncFeature::init);
        autoStreamFeature.runSafe("init", autoStreamFeature::init);
        ignoreFeature.runSafe("init", ignoreFeature::init);
        defenseEstimatesFeature.runSafe("init", defenseEstimatesFeature::init);

        NyahConfig.onFeaturesInitialized();
    }
}
