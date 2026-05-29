package dev.sixik.assemblytable.compat.jei;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.compat.jei.category.CategoryAssemblyTable;
import dev.sixik.assemblytable.recipes.ATMRecipes;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.screens.assembly_table.AssemblyTableScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JEIATMPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.tryBuild(ATM.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CategoryAssemblyTable(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<AssemblyTableRecipe> polishingRecipes =
                recipeManager.getAllRecipesFor(ATMRecipes.ASSEMBLY_TABLE_TYPE.get())
                        .stream().map(RecipeHolder::value).toList();

        registration.addRecipes(CategoryAssemblyTable.ASSEMBLY_TABLE_TYPE, polishingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CategoryAssemblyTable.ASSEMBLY_TABLE_TYPE,
                ATMRegistry.ASSEMBLY_TABLE.get(), ATMRegistry.LASER_BASIC.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(AssemblyTableScreen.class, 60, 30, 20, 30,
                CategoryAssemblyTable.ASSEMBLY_TABLE_TYPE);
    }
}
