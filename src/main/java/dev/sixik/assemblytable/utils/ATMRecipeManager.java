package dev.sixik.assemblytable.utils;

import dev.sixik.assemblytable.recipes.ATMRecipes;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;

public class ATMRecipeManager {

    public static ObjectArrayList<RecipeHolder<AssemblyTableRecipe>> getAllRecipesForAssemblyTable(Level level, ItemStackHandler inventory) {
        return getAllRecipesForAssemblyTable(level, new RecipeWrapper(inventory));
    }

    public static ObjectArrayList<RecipeHolder<AssemblyTableRecipe>> getAllRecipesForAssemblyTable(Level level, RecipeWrapper input) {
        List<RecipeHolder<AssemblyTableRecipe>> recipesList = level.getRecipeManager()
                .getAllRecipesFor(ATMRecipes.ASSEMBLY_TABLE_TYPE.get());

        ObjectArrayList<RecipeHolder<AssemblyTableRecipe>> outList = new ObjectArrayList<>();
        for (int i = 0; i < recipesList.size(); i++) {
            RecipeHolder<AssemblyTableRecipe> recipe = recipesList.get(i);

            if(recipe.value().matches(input, level))
                outList.add(recipe);
        }

        return outList;
    }
}
