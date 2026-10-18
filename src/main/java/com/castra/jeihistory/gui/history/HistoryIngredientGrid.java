package com.castra.jeihistory.gui.history;

import com.castra.jeihistory.JeiHistoryConfig;
import com.castra.jeihistory.mixin.IngredientGridAccessor;
import com.castra.jeihistory.storage.HistoryStorage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IEditModeConfig;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.input.IClickableIngredientInternal;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.common.util.MathUtil;
import mezz.jei.core.config.IWorldConfig;
import mezz.jei.gui.config.IClientConfig;
import mezz.jei.gui.config.IIngredientFilterConfig;
import mezz.jei.gui.config.IIngredientGridConfig;
import mezz.jei.gui.input.handlers.DeleteItemInputHandler;
import mezz.jei.gui.overlay.IngredientGrid;
import mezz.jei.gui.overlay.IngredientListRenderer;
import mezz.jei.gui.overlay.IngredientListSlot;
import mezz.jei.gui.util.CheatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Replacement for JEI's ingredient list grid that reserves the bottom N rows for a
 * recipe lookup history.
 *
 * Ported from JEI Recipe History by Christofmeg (MIT). The dashed border rendering is
 * based on Roughly Enough Items (REI) by shedaniel.
 */
public class HistoryIngredientGrid extends IngredientGrid {
    private static final int INGREDIENT_PADDING = 1;
    private static final int MIN_ROWS = 5;
    private static final int BORDER_COLOR = 0xee555555;

    @Nullable
    public static HistoryIngredientGrid instance;

    private final IngredientGridAccessor accessor = (IngredientGridAccessor) this;
    private final IIngredientManager ingredientManager;
    private final IngredientListRenderer historyRenderer;
    private final List<ITypedIngredient<?>> historyList = new ArrayList<>();
    private final HistoryStorage historyStorage;

    private boolean showHistory;
    private int historyMaxSize;
    private int historyHeight;

    public HistoryIngredientGrid(
        IIngredientManager ingredientManager,
        IIngredientGridConfig gridConfig,
        IEditModeConfig editModeConfig,
        IIngredientFilterConfig ingredientFilterConfig,
        IClientConfig clientConfig,
        IWorldConfig worldConfig,
        IModIdHelper modIdHelper,
        IConnectionToServer serverConnection,
        IInternalKeyMappings keyMappings,
        IColorHelper colorHelper,
        CheatUtil cheatUtil
    ) {
        super(
            ingredientManager,
            gridConfig,
            editModeConfig,
            ingredientFilterConfig,
            clientConfig,
            worldConfig,
            modIdHelper,
            serverConnection,
            keyMappings,
            colorHelper,
            cheatUtil
        );
        this.ingredientManager = ingredientManager;
        this.historyRenderer = new IngredientListRenderer(editModeConfig, worldConfig, ingredientManager);
        this.historyStorage = HistoryStorage.create();
        this.historyStorage.load(ingredientManager, historyList);
    }

    @Override
    public void updateBounds(ImmutableRect2i availableArea, Set<ImmutableRect2i> guiExclusionAreas) {
        if (!JeiHistoryConfig.openHistory()) {
            historyRenderer.clear();
            showHistory = false;
            historyHeight = 0;
            super.updateBounds(availableArea, guiExclusionAreas);
            return;
        }

        super.updateBounds(availableArea, guiExclusionAreas);
        accessor.getIngredientListRenderer().clear();
        historyRenderer.clear();

        ImmutableRect2i area = getArea();
        showHistory = false;
        historyHeight = 0;
        if (area.isEmpty()) {
            return;
        }

        int rows = area.getHeight() / INGREDIENT_HEIGHT;
        int columns = area.getWidth() / INGREDIENT_WIDTH;
        int rowCount = Math.max(1, JeiHistoryConfig.historyRows());

        showHistory = rows - rowCount >= MIN_ROWS;
        historyMaxSize = rowCount * columns;
        historyHeight = showHistory ? rowCount * INGREDIENT_HEIGHT : 0;

        addSlots(
            accessor.getIngredientListRenderer(),
            area,
            area.getY(),
            area.getY() + area.getHeight() - historyHeight,
            guiExclusionAreas
        );

        if (showHistory) {
            addSlots(
                historyRenderer,
                area,
                area.getY() + area.getHeight() - historyHeight,
                area.getY() + area.getHeight(),
                guiExclusionAreas
            );
        }

        if (historyList.size() > historyMaxSize) {
            historyList.subList(historyMaxSize, historyList.size()).clear();
            historyStorage.save(ingredientManager, historyList);
        }
        historyRenderer.set(0, historyList);
    }

    private static void addSlots(
        IngredientListRenderer renderer,
        ImmutableRect2i area,
        int startY,
        int endY,
        Set<ImmutableRect2i> guiExclusionAreas
    ) {
        for (int y = startY; y < endY; y += INGREDIENT_HEIGHT) {
            for (int x = area.getX(); x < area.getX() + area.getWidth(); x += INGREDIENT_WIDTH) {
                IngredientListSlot slot = new IngredientListSlot(
                    x,
                    y,
                    INGREDIENT_WIDTH,
                    INGREDIENT_HEIGHT,
                    INGREDIENT_PADDING
                );
                slot.setBlocked(MathUtil.intersects(guiExclusionAreas, slot.getArea().expandBy(2)));
                renderer.add(slot);
            }
        }
    }

    @Override
    public void draw(Minecraft minecraft, PoseStack poseStack, int mouseX, int mouseY) {
        super.draw(minecraft, poseStack, mouseX, mouseY);
        if (!JeiHistoryConfig.openHistory() || !showHistory) {
            return;
        }

        RenderSystem.disableBlend();
        historyRenderer.render(poseStack);

        if (isMouseOver(mouseX, mouseY)) {
            DeleteItemInputHandler deleteHandler = (DeleteItemInputHandler) getInputHandler();
            if (!deleteHandler.shouldDeleteItemOnClick(minecraft, mouseX, mouseY)) {
                historyRenderer.getSlots()
                    .filter(slot -> slot.getArea().contains(mouseX, mouseY))
                    .filter(slot -> slot.getIngredientRenderer().isPresent())
                    .findFirst()
                    .ifPresent(slot -> drawHighlight(poseStack, slot.getArea()));
            }
        }

        ImmutableRect2i area = getArea();
        int endX = area.getX() + area.getWidth();
        int startY = area.getY() + area.getHeight() - historyHeight;
        int endY = area.getY() + area.getHeight();

        drawHorizontalDashedLine(poseStack, area.getX(), endX, startY, BORDER_COLOR, false);
        drawHorizontalDashedLine(poseStack, area.getX(), endX, endY, BORDER_COLOR, true);
        drawVerticalDashedLine(poseStack, area.getX(), startY, endY, BORDER_COLOR, false);
        drawVerticalDashedLine(poseStack, endX - 1, startY, endY, BORDER_COLOR, true);
    }

    @Override
    public void drawTooltips(Minecraft minecraft, PoseStack poseStack, int mouseX, int mouseY) {
        super.drawTooltips(minecraft, poseStack, mouseX, mouseY);
        if (!JeiHistoryConfig.openHistory() || !showHistory || !isMouseOver(mouseX, mouseY)) {
            return;
        }

        DeleteItemInputHandler deleteHandler = (DeleteItemInputHandler) getInputHandler();
        if (deleteHandler.shouldDeleteItemOnClick(minecraft, mouseX, mouseY)) {
            return;
        }

        historyRenderer.getSlots()
            .filter(slot -> slot.isMouseOver(mouseX, mouseY))
            .map(IngredientListSlot::getTypedIngredient)
            .flatMap(Optional::stream)
            .findFirst()
            .ifPresent(ingredient -> accessor.getTooltipHelper().drawTooltip(poseStack, mouseX, mouseY, ingredient));
    }

    @Override
    public @NotNull Stream<IClickableIngredientInternal<?>> getIngredientUnderMouse(double mouseX, double mouseY) {
        if (!JeiHistoryConfig.openHistory() || !showHistory) {
            return super.getIngredientUnderMouse(mouseX, mouseY);
        }

        Stream<IClickableIngredientInternal<?>> historyIngredients = historyRenderer.getSlots()
            .filter(slot -> slot.isMouseOver(mouseX, mouseY))
            .map(IngredientListSlot::getIngredientRenderer)
            .flatMap(Optional::stream)
            .map(renderer -> (IClickableIngredientInternal<?>) renderer);

        return Stream.concat(super.getIngredientUnderMouse(mouseX, mouseY), historyIngredients);
    }

    @Override
    public <T> Stream<T> getVisibleIngredients(IIngredientType<T> ingredientType) {
        if (!JeiHistoryConfig.openHistory() || !showHistory) {
            return super.getVisibleIngredients(ingredientType);
        }

        Stream<T> historyIngredients = historyRenderer.getSlots()
            .map(IngredientListSlot::getTypedIngredient)
            .flatMap(Optional::stream)
            .map(ingredient -> ingredient.getIngredient(ingredientType))
            .flatMap(Optional::stream);

        return Stream.concat(super.getVisibleIngredients(ingredientType), historyIngredients);
    }

    public <T> void addHistory(@NotNull ITypedIngredient<T> ingredient) {
        if (!JeiHistoryConfig.openHistory()) {
            return;
        }

        IIngredientType<T> type = ingredient.getType();
        IIngredientHelper<T> helper = ingredientManager.getIngredientHelper(type);
        T normalizedIngredient = helper.normalizeIngredient(ingredient.getIngredient());
        ITypedIngredient<T> normalized = ingredientManager.createTypedIngredient(type, normalizedIngredient)
            .orElse(ingredient);

        String uniqueId = helper.getUniqueId(normalized.getIngredient(), UidContext.Ingredient);
        historyList.removeIf(existing -> equal(helper, normalized, uniqueId, existing));
        historyList.add(0, normalized);

        int maxSize = historyMaxSize > 0 ? historyMaxSize : 9 * Math.max(1, JeiHistoryConfig.historyRows());
        if (historyList.size() > maxSize) {
            historyList.subList(maxSize, historyList.size()).clear();
        }
        historyRenderer.set(0, historyList);
        historyStorage.save(ingredientManager, historyList);
    }

    private static <T> boolean equal(
        IIngredientHelper<T> ingredientHelper,
        @NotNull ITypedIngredient<T> a,
        String uidA,
        @NotNull ITypedIngredient<?> b
    ) {
        if (a.getIngredient() == b.getIngredient()) {
            return true;
        }

        if (a.getIngredient() instanceof ItemStack itemStackA && b.getIngredient() instanceof ItemStack itemStackB) {
            return itemStackA.equals(itemStackB, true);
        }

        Optional<T> filteredB = b.getIngredient(a.getType());
        if (filteredB.isPresent()) {
            String uidB = ingredientHelper.getUniqueId(filteredB.get(), UidContext.Ingredient);
            return uidA.equals(uidB);
        }

        return false;
    }

    /**
     * Draws a horizontal dashed line. Copied from Roughly Enough Items (REI) by shedaniel.
     */
    private static void drawHorizontalDashedLine(
        PoseStack poseStack,
        int x1,
        int x2,
        int y,
        int color,
        boolean reverse
    ) {
        float offset = (System.currentTimeMillis() % 600) / 100.0F;
        if (!reverse) {
            offset = 6 - offset;
        }

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Matrix4f pose = poseStack.last().pose();

        for (float x = x1 - offset; x < x2; x += 7) {
            builder.vertex(pose, Mth.clamp(x + 4, x1, x2), y, 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, Mth.clamp(x, x1, x2), y, 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, Mth.clamp(x, x1, x2), y + 1, 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, Mth.clamp(x + 4, x1, x2), y + 1, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    /**
     * Draws a vertical dashed line. Copied from Roughly Enough Items (REI) by shedaniel.
     */
    private static void drawVerticalDashedLine(
        PoseStack poseStack,
        int x,
        int y1,
        int y2,
        int color,
        boolean reverse
    ) {
        float offset = (System.currentTimeMillis() % 600) / 100.0F;
        if (!reverse) {
            offset = 6 - offset;
        }

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Matrix4f pose = poseStack.last().pose();

        for (float y = y1 - offset; y < y2; y += 7) {
            builder.vertex(pose, x + 1, Mth.clamp(y, y1, y2), 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, x, Mth.clamp(y, y1, y2), 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, x, Mth.clamp(y + 4, y1, y2), 0).color(r, g, b, a).endVertex();
            builder.vertex(pose, x + 1, Mth.clamp(y + 4, y1, y2), 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }
}
