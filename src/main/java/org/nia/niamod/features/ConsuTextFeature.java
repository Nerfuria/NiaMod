package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.nia.niamod.models.misc.ConsuType;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.StatLabel;
import org.nia.niamod.util.FileUtils;

import java.util.List;

@SuppressWarnings("unused")
public class ConsuTextFeature extends Feature {
    private static List<StatLabel> STAT_LABELS = List.of();

    @Safe
    public void init() {
        STAT_LABELS = parseStatLabels();
        NiaEventBus.subscribe(this);
    }

    private List<StatLabel> parseStatLabels() {
        String json = FileUtils.readFile("stat_labels.json");
        return new Gson().fromJson(json, new TypeToken<List<StatLabel>>() {
        }.getType());
    }

    @Subscribe
    @Safe
    public void renderText(SlotRenderEvent event) {
        GuiGraphics context = event.context();
        ItemStack stack = event.stack();
        int slotX = event.slotX();
        int slotY = event.slotY();
        CustomConsumable consumable = customConsumable(stack);
        if (consumable == null || consumable.label().isEmpty()) return;

        context.pose().pushMatrix();
        context.pose().scale(NyahConfig.getData().getIdScale(), NyahConfig.getData().getIdScale());
        float x = (slotX + NyahConfig.getData().getIdXOffset()) / NyahConfig.getData().getIdScale();
        float y = (slotY + NyahConfig.getData().getIdYOffset()) / NyahConfig.getData().getIdScale();
        FontRenderer.getInstance().renderText(context, x, y, new TextRenderTask(StyledText.fromUnformattedString(consumable.label()), TextRenderSetting.DEFAULT.withTextShadow(TextShadow.OUTLINE)));
        context.pose().popMatrix();
    }

    public static ItemStack withCustomConsumableModel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !NyahConfig.getData().isConsuTextFeatureEnabled()) return stack;

        LocalPlayer player = Minecraft.getInstance().player;
        List<Component> lore = stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.ADVANCED);
        CustomConsumable consumable = customConsumable(stack, lore);
        if (consumable == null || consumable.texture() == null) return stack;

        ItemStack copy = stack.copy();
        copy.set(DataComponents.ITEM_MODEL, consumable.texture());
        return copy;
    }

    private CustomConsumable customConsumable(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        List<Component> lore = stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.ADVANCED);
        return customConsumable(stack, lore);
    }

    private static CustomConsumable customConsumable(ItemStack stack, List<Component> lore) {
        ConsuType type = getType(lore);
        if (type == ConsuType.NONE) return null;

        List<String> tooltip = lore
                .stream()
                .map(line -> line.getString().replaceAll("[^a-zA-Z %]", "").replaceAll("\\s+", " ").trim())
                .toList();

        int startIndex = -1;
        int endIndex = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains("Combat Level")) {
                startIndex = i + 2;
                break;
            }
        }
        if (startIndex == -1) return null;
        for (int i = startIndex; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains("Crafted by")) {
                endIndex = i;
                break;
            }
        }
        List<String> ids = tooltip.stream()
                .skip(startIndex)
                .limit(endIndex - startIndex)
                .filter(line -> !line.isBlank())
                .toList();

        StatLabel label = matchingLabel(ids);
        if (label == null || label.alias().isEmpty()) return null;

        Identifier texture = textureFor(label);
        return new CustomConsumable(type, label.alias(), texture);
    }

    private static Identifier textureFor(StatLabel label) {
        return identifier(label.texture());
    }

    private static Identifier identifier(String value) {
        if (value == null || value.isBlank()) return null;

        String trimmed = value.trim();
        int separator = trimmed.indexOf(':');
        if (separator <= 0 || separator >= trimmed.length() - 1) return null;

        return Identifier.fromNamespaceAndPath(trimmed.substring(0, separator), trimmed.substring(separator + 1));
    }

    private static StatLabel matchingLabel(List<String> statTypes) {
        for (StatLabel label : STAT_LABELS) {
            int matches = (int) label.ids().stream().filter(statTypes::contains).count();
            int required = label.minCount() != null ? label.minCount() : 1;
            if (matches < required) continue;
            return label;
        }

        return null;
    }

    private static ConsuType getType(List<Component> loreLines) {
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE035\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE033\uDAFF\uDFFF\uE062\uDAFF\uDFE6\uE005\uE00E\uE00E\uE003\uDB00\uDC02")))
            return ConsuType.FOOD;
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE042\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03B\uDAFF\uDFFF\uE03B\uDAFF\uDFFF\uE062\uDAFF\uDFDA\uE012\uE002\uE011\uE00E\uE00B\uE00B\uDB00\uDC02")))
            return ConsuType.SCROLL;
        if (loreLines.stream().anyMatch(l -> l.getString().contains("\uE03F\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE043\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE03D\uDAFF\uDFFF\uE062\uDAFF\uDFDC\uE00F\uE00E\uE013\uE008\uE00E\uE00D\uDB00\uDC02")))
            return ConsuType.POTION;
        return ConsuType.NONE;
    }

    private record CustomConsumable(ConsuType type, String label, Identifier texture) {
    }

}
