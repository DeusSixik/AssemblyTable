package dev.sixik.assemblytable.recipes;

import dev.sixik.assemblytable.ATM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ATMRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ATM.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, ATM.MODID);

    public static final Supplier<RecipeType<AssemblyTableRecipe>> ASSEMBLY_TABLE_TYPE =
            TYPES.register("assembly_table", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "assembly_table";
                }
            });

    public static final Supplier<RecipeSerializer<AssemblyTableRecipe>> ASSEMBLY_TABLE_SERIALIZER =
            SERIALIZERS.register("assembly_table", AssemblyTableRecipe.Serializer::new);

    public static void registerType(IEventBus modEventBus) {

        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }

}
