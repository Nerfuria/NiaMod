package org.nia.niamod.commands.choices;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class EnumHelper {
    public <E extends Enum<E>> String[] enumNames(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}