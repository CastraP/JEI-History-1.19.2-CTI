package com.castra.jeihistory.mixin;

import com.castra.jeihistory.gui.input.handler.ExtendedFocusInputHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.handlers.FocusInputHandler;
import mezz.jei.gui.startup.JeiGuiStarter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = JeiGuiStarter.class, remap = false)
public class JeiGuiStarterMixin {
    @Redirect(
        method = "start",
        at = @At(
            value = "NEW",
            target = "mezz/jei/gui/input/handlers/FocusInputHandler"
        )
    )
    private static FocusInputHandler jeihistory$createFocusInputHandler(
        CombinedRecipeFocusSource focusSource,
        IRecipesGui recipesGui,
        IFocusFactory focusFactory
    ) {
        return new ExtendedFocusInputHandler(focusSource, recipesGui, focusFactory);
    }
}
