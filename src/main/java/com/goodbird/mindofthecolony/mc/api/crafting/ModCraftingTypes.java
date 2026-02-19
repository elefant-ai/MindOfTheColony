package com.goodbird.mindofthecolony.mc.api.crafting;

import com.goodbird.mindofthecolony.mc.api.crafting.registry.CraftingType;
import com.goodbird.mindofthecolony.mc.core.recipes.ArchitectsCutterCraftingType;
import com.goodbird.mindofthecolony.mc.core.recipes.BrewingCraftingType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.MOD_ID;

public class ModCraftingTypes
{
    public static final ResourceLocation SMALL_CRAFTING_ID = new ResourceLocation(MOD_ID, "smallcrafting");
    public static final ResourceLocation LARGE_CRAFTING_ID = new ResourceLocation(MOD_ID, "largecrafting");
    public static final ResourceLocation SMELTING_ID = new ResourceLocation(MOD_ID, "smelting");
    public static final ResourceLocation BREWING_ID = new ResourceLocation(MOD_ID, "brewing");
    public static final ResourceLocation ARCHITECTS_CUTTER_ID = new ResourceLocation("domum_ornamentum", "architects_cutter");

    public static DeferredHolder<CraftingType, RecipeCraftingType<CraftingInput, CraftingRecipe>>     SMALL_CRAFTING;
    public static DeferredHolder<CraftingType, RecipeCraftingType<CraftingInput, CraftingRecipe>>     LARGE_CRAFTING;
    public static DeferredHolder<CraftingType, RecipeCraftingType<SingleRecipeInput, SmeltingRecipe>> SMELTING;
    public static DeferredHolder<CraftingType, BrewingCraftingType>                                   BREWING;
    public static DeferredHolder<CraftingType, ArchitectsCutterCraftingType> ARCHITECTS_CUTTER;

    private ModCraftingTypes()
    {
        throw new IllegalStateException("Tried to initialize: ModCraftingTypes but this is a Utility class.");
    }
}
