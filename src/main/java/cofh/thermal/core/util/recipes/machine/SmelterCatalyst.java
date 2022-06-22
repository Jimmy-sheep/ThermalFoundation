package cofh.thermal.core.util.recipes.machine;

import cofh.thermal.lib.util.recipes.ThermalCatalyst;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nonnull;

import static cofh.thermal.core.ThermalCore.RECIPE_SERIALIZERS;
import static cofh.thermal.core.ThermalCore.RECIPE_TYPES;
import static cofh.thermal.core.init.TCoreRecipeTypes.ID_CATALYST_SMELTER;

public class SmelterCatalyst extends ThermalCatalyst {

    public SmelterCatalyst(ResourceLocation recipeId, Ingredient ingredient, float primaryMod, float secondaryMod, float energyMod, float minChance, float useChance) {

        super(recipeId, ingredient, primaryMod, secondaryMod, energyMod, minChance, useChance);
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {

        return RECIPE_SERIALIZERS.get(ID_CATALYST_SMELTER);
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {

        return RECIPE_TYPES.get(ID_CATALYST_SMELTER);
    }

}
