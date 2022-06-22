package cofh.thermal.core.util.recipes.machine;

import cofh.lib.content.fluid.FluidIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;

import static cofh.thermal.core.ThermalCore.RECIPE_SERIALIZERS;
import static cofh.thermal.core.ThermalCore.RECIPE_TYPES;
import static cofh.thermal.core.init.TCoreRecipeTypes.ID_RECIPE_SMELTER_RECYCLE;

public class SmelterRecycleRecipe extends SmelterRecipe {

    public SmelterRecycleRecipe(ResourceLocation recipeId, int energy, float experience, List<Ingredient> inputItems, List<FluidIngredient> inputFluids, List<ItemStack> outputItems, List<Float> outputItemChances, List<FluidStack> outputFluids) {

        super(recipeId, energy, experience, inputItems, inputFluids, outputItems, outputItemChances, outputFluids);
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {

        return RECIPE_SERIALIZERS.get(ID_RECIPE_SMELTER_RECYCLE);
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {

        return RECIPE_TYPES.get(ID_RECIPE_SMELTER_RECYCLE);
    }

}
