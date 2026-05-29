package dev.sixik.assemblytable.recipes;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sixik.assemblytable.utils.SizedIngredient;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.openzen.zencode.java.ZenCodeType;

import java.util.List;

@ZenRegister
@ZenCodeType.Name("assemblytable.api.recipe.AssemblyTableRecipe")
//@Document("mods/assemblytable/recipe/AssemblyTableRecipe")
public class AssemblyTableRecipe implements Recipe<RecipeWrapper> {

    private final ItemStack result;
    private final List<SizedIngredient> ingredients;
    private final int energyRequired;

    public AssemblyTableRecipe(ItemStack result, List<SizedIngredient> ingredients, int energyRequired) {
        this.result = result;
        this.ingredients = ingredients;
        this.energyRequired = energyRequired;
    }

    @ZenCodeType.Method
    public List<SizedIngredient> getIngredientsSizable() {
        return ingredients;
    }

    @ZenCodeType.Method
    public int getEnergyRequired() {
        return energyRequired;
    }

    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        int[] remainingCounts = new int[ingredients.size()];
        for (int i = 0; i < ingredients.size(); i++) {
            remainingCounts[i] = ingredients.get(i).count();
        }

        for (int i = 0; i < input.size(); i++) {
            ItemStack stackInSlot = input.getItem(i);
            if (stackInSlot.isEmpty()) continue;

            int stackCount = stackInSlot.getCount();

            for (int j = 0; j < ingredients.size(); j++) {
                if (remainingCounts[j] > 0 && ingredients.get(j).ingredient().test(stackInSlot)) {
                    int taken = Math.min(stackCount, remainingCounts[j]);

                    remainingCounts[j] -= taken;
                    stackCount -= taken;

                    if (stackCount == 0) break;
                }
            }
        }

        for (int i = 0; i < remainingCounts.length; i++) {
            if (remainingCounts[i] > 0) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(RecipeWrapper input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ATMRecipes.ASSEMBLY_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ATMRecipes.ASSEMBLY_TABLE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AssemblyTableRecipe> {

        private static final MapCodec<AssemblyTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                SizedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
                com.mojang.serialization.Codec.INT.fieldOf("energy").forGetter(r -> r.energyRequired)
        ).apply(inst, AssemblyTableRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyTableRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork
        );

        @Override
        public MapCodec<AssemblyTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblyTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static AssemblyTableRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int size = buffer.readVarInt();

            java.util.List<SizedIngredient> ingredients = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ingredients.add(SizedIngredient.STREAM_CODEC.decode(buffer));
            }

            int energy = buffer.readInt();
            return new AssemblyTableRecipe(result, ingredients, energy);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, AssemblyTableRecipe recipe) {
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.ingredients.size());

            for (SizedIngredient ingredient : recipe.ingredients) {
                SizedIngredient.STREAM_CODEC.encode(buffer, ingredient);
            }

            buffer.writeInt(recipe.energyRequired);
        }
    }
}
