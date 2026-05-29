package dev.sixik.assemblytable.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.sixik.assemblytable.ATM;
import net.minecraft.resources.ResourceLocation;

public class ATMKubeJs implements KubeJSPlugin {
    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.register(ResourceLocation.fromNamespaceAndPath(ATM.MODID, "assembly_table"), AssemblyTableKubeRecipe.SCHEMA);
    }
}