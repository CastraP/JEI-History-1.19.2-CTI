package com.castra.jeihistory.storage;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.util.ServerConfigPathUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persists the recipe lookup history for the current world in {@code history.ini}.
 *
 * <p>Uses the same per-world directory layout and entry format as JEI's bookmarks:
 * {@code config/jei/world/local/<levelId>/} for singleplayer and
 * {@code config/jei/world/server/<name>_<ipHash>/} for multiplayer.
 * Each line is either {@code T:<item stack SNBT>} or {@code O:<ingredient unique id>}.
 *
 * <p>Based on JEI's {@code BookmarkConfig} by mezz (MIT).
 */
public final class HistoryStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MARKER_ITEM_STACK = "T:";
    private static final String MARKER_OTHER = "O:";

    @Nullable
    private final Path path;

    private HistoryStorage(@Nullable Path path) {
        this.path = path;
    }

    public static HistoryStorage create() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("jei");
        Path path = ServerConfigPathUtil.getWorldPath(configDir)
            .map(worldDir -> worldDir.resolve("history.ini"))
            .orElse(null);
        return new HistoryStorage(path);
    }

    public void load(IIngredientManager ingredientManager, List<ITypedIngredient<?>> target) {
        if (path == null || !Files.exists(path)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            LOGGER.error("Failed to load JEI history from file {}", path, e);
            return;
        }

        Collection<IIngredientType<?>> otherIngredientTypes = ingredientManager.getRegisteredIngredientTypes()
            .stream()
            .filter(type -> !type.equals(VanillaTypes.ITEM_STACK))
            .toList();
        IIngredientHelper<ItemStack> itemStackHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);

        for (String line : lines) {
            try {
                loadLine(ingredientManager, otherIngredientTypes, itemStackHelper, line, target);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to load JEI history entry from file {}: {}", path, line, e);
            }
        }
    }

    private static void loadLine(
        IIngredientManager ingredientManager,
        Collection<IIngredientType<?>> otherIngredientTypes,
        IIngredientHelper<ItemStack> itemStackHelper,
        String line,
        List<ITypedIngredient<?>> target
    ) {
        if (line.startsWith(MARKER_ITEM_STACK)) {
            String itemStackAsSnbt = line.substring(MARKER_ITEM_STACK.length());
            CompoundTag itemStackAsNbt;
            try {
                itemStackAsNbt = TagParser.parseTag(itemStackAsSnbt);
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Failed to parse JEI history item stack: {}", itemStackAsSnbt);
                return;
            }

            ItemStack itemStack = ItemStack.of(itemStackAsNbt);
            if (itemStack.isEmpty()) {
                LOGGER.warn("Failed to load JEI history item stack, the item no longer exists: {}", itemStackAsSnbt);
                return;
            }

            ItemStack normalized = itemStackHelper.normalizeIngredient(itemStack);
            ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, normalized)
                .ifPresentOrElse(
                    target::add,
                    () -> LOGGER.warn("Failed to load JEI history item stack: {}", itemStackAsSnbt)
                );
        } else if (line.startsWith(MARKER_OTHER)) {
            String uid = line.substring(MARKER_OTHER.length());
            findNormalizedIngredientByUid(ingredientManager, otherIngredientTypes, uid)
                .ifPresentOrElse(
                    target::add,
                    () -> LOGGER.warn("Failed to load JEI history ingredient with uid: {}", uid)
                );
        } else if (!line.isBlank()) {
            LOGGER.warn("Failed to load unknown JEI history entry: {}", line);
        }
    }

    public void save(IIngredientManager ingredientManager, List<ITypedIngredient<?>> ingredientList) {
        if (path == null) {
            return;
        }

        List<String> lines = new ArrayList<>(ingredientList.size());
        for (ITypedIngredient<?> typedIngredient : ingredientList) {
            try {
                if (typedIngredient.getIngredient() instanceof ItemStack itemStack) {
                    lines.add(MARKER_ITEM_STACK + itemStack.save(new CompoundTag()));
                } else {
                    lines.add(MARKER_OTHER + getUniqueId(ingredientManager, typedIngredient));
                }
            } catch (RuntimeException e) {
                LOGGER.error("Failed to save JEI history entry: {}", typedIngredient, e);
            }
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines);
        } catch (IOException e) {
            LOGGER.error("Failed to save JEI history to file {}", path, e);
        }
    }

    private static <T> String getUniqueId(IIngredientManager ingredientManager, ITypedIngredient<T> typedIngredient) {
        IIngredientHelper<T> ingredientHelper = ingredientManager.getIngredientHelper(typedIngredient.getType());
        return ingredientHelper.getUniqueId(typedIngredient.getIngredient(), UidContext.Ingredient);
    }

    private static Optional<ITypedIngredient<?>> findNormalizedIngredientByUid(
        IIngredientManager ingredientManager,
        Collection<IIngredientType<?>> ingredientTypes,
        String uid
    ) {
        return ingredientTypes.stream()
            .map(type -> findNormalizedIngredientByUid(ingredientManager, type, uid))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private static <T> Optional<ITypedIngredient<?>> findNormalizedIngredientByUid(
        IIngredientManager ingredientManager,
        IIngredientType<T> ingredientType,
        String uid
    ) {
        return ingredientManager.getIngredientByUid(ingredientType, uid)
            .map(ingredient -> {
                IIngredientHelper<T> ingredientHelper = ingredientManager.getIngredientHelper(ingredientType);
                return ingredientHelper.normalizeIngredient(ingredient);
            })
            .flatMap(ingredient -> ingredientManager.createTypedIngredient(ingredientType, ingredient));
    }
}
