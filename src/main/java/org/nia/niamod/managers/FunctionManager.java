package org.nia.niamod.managers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.arguments.Argument;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.functions.WarFunctions;

public class FunctionManager {
    public static void init() {
        try {
            registerAllFunctions();
        } catch (AssertionError ae) {
            NiamodClient.LOGGER.error("Fix i18n for functions", ae);
            if (WynntilsMod.isDevelopmentEnvironment()) {
                System.exit(1);
            }
        }
    }

    private static void registerFunction(Function<?> function) {
        Managers.Function.getFunctions().add(function);

        var name = function.getTranslatedName();
        assert !name.startsWith("function.niamod.")
                : "Fix i18n name for " + name;
        var description = function.getDescription();
        assert !description.startsWith("function.niamod.")
                : "Fix i18n description for " + description;
        for (Argument<?> argument : function.getArgumentsBuilder().getArguments()) {
            var argument_description = function.getArgumentDescription(argument.getName());
            assert !argument_description.startsWith("function.niamod.")
                    : "Fix i18n argument description for " + argument_description;
        }
    }

    private static void registerAllFunctions() {
        registerFunction(new WarFunctions.ResTickFunction());
        registerFunction(new WarFunctions.HqTimerFunction());
    }
}
