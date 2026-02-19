package com.goodbird.mindofthecolony.mc.apiimp.initializer;

import com.goodbird.mindofthecolony.mc.api.crafting.ModCraftingTypes;
import com.goodbird.mindofthecolony.mc.api.crafting.RecipeCraftingType;
import com.goodbird.mindofthecolony.mc.api.crafting.registry.CraftingType;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.apiimp.CommonMinecoloniesAPIImpl;
import com.goodbird.mindofthecolony.mc.core.recipes.ArchitectsCutterCraftingType;
import com.goodbird.mindofthecolony.mc.core.recipes.BrewingCraftingType;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCraftingTypesInitializer
{
    public final static DeferredRegister<CraftingType>
       DEFERRED_REGISTER = DeferredRegister.create(CommonMinecoloniesAPIImpl.CRAFTING_TYPES, Constants.MOD_ID);

    private ModCraftingTypesInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModCraftingTypesInitializer but this is a Utility class.");
    }

    static
    {
            ModCraftingTypes.SMALL_CRAFTING = DEFERRED_REGISTER.register(ModCraftingTypes.SMALL_CRAFTING_ID.getPath(), () -> new RecipeCraftingType<>(ModCraftingTypes.SMALL_CRAFTING_ID,
              RecipeType.CRAFTING, r -> r.value().canCraftInDimensions(2, 2)));

            ModCraftingTypes.LARGE_CRAFTING = DEFERRED_REGISTER.register(ModCraftingTypes.LARGE_CRAFTING_ID.getPath(), () -> new RecipeCraftingType<>(ModCraftingTypes.LARGE_CRAFTING_ID,
              RecipeType.CRAFTING, r -> r.value().canCraftInDimensions(3, 3)
                                          && !r.value().canCraftInDimensions(2, 2)));

            ModCraftingTypes.SMELTING = DEFERRED_REGISTER.register(ModCraftingTypes.SMELTING_ID.getPath(), () -> new RecipeCraftingType<>(ModCraftingTypes.SMELTING_ID,
              RecipeType.SMELTING, null));

            ModCraftingTypes.BREWING = DEFERRED_REGISTER.register(ModCraftingTypes.BREWING_ID.getPath(), BrewingCraftingType::new);

            ModCraftingTypes.ARCHITECTS_CUTTER = DEFERRED_REGISTER.register(ModCraftingTypes.ARCHITECTS_CUTTER_ID.getPath(), () -> new ArchitectsCutterCraftingType());
    }
}
