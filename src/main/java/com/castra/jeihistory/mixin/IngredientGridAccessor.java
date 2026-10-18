package com.castra.jeihistory.mixin;

import mezz.jei.gui.overlay.IngredientGrid;
import mezz.jei.gui.overlay.IngredientGridTooltipHelper;
import mezz.jei.gui.overlay.IngredientListRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = IngredientGrid.class, remap = false)
public interface IngredientGridAccessor {
    @Accessor
    IngredientListRenderer getIngredientListRenderer();

    @Accessor
    IngredientGridTooltipHelper getTooltipHelper();
}
