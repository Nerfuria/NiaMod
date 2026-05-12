package org.nia.niamod.managers;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.*;

import java.util.List;

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

        for (Feature feature : List.of(
                resTickFeature,
                chatEncryptionFeature,
                warTimersFeature,
                warTowerEHPFeature,
                consuTextFeature,
                shoutFilterFeature,
                viewModelTransformationFeature,
                radianceSyncFeature,
                autoStreamFeature,
                ignoreFeature,
                defenseEstimatesFeature
        )) {
            feature.runSafe("init", feature::init);
        }

        NyahConfig.onFeaturesInitialized();
    }
}
