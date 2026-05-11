package org.nia.niamod.models.records;

import java.util.List;

public record StatLabel(List<String> ids, String alias, Integer minCount, String texture) {
}
