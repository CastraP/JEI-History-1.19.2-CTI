package com.castra.jeihistory.gui.input.handler;

import com.castra.jeihistory.JeiHistoryConfig;
import com.castra.jeihistory.gui.history.HistoryIngredientGrid;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.FocusInputHandler;
import mezz.jei.gui.input.handlers.LimitedAreaInputHandler;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Based on JEI's {@link FocusInputHandler}. Adds the hovered ingredient to the recipe history
 * right before the recipe/uses lookup is opened.
 *
 * Ported from JEI Recipe History by Christofmeg (MIT).
 */
public class ExtendedFocusInputHandler extends FocusInputHandler {
    private static final List<RecipeIngredientRole> SHOW_RECIPE_ROLES = List.of(RecipeIngredientRole.OUTPUT);
    private static final List<RecipeIngredientRole> SHOW_USES_ROLES = List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST);

    private final CombinedRecipeFocusSource focusSource;
    private final IRecipesGui recipesGui;
    private final IFocusFactory focusFactory;

    public ExtendedFocusInputHandler(
        CombinedRecipeFocusSource focusSource,
        IRecipesGui recipesGui,
        IFocusFactory focusFactory
    ) {
        super(focusSource, recipesGui, focusFactory);
        this.focusSource = focusSource;
        this.recipesGui = recipesGui;
        this.focusFactory = focusFactory;
    }

    @Override
    public @NotNull Optional<IUserInputHandler> handleUserInput(
        @NotNull Screen screen,
        @NotNull UserInput input,
        @NotNull IInternalKeyMappings keyBindings
    ) {
        if (input.is(keyBindings.getShowRecipe())) {
            return handleShow(input, SHOW_RECIPE_ROLES, keyBindings);
        }
        if (input.is(keyBindings.getShowUses())) {
            return handleShow(input, SHOW_USES_ROLES, keyBindings);
        }
        return Optional.empty();
    }

    private Optional<IUserInputHandler> handleShow(
        UserInput input,
        List<RecipeIngredientRole> roles,
        IInternalKeyMappings keyBindings
    ) {
        return focusSource.getIngredientUnderMouse(input, keyBindings)
            .findFirst()
            .map(clicked -> {
                if (!input.isSimulate()) {
                    if (JeiHistoryConfig.openHistory() && HistoryIngredientGrid.instance != null) {
                        HistoryIngredientGrid.instance.addHistory(clicked.getTypedIngredient());
                    }

                    List<IFocus<?>> focuses = roles.stream()
                        .<IFocus<?>>map(role -> focusFactory.createFocus(role, clicked.getTypedIngredient()))
                        .toList();
                    recipesGui.show(focuses);
                }

                return LimitedAreaInputHandler.create(this, clicked.getArea());
            });
    }
}
