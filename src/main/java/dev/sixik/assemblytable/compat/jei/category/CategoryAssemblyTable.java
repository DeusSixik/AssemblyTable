package dev.sixik.assemblytable.compat.jei.category;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.recipes.ATMRecipes;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.utils.SizedIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CategoryAssemblyTable implements IRecipeCategory<RecipeHolder<AssemblyTableRecipe>> {

    private static final ResourceLocation TEXTURE_BASE =
            ResourceLocation.tryBuild(ATM.MODID,"textures/gui/assembly_table.png");

    public static final RecipeType<RecipeHolder<AssemblyTableRecipe>> ASSEMBLY_TABLE_TYPE =
            RecipeType.createFromVanilla(ATMRecipes.ASSEMBLY_TABLE_TYPE.get());

    private final IDrawable background;
    private final IDrawable icon;

    public CategoryAssemblyTable(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE_BASE, 5, 34, 166, 76);
        this.icon = helper.createDrawableItemStack(ATMRegistry.ASSEMBLY_TABLE.toStack());
    }

    @Override
    public RecipeType<RecipeHolder<AssemblyTableRecipe>> getRecipeType() {
        return ASSEMBLY_TABLE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("be.assembly_tableBlock.name");
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder recipeLayout, RecipeHolder<AssemblyTableRecipe> recipe, IFocusGroup focuses) {
        List<SizedIngredient> ingList = recipe.value().getIngredientsSizable();
        for (int i = 0; i < ingList.size(); i++) {
            SizedIngredient sizedIngredient = ingList.get(i);
            ItemStack[] items = sizedIngredient.ingredient().getItems();
            IRecipeSlotBuilder slot = recipeLayout.addSlot(RecipeIngredientRole.INPUT, 3 + i % 3 * 18, 2 + i / 3 * 18);

            if(items.length == 0)
                slot.addItemStack(ItemStack.EMPTY);
            else {
                for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                    slot.addItemStack(items[itemIndex].copyWithCount(sizedIngredient.count()));
                }
            }
        }

        recipeLayout.addSlot(RecipeIngredientRole.OUTPUT, 111, 2)
                .addItemStack(recipe.value().getResult());
    }

    private static final Font FONT = Minecraft.getInstance().font;

    @Override
    public void draw(RecipeHolder<AssemblyTableRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(FONT, NumberFormat.getIntegerInstance().format(recipe.value().getEnergyRequired()) + " FE", 0, -7, 0, false);
    }
}
