package org.nia.niamod.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.config.choices.GuiAnimationMode;
import org.nia.niamod.config.choices.SettingCategory;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.gui.GuiStyle;
import org.nia.niamod.gui.animation.Animation;
import org.nia.niamod.gui.animation.Easing;
import org.nia.niamod.gui.component.SectionComponent;
import org.nia.niamod.gui.render.GuiRenderTargetOverride;
import org.nia.niamod.gui.render.GuiRenderTargetScope;
import org.nia.niamod.gui.render.RenderTarget;
import org.nia.niamod.gui.render.UiRect;
import org.nia.niamod.gui.theme.GuiTheme;
import org.nia.niamod.gui.theme.ThemeOption;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.managers.OverlayManager;
import org.nia.niamod.mixin.GameRendererAccessor;
import org.nia.niamod.render.Render2D;

import java.util.*;

public class ConfigScreen extends Screen {
    private static final int PORTAL_COLOR = 0xFF79E918;
    private static final int SIDEBAR_W = 100;
    private static final int ROUND = 12;
    private static final int MODULE_GAP = 7;
    private static final int SEARCH_BAR_HEIGHT = 24;
    private static final int MIN_PANEL_W = 320;
    private static final int MIN_PANEL_H = 260;
    private static final int MAX_PANEL_W = 1000;
    private static final int MAX_PANEL_H = 800;
    private static final int SCREEN_MARGIN = 12;
    private final Screen parent;
    private final Animation openAnim = new Animation(Easing.LINEAR, NyahConfig.getData().getAnimationTime());
    private final Animation closeAnim = new Animation(Easing.LINEAR, NyahConfig.getData().getAnimationTime());
    private final Animation scaleAnim = new Animation(Easing.EASE_OUT_EXPO, 300);
    private final Animation opacityAnim = new Animation(Easing.EASE_OUT_EXPO, 300);
    private final Map<SettingCategory, List<SectionComponent>> catComps = new EnumMap<>(SettingCategory.class);
    private final List<EditBox> textInputs = new ArrayList<>();
    private final List<TabButton> tabBtns = new ArrayList<>();
    private final Animation[] tabAnims = new Animation[GuiTab.values().length];
    private final List<SectionComponent> allComps = new ArrayList<>();
    private final List<SectionComponent> searchResults = new ArrayList<>();
    private int panelW;
    private int panelH;
    private int moduleW;
    private boolean opening;
    private boolean closing;
    private float panelX;
    private float panelY;
    private boolean dragging;
    private double dragOffX;
    private double dragOffY;
    private int resizeEdge;
    private float resizeStartW;
    private float resizeStartH;
    private GuiTab selectedTab = GuiTab.GENERAL;
    private List<SectionComponent> activeComps = new ArrayList<>();
    private boolean searchMode;
    private EditBox searchBox;
    private String searchQuery = "";
    private double scrollTarget;
    private double scroll;
    private long lastFrameTime;
    private RenderTarget portalSnapshot;
    private boolean portalSnapshotReady;
    private boolean portalCapturePending;
    private float portalSeedX;
    private float portalSeedY;
    private int lastRenderMouseX;
    private int lastRenderMouseY;
    private float lastRenderDelta;
    private ThemeOption lastRenderedTheme;
    private double animTime;

    public ConfigScreen(Screen parent) {
        super(Component.literal("NiaMod"));
        this.parent = parent;
    }

    private GuiTheme getTheme() {
        double opacityMultiplier = getAnimationMode() == GuiAnimationMode.NONE && animTime < 1.0
                ? opacityAnim.getValue()
                : 1.0;
        return GuiStyle.configuredTheme(opacityMultiplier);
    }

    @Override
    protected void init() {
        setPanelSize(NyahConfig.getData().getGuiWidth(), NyahConfig.getData().getGuiHeight());

        panelX = (width - panelW) / 2.0f;
        panelY = (height - panelH) / 2.0f;
        clearPortalSnapshot();
        rebuildWidgetsAndSections();

        opening = true;
        closing = false;
        resizeEdge = 0;

        openAnim.setEasing(Easing.LINEAR);
        openAnim.setDuration(getAnimationDurationMs());
        openAnim.setValue(0);

        closeAnim.setEasing(Easing.LINEAR);
        closeAnim.setDuration(getAnimationDurationMs());
        closeAnim.setValue(0);

        portalSnapshotReady = false;
        portalCapturePending = getAnimationMode() != GuiAnimationMode.NONE;

        scaleAnim.setEasing(Easing.EASE_OUT_EXPO);
        scaleAnim.setDuration(300);
        scaleAnim.setValue(0);
        scaleAnim.run(1);
        opacityAnim.setEasing(Easing.EASE_OUT_EXPO);
        opacityAnim.setDuration(300);
        opacityAnim.setValue(0);
        opacityAnim.run(1);

        lastFrameTime = System.currentTimeMillis();
        for (int i = 0; i < tabAnims.length; i++) {
            tabAnims[i] = new Animation(Easing.LINEAR, 200);
            tabAnims[i].setValue(selectedTab.ordinal() == i ? 255 : 0);
        }

        lastRenderedTheme = NyahConfig.getClickGuiThemeOption();
    }

    private void buildAll() {
        Map<String, Boolean> expandedStates = new java.util.HashMap<>();
        for (SectionComponent sectionComponent : allComps) {
            expandedStates.put(sectionComponent.getSection().title(), sectionComponent.isExpanded());
        }

        catComps.clear();
        allComps.clear();
        for (SettingCategory cat : SettingCategory.values()) {
            List<SectionComponent> list = new ArrayList<>();
            for (SettingSection sec : NyahConfig.getSections(cat)) {
                SectionComponent component = createSection(sec);
                Boolean wasExpanded = expandedStates.get(sec.title());
                if (wasExpanded != null) {
                    component.setExpanded(wasExpanded);
                }
                component.syncStateImmediately();
                list.add(component);
                allComps.add(component);
            }
            catComps.put(cat, list);
        }
    }

    private void rebuildWidgetsAndSections() {
        String preservedSearch = searchQuery;
        clearWidgets();
        textInputs.clear();
        initSearchBox(preservedSearch);
        buildAll();
        selectTab(selectedTab);
    }

    private void initSearchBox(String initialValue) {
        int searchInputHeight = Math.max(16, font.lineHeight + 4);
        searchBox = new EditBox(font, 0, 0, moduleW, searchInputHeight, GuiStyle.styled("Search"));
        searchBox.setBordered(false);
        searchBox.setHeight(searchInputHeight);
        searchBox.setTextColor(0xFFFFFFFF);
        GuiStyle.applyFont(searchBox, "Search...");
        searchBox.setResponder(q -> {
            searchQuery = q;
            updateSearch();
        });
        searchBox.setCanLoseFocus(true);
        searchBox.setValue(initialValue == null ? "" : initialValue);
        searchBox.visible = false;
        addRenderableWidget(searchBox);
        textInputs.add(searchBox);
        searchQuery = searchBox.getValue();
    }

    private SectionComponent createSection(SettingSection sec) {
        SectionComponent sectionComponent = new SectionComponent(sec);
        GuiTheme theme = getTheme();

        for (EditBox box : sectionComponent.createEditBoxes(font, theme)) {
            addRenderableWidget(box);
            textInputs.add(box);
        }

        return sectionComponent;
    }

    private void selectTab(GuiTab tab) {
        hideAllTextInputs();
        selectedTab = tab;
        searchMode = tab == GuiTab.SEARCH;
        scroll = scrollTarget = 0;
        if (searchMode) {
            searchBox.visible = true;
            searchBox.active = true;
            searchBox.setEditable(true);
            searchBox.setFocused(true);
            this.setFocused(searchBox);
            updateSearch();
            activeComps = searchResults;
        } else {
            searchBox.visible = false;
            searchBox.active = false;
            searchBox.setEditable(false);
            searchBox.setFocused(false);
            SettingCategory cat = switch (tab) {
                case WAR -> SettingCategory.WAR;
                case SOCIAL -> SettingCategory.SOCIAL;
                default -> SettingCategory.GENERAL;
            };
            activeComps = catComps.getOrDefault(cat, List.of());
        }
    }

    private void updateSearch() {
        hideAllTextInputs();
        searchResults.clear();
        if (searchQuery.isEmpty()) {
            searchResults.addAll(allComps);
        } else {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            for (SectionComponent component : allComps) {
                if (matchesSearch(component, q)) {
                    searchResults.add(component);
                }
            }
        }
        if (searchMode) {
            activeComps = searchResults;
        }
    }

    private boolean matchesSearch(SectionComponent component, String query) {
        SettingSection section = component.getSection();
        if (matchesQuery(section.title(), query) || matchesQuery(section.description(), query)) {
            return true;
        }

        return section.settings().stream().anyMatch(setting ->
                matchesQuery(setting.getTitle(), query)
                        || matchesQuery(setting.getDescription(), query)
                        || matchesQuery(setting.getId(), query)
        );
    }

    private boolean matchesQuery(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean hasFocusedTextInput() {
        return textInputs.stream().anyMatch(EditBox::isFocused);
    }

    private void hideAllTextInputs() {
        for (EditBox editBox : textInputs) {
            if (editBox == searchBox) continue;
            editBox.setFocused(false);
            editBox.visible = false;
            editBox.active = false;
            editBox.setEditable(false);
            editBox.setX(-300);
            editBox.setY(-300);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float dt) {
        ThemeOption currentTheme = NyahConfig.getClickGuiThemeOption();
        if (currentTheme != lastRenderedTheme) {
            rebuildWidgetsAndSections();
            lastRenderedTheme = currentTheme;
        }

        moduleW = moduleWidth();
        lastRenderMouseX = mouseX;
        lastRenderMouseY = mouseY;
        lastRenderDelta = dt;

        long now = System.currentTimeMillis();
        long frameDt = now - lastFrameTime;
        lastFrameTime = now;
        updateScrollPosition(frameDt);

        GuiAnimationMode animationMode = getAnimationMode();

        if (animationMode != GuiAnimationMode.NONE) {
            if (opening && portalSnapshotReady && isAnimationTransitionComplete()) {
                opening = false;
                clearPortalSnapshot();
            }
            if (closing && portalSnapshotReady && isAnimationTransitionComplete()) {
                minecraft.setScreen(parent);
                return;
            }

            if ((opening || closing) && portalCapturePending) {
                return;
            }

            if ((opening || closing) && portalSnapshotReady) {
                renderPortalTransition(g);
                return;
            }
        } else {
            if (closing) {
                scaleAnim.setEasing(Easing.LINEAR);
                scaleAnim.setDuration(100);
                scaleAnim.run(0);
                opacityAnim.setEasing(Easing.LINEAR);
                opacityAnim.setDuration(100);
                opacityAnim.run(0);
            }
            animTime = scaleAnim.getValue();
            if (closing && scaleAnim.isFinished()) {
                minecraft.setScreen(parent);
                return;
            }
            if (animTime <= 0) return;
        }

        if (dragging) {
            updateDragPosition(mouseX, mouseY);
        }

        GuiTheme theme = getTheme();

        if (animationMode == GuiAnimationMode.NONE) {
            int px = Math.round(panelX), py = Math.round(panelY);
            double cx = px + panelW / 2.0, cy = py + panelH / 2.0;

            g.pose().pushMatrix();
            if (animTime != 1) {
                g.pose().translate((float) (cx * (1 - animTime)), (float) (cy * (1 - animTime)));
                g.pose().scale((float) animTime, (float) animTime);
            }

            renderPanelFrame(g, mouseX, mouseY, dt, px, py, theme);

            g.pose().popMatrix();
        } else {
            renderPanelFrame(g, mouseX, mouseY, dt, Math.round(panelX), Math.round(panelY), theme);
        }
    }

    private void renderPanelFrame(GuiGraphics g, int mouseX, int mouseY, float dt, int px, int py, GuiTheme theme) {
        Render2D.dropShadow(g, new UiRect(px, py, panelW, panelH), 6, 0x26000000, ROUND);
        Render2D.shaderRoundedRect(g, px, py, panelW, panelH, ROUND, theme.background());
        Render2D.shaderRoundedRect(g, px, py, SIDEBAR_W, panelH, ROUND, theme.secondary());
        g.nextStratum();

        g.enableScissor(px + 1, py + 1, px + panelW - 1, py + panelH - 1);

        int contentX = px + SIDEBAR_W + 8;
        int contentW = panelW - SIDEBAR_W - 12;
        int viewH = panelH - 14;
        int maxScroll = maxScroll(viewH);
        clampScroll(maxScroll);
        renderContentArea(g, px, py, contentX, contentW, mouseX, mouseY, theme);

        super.render(g, mouseX, mouseY, dt);

        renderSidebar(g, px, py, mouseX, mouseY, theme);
        renderScrollbar(g, px + panelW - 4, py + 7 + (searchMode ? 35 : 0), viewH - (searchMode ? 35 : 0), maxScroll, theme);
        g.disableScissor();
    }

    public void renderPortalTransition(GuiGraphics g) {
        if (portalSnapshot == null || !portalSnapshotReady) {
            return;
        }

        int px = Math.round(panelX);
        int py = Math.round(panelY);
        float scaleX = minecraft.getWindow().getWidth() / (float) Math.max(1, width);
        float scaleY = minecraft.getWindow().getHeight() / (float) Math.max(1, height);
        int sourceX = Math.round(panelX * scaleX);
        int sourceY = Math.round(panelY * scaleY);
        int sourceWidth = Math.max(1, Math.round(panelW * scaleX));
        int sourceHeight = Math.max(1, Math.round(panelH * scaleY));
        float progress = (float) closeAnim.getValue();

        float signedProgress = opening ? -Math.max(0.01f, progress) : progress;
        GuiAnimationMode animationMode = getAnimationMode();
        switch (animationMode) {
            case INCINERATE -> Render2D.shaderIncinerateCapture(
                    g,
                    portalSnapshot,
                    px,
                    py,
                    panelW,
                    panelH,
                    signedProgress,
                    portalSeedX,
                    portalSeedY,
                    0xFFFFFFFF,
                    sourceX,
                    sourceY,
                    sourceWidth,
                    sourceHeight
            );
            case MUSHROOM -> Render2D.shaderMushroomCapture(
                    g,
                    portalSnapshot,
                    px,
                    py,
                    panelW,
                    panelH,
                    signedProgress,
                    portalSeedX,
                    portalSeedY,
                    0xFFFFFFFF,
                    sourceX,
                    sourceY,
                    sourceWidth,
                    sourceHeight
            );
            default -> Render2D.shaderPortalCapture(
                    g,
                    portalSnapshot,
                    px,
                    py,
                    panelW,
                    panelH,
                    signedProgress,
                    portalSeedX,
                    portalSeedY,
                    PORTAL_COLOR,
                    sourceX,
                    sourceY,
                    sourceWidth,
                    sourceHeight
            );
        }
    }

    private void ensurePortalSnapshot() {
        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int targetWidth = Math.max(1, mainTarget.width);
        int targetHeight = Math.max(1, mainTarget.height);
        if (portalSnapshot == null) {
            portalSnapshot = new RenderTarget("Nia ClickGUI Portal Snapshot", targetWidth, targetHeight, true);
            return;
        }

        if (portalSnapshot.width != targetWidth || portalSnapshot.height != targetHeight) {
            portalSnapshot.resize(targetWidth, targetHeight);
        }
    }

    private void clearPortalSnapshot() {
        portalSnapshotReady = false;
        portalCapturePending = false;
        if (portalSnapshot != null) {
            portalSnapshot.close();
            portalSnapshot = null;
        }
    }

    public boolean shouldPreparePortalSnapshot() {
        return getAnimationMode() != GuiAnimationMode.NONE && (opening || closing) && portalCapturePending && !portalSnapshotReady;
    }

    public void preparePortalSnapshotOffscreen() {
        if (!shouldPreparePortalSnapshot()) {
            return;
        }

        ensurePortalSnapshot();
        if (portalSnapshot == null) {
            return;
        }

        GameRenderer gameRenderer = minecraft.gameRenderer;
        if (!(gameRenderer instanceof GameRendererAccessor accessor)) {
            return;
        }

        FogRenderer fogRenderer = accessor.niamod$getFogRenderer();
        if (fogRenderer == null) {
            return;
        }

        GuiRenderState snapshotState = new GuiRenderState();
        GuiGraphics snapshotGraphics = new GuiGraphics(minecraft, snapshotState, width, height);
        GuiTheme theme = getTheme();

        portalSeedX = (float) Math.random();
        portalSeedY = (float) Math.random();

        RenderSystem.assertOnRenderThread();
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var colorTexture = portalSnapshot.getColorTexture();
        if (colorTexture != null) {
            encoder.clearColorTexture(colorTexture, 0);
        }
        if (portalSnapshot.getDepthTexture() != null) {
            encoder.clearDepthTexture(portalSnapshot.getDepthTexture(), 1.0);
        }

        try (GuiRenderer snapshotRenderer = new GuiRenderer(
                snapshotState,
                minecraft.renderBuffers().bufferSource(),
                gameRenderer.getSubmitNodeStorage(),
                gameRenderer.getFeatureRenderDispatcher(),
                List.of()
        ); GuiRenderTargetScope ignored = GuiRenderTargetOverride.push(portalSnapshot)) {
            renderPanelFrame(snapshotGraphics, lastRenderMouseX, lastRenderMouseY, lastRenderDelta, Math.round(panelX), Math.round(panelY), theme);
            snapshotRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            snapshotRenderer.incrementFrameNumber();
        }

        portalCapturePending = false;
        portalSnapshotReady = true;
        closeAnim.setEasing(Easing.LINEAR);
        closeAnim.setDuration(getAnimationDurationMs());
        closeAnim.setValue(0.0);
        closeAnim.run(1);
        hideAllTextInputs();
    }

    private GuiAnimationMode getAnimationMode() {
        GuiAnimationMode animationMode = NyahConfig.getData().getClickGuiAnimation();
        return animationMode == null ? GuiAnimationMode.NONE : animationMode;
    }

    private int getAnimationDurationMs() {
        return NyahConfig.getData().getAnimationTime();
    }

    private boolean isAnimationTransitionComplete() {
        if (portalSnapshotReady && closeAnim.isFinished()) {
            return true;
        }
        return closeAnim.getValue() >= (opening ? 1.0 : getAnimationCompletionThreshold());
    }

    private double getAnimationCompletionThreshold() {
        return switch (getAnimationMode()) {
            case INCINERATE -> 0.72;
            case MUSHROOM -> 0.94;
            case PORTAL -> 0.98;
            default -> 1.0;
        };
    }

    private void renderSidebar(GuiGraphics g, int px, int py, int mx, int my, GuiTheme theme) {
        g.fill(px + SIDEBAR_W - 1, py + 10, px + SIDEBAR_W, py + panelH - 10, 0x18FFFFFF);

        tabBtns.clear();
        int buttonX = px + 10;
        int buttonW = SIDEBAR_W - 20;
        double catY = py + 18;
        GuiTab[] tabs = GuiTab.values();
        String[] labels = {"Search", "General", "War", "Social"};

        for (int i = 0; i < tabs.length; i++) {
            int by = (int) Math.round(catY);
            int buttonH = 24;
            boolean sel = tabs[i] == selectedTab;
            boolean hovered = mx >= buttonX && mx <= buttonX + buttonW && my >= by && my <= by + buttonH;

            tabAnims[i].setDuration(200);
            tabAnims[i].run(sel ? 255 : 0);
            double sa = tabAnims[i].getValue();

            int buttonColor = hovered ? Render2D.withAlpha(0xFFFFFF, sel ? 24 : 10) : 0x00000000;
            if (buttonColor != 0) {
                Render2D.shaderRoundedRect(g, buttonX, by, buttonW, buttonH, 7, buttonColor);
            }

            if (sel) {
                Render2D.shaderRoundedRect(g, buttonX, by, buttonW, buttonH, 7, Render2D.withAlpha(theme.accentColor(), 32));
                Render2D.shaderRoundedRect(g, buttonX + 7, by + 10, 4, 4, 2, theme.accentColor());
            }

            int textColor = sel ? 0xFFFFFFFF : 0xC8FFFFFF;
            float xOff = sel ? (float) (sa / 140.0) : 0.0f;
            g.drawString(font, GuiStyle.styled(labels[i]), (int) (buttonX + 16 + xOff), by + 8, textColor, false);
            tabBtns.add(new TabButton(tabs[i], buttonX, by, buttonW, buttonH));
            catY += 30;
        }

        renderSidebarActionButton(g, "Ignore", buttonX, sidebarActionY(py, 2), buttonW, mx, my, theme);
        renderSidebarActionButton(g, "Overlays", buttonX, sidebarActionY(py, 1), buttonW, mx, my, theme);
        renderSidebarActionButton(g, "Reset Defaults", buttonX, sidebarActionY(py, 0), buttonW, mx, my, theme);
    }

    private void renderSidebarActionButton(GuiGraphics g, String label, int x, int y, int width, int mouseX, int mouseY, GuiTheme theme) {
        boolean hovered = insideSidebarAction(x, y, width, mouseX, mouseY);
        int fill = hovered ? Render2D.withAlpha(theme.secondary(), 242) : Render2D.withAlpha(theme.secondary(), 224);
        int border = hovered ? Render2D.withAlpha(0xFFFFFF, 56) : Render2D.withAlpha(0xFFFFFF, 26);
        Render2D.shaderRoundedSurface(g, x, y, width, 24, 7, fill, border);

        int textW = font.width(GuiStyle.styled(label));
        g.drawString(font, GuiStyle.styled(label), x + (width - textW) / 2, y + 8, hovered ? 0xFFFFFFFF : 0xD6FFFFFF, false);
    }

    private int sidebarActionY(int py, int indexFromBottom) {
        return py + panelH - 35 - indexFromBottom * 30;
    }

    private boolean insideSidebarAction(int x, int y, int width, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 24;
    }

    private void renderSearchBar(GuiGraphics g, int x, int y, int mouseX, int mouseY, GuiTheme theme) {
        UiRect rect = new UiRect(x, y, moduleW, SEARCH_BAR_HEIGHT);
        boolean focused = searchBox.isFocused();
        boolean hovered = mouseX >= rect.x() && mouseX <= rect.right() && mouseY >= rect.y() && mouseY <= rect.bottom();
        int fill = focused
                ? Render2D.withAlpha(theme.secondary(), 245)
                : hovered ? Render2D.withAlpha(theme.secondary(), 228) : Render2D.withAlpha(theme.secondary(), 214);
        int border = focused ? Render2D.withAlpha(theme.accentColor(), 105) : 0x20FFFFFF;

        Render2D.shaderRoundedSurface(g, rect.x(), rect.y(), rect.width(), rect.height(), 8, fill, border);
        Render2D.circle(g, rect.x() + 12, rect.y() + rect.height() / 2, 5, focused ? Render2D.withAlpha(theme.accentColor(), 220) : 0x66FFFFFF);

        GuiStyle.layoutBorderlessEditBox(searchBox, font, rect.x() + 22, rect.y(), rect.width() - 58, rect.height());

        searchBox.visible = true;
        searchBox.active = true;
        searchBox.setEditable(true);

        String count = searchResults.isEmpty() ? "0" : Integer.toString(searchResults.size());
        int countWidth = font.width(count);
        g.drawString(font, GuiStyle.styled(count), rect.right() - countWidth - 10, rect.y() + 8, 0x96FFFFFF, false);
    }

    private void renderModules(GuiGraphics g, int x, int y, int w, int viewportTop, int viewportBottom, int mx, int my, GuiTheme theme) {
        double moduleY = y + scroll;
        int moduleWidth = Math.min(moduleW, w);
        for (SectionComponent component : activeComps) {
            component.setPosition(x, (int) Math.round(moduleY), moduleWidth);
            component.setViewportClip(viewportTop, viewportBottom);
            component.render(g, font, mx, my, theme);
            moduleY += component.getHeight() + MODULE_GAP;
        }
    }

    private void renderScrollbar(GuiGraphics g, int sx, int sy, int sh, int maxScroll, GuiTheme theme) {
        if (maxScroll <= 0) {
            return;
        }
        int thumbH = Math.max(15, Math.round(sh * (float) sh / (sh + maxScroll)));
        float progress = (float) (-scroll / maxScroll);
        int thumbY = sy + Math.round((sh - thumbH) * progress);
        g.fill(sx, thumbY, sx + 1, thumbY + thumbH, theme.scrollbarColor());
    }

    private int totalHeight() {
        int total = 0;
        for (int i = 0; i < activeComps.size(); i++) {
            total += activeComps.get(i).getHeight();
            if (i < activeComps.size() - 1) {
                total += MODULE_GAP;
            }
        }
        return total;
    }

    private int maxScroll(int viewH) {
        return Math.max(0, totalHeight() - viewH + (searchMode ? 42 : 7));
    }

    private void updateScrollPosition(long frameDt) {
        double lerpFactor = 1.0 - Math.pow(0.005, frameDt / 1000.0);
        scroll += (scrollTarget - scroll) * Math.min(lerpFactor, 0.6);
        if (Math.abs(scrollTarget - scroll) < 0.3) {
            scroll = scrollTarget;
        }
    }

    private void updateDragPosition(double mouseX, double mouseY) {
        panelX = Math.max(-panelW + 50, Math.min(width - 50, (float) (mouseX + dragOffX)));
        panelY = Math.max(0, Math.min(height - 30, (float) (mouseY + dragOffY)));
    }

    private void setPanelSize(int panelW, int panelH) {
        this.panelW = clamp(panelW, minPanelWidth(), maxPanelWidth());
        this.panelH = clamp(panelH, minPanelHeight(), maxPanelHeight());
        this.moduleW = moduleWidth();
    }

    private int moduleWidth() {
        return Math.max(1, panelW - SIDEBAR_W - 17);
    }

    private int minPanelWidth() {
        return Math.min(MIN_PANEL_W, viewportMaxWidth());
    }

    private int minPanelHeight() {
        return Math.min(MIN_PANEL_H, viewportMaxHeight());
    }

    private int maxPanelWidth() {
        return Math.max(minPanelWidth(), Math.min(MAX_PANEL_W, viewportMaxWidth()));
    }

    private int maxPanelHeight() {
        return Math.max(minPanelHeight(), Math.min(MAX_PANEL_H, viewportMaxHeight()));
    }

    private int viewportMaxWidth() {
        return Math.max(1, width - SCREEN_MARGIN * 2);
    }

    private int viewportMaxHeight() {
        return Math.max(1, height - SCREEN_MARGIN * 2);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clampScroll(int maxScroll) {
        scrollTarget = Math.max(-maxScroll, Math.min(0, scrollTarget));
        scroll = Math.max(-maxScroll, Math.min(0, scroll));
    }

    private void renderContentArea(GuiGraphics g, int px, int py, int contentX, int contentW, int mouseX, int mouseY, GuiTheme theme) {
        if (searchMode) {
            renderSearchBar(g, contentX, py + 10, mouseX, mouseY, theme);
            g.enableScissor(contentX, py + 42, px + panelW - 4, py + panelH - 1);
            renderModules(g, contentX, py + 42, contentW, py + 42, py + panelH - 1, mouseX, mouseY, theme);
            g.disableScissor();
            return;
        }

        g.enableScissor(contentX, py + 7, px + panelW - 4, py + panelH - 1);
        renderModules(g, contentX, py + 7, contentW, py + 7, py + panelH - 1, mouseX, mouseY, theme);
        g.disableScissor();
    }

    private void resetToDefaults() {
        NyahConfig.reset();
        lastRenderedTheme = NyahConfig.getClickGuiThemeOption();
        rebuildWidgetsAndSections();
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean outside) {
        if (closing) {
            return true;
        }

        double mx = click.x();
        double my = click.y();
        int btn = click.button();
        int px = Math.round(panelX);
        int py = Math.round(panelY);

        clearTextFocusIfClickedOutside(mx, my);

        if (startResizeIfNearEdge(mx, my, px, py)) {
            return true;
        }

        if (btn == 0 && handleSidebarActionClick(mx, my, px, py)) {
            return true;
        }

        if (selectClickedTab(mx, my)) {
            return true;
        }

        if (btn == 0 && startPanelDrag(mx, my, px, py)) {
            return true;
        }

        for (SectionComponent component : activeComps) {
            if (component.mouseClicked(mx, my, btn)) {
                return true;
            }
        }
        return super.mouseClicked(click, outside);
    }

    private void clearTextFocusIfClickedOutside(double mouseX, double mouseY) {
        for (EditBox box : textInputs) {
            if (box.visible && box.active && box.isMouseOver(mouseX, mouseY)) {
                return;
            }
        }

        for (EditBox box : textInputs) {
            box.setFocused(false);
        }
        setFocused(null);
    }

    private boolean startResizeIfNearEdge(double mouseX, double mouseY, int panelLeft, int panelTop) {
        int margin = 6;
        boolean rightEdge = mouseX >= panelLeft + panelW - margin && mouseX <= panelLeft + panelW + margin;
        boolean bottomEdge = mouseY >= panelTop + panelH - margin && mouseY <= panelTop + panelH + margin;
        if ((!rightEdge && !bottomEdge) || mouseX < panelLeft || mouseY < panelTop) {
            return false;
        }

        resizeEdge = (rightEdge ? 1 : 0) | (bottomEdge ? 2 : 0);
        dragging = false;
        resizeStartW = panelW;
        resizeStartH = panelH;
        dragOffX = mouseX;
        dragOffY = mouseY;
        return true;
    }

    private boolean handleSidebarActionClick(double mouseX, double mouseY, int panelLeft, int panelTop) {
        int actionX = panelLeft + 10;
        int actionW = SIDEBAR_W - 20;
        if (insideSidebarAction(actionX, sidebarActionY(panelTop, 0), actionW, mouseX, mouseY)) {
            resetToDefaults();
            return true;
        }

        if (insideSidebarAction(actionX, sidebarActionY(panelTop, 1), actionW, mouseX, mouseY)) {
            OverlayManager.openConfig();
            return true;
        }

        if (insideSidebarAction(actionX, sidebarActionY(panelTop, 2), actionW, mouseX, mouseY)) {
            FeatureManager.getIgnoreFeature().openScreen();
            return true;
        }

        return false;
    }

    private boolean selectClickedTab(double mouseX, double mouseY) {
        for (TabButton tabBtn : tabBtns) {
            if (mouseX >= tabBtn.x() && mouseX <= tabBtn.x() + tabBtn.w() && mouseY >= tabBtn.y() && mouseY <= tabBtn.y() + tabBtn.h()) {
                selectTab(tabBtn.tab());
                return true;
            }
        }
        return false;
    }

    private boolean startPanelDrag(double mouseX, double mouseY, int panelLeft, int panelTop) {
        if (!isDragZone(mouseX, mouseY, panelLeft, panelTop)) {
            return false;
        }

        dragging = true;
        dragOffX = panelX - mouseX;
        dragOffY = panelY - mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (closing) {
            return true;
        }

        if (resizeEdge != 0) {
            int nextW = panelW;
            int nextH = panelH;
            if ((resizeEdge & 1) != 0) {
                nextW = (int) (resizeStartW + (event.x() - dragOffX));
            }
            if ((resizeEdge & 2) != 0) {
                nextH = (int) (resizeStartH + (event.y() - dragOffY));
            }
            setPanelSize(nextW, nextH);
            return true;
        }

        if (dragging) {
            updateDragPosition(event.x(), event.y());
            return true;
        }
        for (SectionComponent component : activeComps) {
            if (component.mouseDragged(event.x(), event.y(), event.button(), dx, dy)) {
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (closing) {
            return true;
        }

        if (resizeEdge != 0) {
            resizeEdge = 0;
            NyahConfig.getData().setGuiWidth(panelW);
            NyahConfig.getData().setGuiHeight(panelH);
            NyahConfig.save();
            return true;
        }

        if (dragging) {
            dragging = false;
            return true;
        }
        for (SectionComponent component : activeComps) {
            if (component.mouseReleased(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (closing) {
            return true;
        }

        int px = Math.round(panelX);
        int py = Math.round(panelY);
        if (mx >= px + SIDEBAR_W && mx <= px + panelW && my >= py && my <= py + panelH) {
            scrollTarget += v * 30;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (closing) {
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchMode && !searchQuery.isEmpty()) {
                searchBox.setValue("");
                return true;
            }
            onClose();
            return true;
        }
        if (!searchMode
                && !hasFocusedTextInput()
                && event.key() >= GLFW.GLFW_KEY_A
                && event.key() <= GLFW.GLFW_KEY_Z) {
            selectTab(GuiTab.SEARCH);
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (!closing) {
            dragging = false;
            resizeEdge = 0;
            hideAllTextInputs();
            if (getAnimationMode() == GuiAnimationMode.NONE) {
                clearPortalSnapshot();
                closing = true;
                scaleAnim.setEasing(Easing.LINEAR);
                scaleAnim.setDuration(100);
                scaleAnim.run(0);
                opacityAnim.setEasing(Easing.LINEAR);
                opacityAnim.setDuration(100);
                opacityAnim.run(0);
                return;
            }
            opening = false;
            closing = true;
            portalSnapshotReady = false;
            portalCapturePending = true;
        }
    }

    @Override
    public void removed() {
        clearPortalSnapshot();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isDragZone(double mx, double my, int px, int py) {
        if (mx >= px && mx <= px + panelW && my >= py && my <= py + 8) {
            return true;
        }

        if (mx < px || mx > px + SIDEBAR_W || my < py || my > py + panelH) {
            return false;
        }

        for (TabButton tabBtn : tabBtns) {
            if (mx >= tabBtn.x() && mx <= tabBtn.x() + tabBtn.w() && my >= tabBtn.y() && my <= tabBtn.y() + tabBtn.h()) {
                return false;
            }
        }

        return true;
    }

}
