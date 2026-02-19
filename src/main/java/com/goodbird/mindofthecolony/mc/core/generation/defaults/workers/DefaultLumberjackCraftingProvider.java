package com.goodbird.mindofthecolony.mc.core.generation.defaults.workers;

import com.goodbird.mindofthecolony.mc.api.colony.jobs.ModJobs;
import com.goodbird.mindofthecolony.mc.api.crafting.ItemStorage;
import com.goodbird.mindofthecolony.mc.api.equipment.ModEquipmentTypes;
import com.goodbird.mindofthecolony.mc.core.generation.CustomRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.goodbird.mindofthecolony.mc.api.util.constant.BuildingConstants.MODULE_CUSTOM;

/**
 * Datagen for Lumberjack
 */
public class DefaultLumberjackCraftingProvider extends CustomRecipeProvider
{
    private static final String LUMBERJACK = ModJobs.LUMBERJACK_ID.getPath();

    public DefaultLumberjackCraftingProvider(@NotNull final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(packOutput, lookupProvider);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "DefaultLumberjackCraftingProvider";
    }

    @Override
    protected void registerRecipes(@NotNull final Consumer<CustomRecipeBuilder> consumer)
    {
        // Bamboo blocks, whilst they can be stripped, do not fall under the logs tag, hence they do not appear as part of the strip_logs.json
        recipe(LUMBERJACK, MODULE_CUSTOM, "strip_bamboo_block")
          .inputs(List.of(new ItemStorage(new ItemStack(Items.BAMBOO_BLOCK))))
          .result(new ItemStack(Items.STRIPPED_BAMBOO_BLOCK))
          .requiredTool(ModEquipmentTypes.axe.get())
          .build(consumer);
    }
}
