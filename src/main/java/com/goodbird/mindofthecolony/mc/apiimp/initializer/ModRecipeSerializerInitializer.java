package com.goodbird.mindofthecolony.mc.apiimp.initializer;

import com.goodbird.mindofthecolony.mc.api.crafting.ZeroWasteRecipe;
import com.goodbird.mindofthecolony.mc.api.crafting.CompostRecipe;
import com.goodbird.mindofthecolony.mc.api.crafting.registry.ModRecipeSerializer;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializerInitializer
{
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);
    public static final DeferredRegister<RecipeType<?>>       RECIPE_TYPES      = DeferredRegister.create(Registries.RECIPE_TYPE, Constants.MOD_ID);

    static
    {
        ModRecipeSerializer.CompostRecipeSerializer = RECIPE_SERIALIZER.register("composting", CompostRecipe.Serializer::new);
        ModRecipeSerializer.CompostRecipeType = RECIPE_TYPES.register("composting", () -> RecipeType.<CompostRecipe>simple(new ResourceLocation(Constants.MOD_ID, "composting")));

        ModRecipeSerializer.ZeroWasteRecipeSerializer = RECIPE_SERIALIZER.register("zero_waste", ZeroWasteRecipe.Serializer::new);
    }

    private ModRecipeSerializerInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModRecipeSerializerInitializer but this is a Utility class.");
    }
}
