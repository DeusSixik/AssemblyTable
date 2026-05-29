package dev.sixik.assemblytable.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.SizedIngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public interface AssemblyTableKubeRecipe {
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.key("result", ComponentRole.OUTPUT);
    RecipeKey<List<SizedIngredient>> INGREDIENTS = SizedIngredientComponent.SIZED_INGREDIENT.instance()
            .asList()
            .key("ingredients", ComponentRole.INPUT);
    RecipeKey<Integer> ENERGY = NumberComponent.NON_NEGATIVE_INT.key("energy", ComponentRole.INPUT);

    RecipeSchema SCHEMA = new RecipeSchema(RESULT, INGREDIENTS, ENERGY).uniqueId(RESULT);
}