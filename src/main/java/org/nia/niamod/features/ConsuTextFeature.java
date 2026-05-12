package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.gear.type.ConsumableType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.TextShadow;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.SlotRenderEvent;
import org.nia.niamod.models.records.StatLabel;
import org.nia.niamod.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public class ConsuTextFeature extends Feature {
    public static final String NO_TEXTURE_CATEGORY = "no_change";

    private static final String LABEL_DATA_KEY = "LABELTXT";
    private static final String MODEL_FLAG = "CUSTOM_MODEL";
    private static final Identifier LABELS = Identifier.fromNamespaceAndPath("niamod", "consumable_labels.json");
    private static final Identifier RELOADER_ID = Identifier.fromNamespaceAndPath("niamod", "consumable_textures");
    private static final Type LABEL_LIST_TYPE = new TypeToken<List<StatLabel>>() {
    }.getType();
    private static final Gson GSON = new Gson();

    private static boolean reloadersRegistered;
    private static List<StatLabel> statLabels = List.of();
    private static List<String> textureCategories = List.of(NO_TEXTURE_CATEGORY);
    private static Set<Identifier> textureModels = Set.of();

    public static void registerResourceReloaders() {
        if (reloadersRegistered) {
            return;
        }
        reloadersRegistered = true;
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloader(RELOADER_ID, (ResourceManagerReloadListener) ConsuTextFeature::reloadResources);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> reloadResources(client.getResourceManager()));
    }

    private static void reloadResources(ResourceManager resources) {
        statLabels = loadLabels(resources);
        textureModels = scanTextureModels(resources);
        textureCategories = buildTextureCategories();
        refreshLoadedItemModels();
    }

    public static List<String> textureCategoryOptions() {
        return textureCategories;
    }

    public static String resolveTextureCategory(String category) {
        String normalised = normalise(category);
        for (String option : textureCategories) {
            if (normalise(option).equals(normalised) || normalise(textureCategoryLabel(option)).equals(normalised)) {
                return option;
            }
        }
        return !normalised.isBlank() && !normalise(NO_TEXTURE_CATEGORY).equals(normalised) && textureModels.isEmpty()
                ? category.trim()
                : NO_TEXTURE_CATEGORY;
    }

    public static String textureCategoryLabel(String category) {
        if (category == null || category.isBlank() || NO_TEXTURE_CATEGORY.equals(category)) {
            return "No Change";
        }

        String text = category.trim().replace('-', '_');
        int namespace = text.indexOf(':');
        if (namespace >= 0 && namespace < text.length() - 1) {
            text = text.substring(namespace + 1);
        }

        StringBuilder label = new StringBuilder();
        for (String word : text.split("_+")) {
            if (word.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return label.isEmpty() ? "No Change" : label.toString();
    }

    public static void updateConsumableMetadata(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!NyahConfig.getData().isConsuTextFeatureEnabled()) {
            clearMetadata(stack);
            return;
        }
        if (statLabels().isEmpty()) {
            return;
        }

        StoredMetadata previous = storedMetadata(stack);
        Optional<DetectedConsumable> detected = detect(stack);
        if (detected.isEmpty()) {
            preserveStoredModel(stack, previous);
            return;
        }

        DetectedConsumable consumable = detected.get();
        Identifier model = customModel(consumable.label(), consumable.type());
        clearMetadata(stack);
        writeMetadata(stack, consumable.text(), model);
        if (model != null) {
            stack.set(DataComponents.ITEM_MODEL, model);
        }
    }

    public static void refreshLoadedItemModels() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Set<ItemStack> refreshed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            refreshStack(minecraft.player.getInventory().getItem(i), refreshed);
        }
        refreshMenu(minecraft.player.inventoryMenu, refreshed);
        refreshMenu(minecraft.player.containerMenu, refreshed);
    }

    public static void renderFloatingLabel(GuiGraphics context, ItemStack stack, int itemX, int itemY) {
        if (NyahConfig.getData().isConsuTextFeatureEnabled()) {
            renderLabel(context, stack, itemX, itemY);
        }
    }

    private static List<StatLabel> statLabels() {
        if (statLabels.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            statLabels = loadLabels(minecraft.getResourceManager());
        }
        return statLabels;
    }

    private static List<StatLabel> loadLabels(ResourceManager resources) {
        List<StatLabel> labels = new ArrayList<>();
        if (resources != null) {
            for (Resource resource : resources.getResourceStack(LABELS)) {
                labels.addAll(readLabels(resource));
            }
        }
        if (labels.isEmpty()) {
            labels.addAll(parseLabels(FileUtils.readFile("assets/niamod/consumable_labels.json")));
        }
        return labels.stream()
                .filter(label -> label != null
                        && label.ids() != null
                        && !label.ids().isEmpty()
                        && label.alias() != null
                        && !label.alias().isBlank())
                .toList();
    }

    private static List<StatLabel> readLabels(Resource resource) {
        try (InputStream stream = resource.open()) {
            return parseLabels(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read consumable labels: " + LABELS, e);
        }
    }

    private static List<StatLabel> parseLabels(String json) {
        try {
            List<StatLabel> labels = GSON.fromJson(json, LABEL_LIST_TYPE);
            return labels == null ? List.of() : labels;
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Invalid consumable labels JSON: " + LABELS, e);
        }
    }

    private static Set<Identifier> scanTextureModels(ResourceManager resources) {
        if (resources == null) {
            return Set.of();
        }

        LinkedHashSet<Identifier> models = new LinkedHashSet<>();
        resources.listResources("items", id -> modelFromItemDefinition(id) != null)
                .keySet()
                .stream()
                .map(ConsuTextFeature::modelFromItemDefinition)
                .forEach(models::add);
        return Collections.unmodifiableSet(models);
    }

    private static Identifier modelFromItemDefinition(Identifier resource) {
        String path = resource.getPath();
        if (!path.endsWith(".json")) {
            return null;
        }

        String[] parts = path.substring(0, path.length() - ".json".length()).split("/");
        if (parts.length < 4
                || !"items".equals(parts[0])
                || !isTypedConsumable(ConsumableType.fromString(parts[parts.length - 1]))) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(
                resource.getNamespace(), String.join("/", Arrays.copyOfRange(parts, 1, parts.length)));
    }

    private static List<String> buildTextureCategories() {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        categories.add(NO_TEXTURE_CATEGORY);
        textureModels.stream().map(ConsuTextFeature::categoryFromModel).filter(Objects::nonNull).forEach(categories::add);
        return List.copyOf(categories);
    }

    private static void refreshMenu(AbstractContainerMenu menu, Set<ItemStack> refreshed) {
        if (menu == null) {
            return;
        }
        for (Slot slot : menu.slots) {
            refreshStack(slot.getItem(), refreshed);
        }
        refreshStack(menu.getCarried(), refreshed);
    }

    private static void refreshStack(ItemStack stack, Set<ItemStack> refreshed) {
        if (stack != null && !stack.isEmpty() && refreshed.add(stack)) {
            updateConsumableMetadata(stack);
        }
    }

    private static Optional<DetectedConsumable> detect(ItemStack stack) {
        Optional<CraftedConsumableItem> wynnItem = craftedConsumable(stack);
        if (wynnItem.isEmpty() || !isTypedConsumable(wynnItem.get().getConsumableType())) {
            return Optional.empty();
        }

        CraftedConsumableItem consumable = wynnItem.get();
        List<StatActualValue> identifications = consumable.getIdentifications();
        StatLabel label = matchingLabel(identifications);
        return label == null
                ? Optional.empty()
                : Optional.of(new DetectedConsumable(consumable.getConsumableType(), label, labelText(label, identifications)));
    }

    private static Optional<CraftedConsumableItem> craftedConsumable(ItemStack stack) {
        Optional<CraftedConsumableItem> consumable = parseConsu(stack);
        if (consumable.isPresent()) {
            return consumable;
        }

        StoredMetadata stored = storedMetadata(stack);
        Identifier currentModel = stack.get(DataComponents.ITEM_MODEL);
        if (stored.model() == null || !stored.model().equals(currentModel)) {
            return Optional.empty();
        }

        stack.remove(DataComponents.ITEM_MODEL);
        try {
            return parseConsu(stack);
        } finally {
            stack.set(DataComponents.ITEM_MODEL, currentModel);
        }
    }

    private static Optional<CraftedConsumableItem> parseConsu(ItemStack stack) {
        Optional<WynnItem> item = Models.Item.getWynnItem(stack);
        if (item.isPresent() && item.get() instanceof CraftedConsumableItem consumable) {
            return Optional.of(consumable);
        }
        return Models.Item.asWynnItem(stack, CraftedConsumableItem.class);
    }

    private static StatLabel matchingLabel(List<StatActualValue> identifications) {
        Set<String> ids = new LinkedHashSet<>();
        for (StatActualValue identification : identifications) {
            String id = identification.statType().getApiName();
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }

        for (StatLabel label : statLabels()) {
            int required = label.minCount() == null ? 1 : label.minCount();
            if (label.ids().stream().filter(ids::contains).count() >= required) {
                return label;
            }
        }
        return null;
    }

    private static String labelText(StatLabel label, List<StatActualValue> identifications) {
        return !"{sign}ATK".equals(label.alias())
                ? label.alias()
                : identifications.stream()
                  .filter(identification -> "rawAttackSpeed".equals(identification.statType().getApiName()))
                  .findFirst()
                  .map(identification -> (identification.value() < 0 ? "-" : "+") + "ATK")
                  .orElse("ATK");
    }

    private static Identifier customModel(StatLabel label, ConsumableType type) {
        String category = resolveTextureCategory(NyahConfig.getData().getConsumableTextureCategory());
        if (NO_TEXTURE_CATEGORY.equals(category) || label.texture() == null || label.texture().isBlank()) {
            return null;
        }

        Identifier model = categoryTexture(category, label.texture(), type);
        return textureModels.contains(model) ? model : null;
    }

    private static Identifier categoryTexture(String category, String texture, ConsumableType type) {
        String namespace = "niamod";
        String path = category.trim();
        int separator = path.indexOf(':');
        if (separator >= 0) {
            namespace = path.substring(0, separator);
            path = path.substring(separator + 1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return Identifier.fromNamespaceAndPath(
                namespace, path + "/" + texture.trim() + "/" + type.name().toLowerCase(Locale.ROOT));
    }

    private static void clearMetadata(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }

        CompoundTag tag = data.copyTag();
        Identifier currentModel = stack.get(DataComponents.ITEM_MODEL);
        Identifier storedModel = identifier(tag.getStringOr(MODEL_FLAG, ""));
        if (storedModel != null && storedModel.equals(currentModel)) {
            stack.remove(DataComponents.ITEM_MODEL);
        }

        if (tag.contains(LABEL_DATA_KEY) || tag.contains(MODEL_FLAG)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, editable -> {
                editable.remove(LABEL_DATA_KEY);
                editable.remove(MODEL_FLAG);
            });
        }
    }

    private static void preserveStoredModel(ItemStack stack, StoredMetadata metadata) {
        if (metadata.model() != null && !metadata.model().equals(stack.get(DataComponents.ITEM_MODEL))) {
            stack.set(DataComponents.ITEM_MODEL, metadata.model());
        }
    }

    private static void writeMetadata(ItemStack stack, String label, Identifier model) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(LABEL_DATA_KEY, label);
            if (model != null) {
                tag.putString(MODEL_FLAG, model.toString());
            }
        });
    }

    private static StoredMetadata storedMetadata(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StoredMetadata.EMPTY;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return StoredMetadata.EMPTY;
        }

        CompoundTag tag = data.copyTag();
        return new StoredMetadata(tag.getStringOr(LABEL_DATA_KEY, ""), identifier(tag.getStringOr(MODEL_FLAG, "")));
    }

    private static void renderLabel(GuiGraphics context, ItemStack stack, int itemX, int itemY) {
        String label = storedMetadata(stack).label();
        if (label.isBlank() && stack != null && !stack.isEmpty() && !statLabels().isEmpty()) {
            label = detect(stack).map(DetectedConsumable::text).orElse("");
        }
        if (label.isBlank()) {
            return;
        }

        float scale = NyahConfig.getData().getIdScale();
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        FontRenderer.getInstance()
                .renderText(
                        context,
                        (itemX + NyahConfig.getData().getIdXOffset()) / scale,
                        (itemY + NyahConfig.getData().getIdYOffset()) / scale,
                        new TextRenderTask(
                                StyledText.fromUnformattedString(label),
                                TextRenderSetting.DEFAULT.withTextShadow(TextShadow.OUTLINE)));
        context.pose().popMatrix();
    }

    private static Identifier identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        int separator = trimmed.indexOf(':');
        return separator <= 0 || separator >= trimmed.length() - 1
                ? null
                : Identifier.fromNamespaceAndPath(trimmed.substring(0, separator), trimmed.substring(separator + 1));
    }

    private static String categoryFromModel(Identifier model) {
        String path = model.getPath();
        int separator = path.indexOf('/');
        if (separator <= 0) {
            return null;
        }
        String category = path.substring(0, separator);
        return "niamod".equals(model.getNamespace()) ? category : model.getNamespace() + ":" + category;
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static boolean isTypedConsumable(ConsumableType type) {
        return type == ConsumableType.FOOD || type == ConsumableType.POTION || type == ConsumableType.SCROLL;
    }

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
        registerResourceReloaders();
    }

    @Subscribe
    public void renderText(SlotRenderEvent event) {
        if (NyahConfig.getData().isConsuTextFeatureEnabled()) {
            updateConsumableMetadata(event.stack());
            renderLabel(event.context(), event.stack(), event.slotX(), event.slotY());
        }
    }

    private record DetectedConsumable(ConsumableType type, StatLabel label, String text) {
    }

    private record StoredMetadata(String label, Identifier model) {
        private static final StoredMetadata EMPTY = new StoredMetadata("", null);
    }
}
