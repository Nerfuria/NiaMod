package org.nia.niamod.functions;

import com.wynntils.core.consumers.functions.Function;
import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

public abstract class NiaFunction<T> extends Function<T> {

    @Override
    public String getTranslation(String keySuffix, Object... parameters) {
        return I18n.get(
                getTypeName().toLowerCase(Locale.ROOT) + ".niamod." + getTranslationKeyName() + "." + keySuffix,
                parameters);
    }
}
