package com.castra.jeihistory.mixin;

import com.castra.jeihistory.gui.history.HistoryIngredientGrid;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.runtime.IEditModeConfig;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IScreenHelper;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.core.config.IWorldConfig;
import mezz.jei.gui.config.IClientConfig;
import mezz.jei.gui.config.IIngredientFilterConfig;
import mezz.jei.gui.config.IIngredientGridConfig;
import mezz.jei.gui.filter.IFilterTextSource;
import mezz.jei.gui.overlay.IIngredientGridSource;
import mezz.jei.gui.overlay.IngredientGridWithNavigation;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.gui.startup.OverlayHelper;
import mezz.jei.gui.util.CheatUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OverlayHelper.class, remap = false)
public class OverlayHelperMixin {
    @Inject(method = "createIngredientListOverlay", at = @At("HEAD"), cancellable = true)
    private static void jeihistory$createIngredientListOverlay(
        IIngredientManager ingredientManager,
        IScreenHelper screenHelper,
        IIngredientGridSource ingredientFilter,
        IFilterTextSource filterTextSource,
        IModIdHelper modIdHelper,
        IInternalKeyMappings keyMappings,
        IIngredientGridConfig ingredientGridConfig,
        IClientConfig clientConfig,
        IWorldConfig worldConfig,
        IEditModeConfig editModeConfig,
        IConnectionToServer serverConnection,
        IIngredientFilterConfig ingredientFilterConfig,
        Textures textures,
        IColorHelper colorHelper,
        CheatUtil cheatUtil,
        CallbackInfoReturnable<IngredientListOverlay> cir
    ) {
        HistoryIngredientGrid grid = new HistoryIngredientGrid(
            ingredientManager,
            ingredientGridConfig,
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
        HistoryIngredientGrid.instance = grid;

        IngredientGridWithNavigation navigation = new IngredientGridWithNavigation(
            ingredientFilter,
            grid,
            worldConfig,
            clientConfig,
            serverConnection,
            ingredientGridConfig,
            textures.getIngredientListBackground(),
            textures.getIngredientListSlotBackground(),
            textures,
            cheatUtil,
            screenHelper,
            ingredientManager
        );

        cir.setReturnValue(new IngredientListOverlay(
            ingredientFilter,
            filterTextSource,
            screenHelper,
            navigation,
            clientConfig,
            worldConfig,
            serverConnection,
            textures,
            keyMappings,
            cheatUtil
        ));
        cir.cancel();
    }
}
