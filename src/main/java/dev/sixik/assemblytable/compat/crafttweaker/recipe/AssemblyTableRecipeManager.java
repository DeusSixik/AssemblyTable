package dev.sixik.assemblytable.compat.crafttweaker.recipe;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import dev.sixik.assemblytable.recipes.ATMRecipes;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import dev.sixik.assemblytable.utils.SizedIngredient;
import net.minecraft.world.item.crafting.RecipeType;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenCodeType.Name("assemblytable.api.recipe.AssemblyTableRecipeManager")
//@Document("mods/assemblytable/recipe/manager/AssemblyTableRecipeManager")
public class AssemblyTableRecipeManager implements IRecipeManager<AssemblyTableRecipe> {

    @ZenCodeGlobals.Global("bc_assemblyTable")
    public static final AssemblyTableRecipeManager INSTANCE = new AssemblyTableRecipeManager();

    @ZenCodeType.Method
    public void addRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs, int energyRequired) {

        List<SizedIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < inputs.length; i++) {
            IIngredientWithAmount input = inputs[i];
            ingredients.add(new SizedIngredient(input.ingredient().asVanillaIngredient(), input.amount()));
        }

        AssemblyTableRecipe recipe = new AssemblyTableRecipe(output.getInternal(), ingredients, energyRequired);
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    @Override
    public RecipeType<AssemblyTableRecipe> getRecipeType() {
        return ATMRecipes.ASSEMBLY_TABLE_TYPE.get();
    }
}
