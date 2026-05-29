package dev.sixik.assemblytable.register;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.block.LaserBlock;
import dev.sixik.assemblytable.block.LaserTableBlock;
import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import dev.sixik.assemblytable.blockentity.LaserBlockEntity;
import dev.sixik.assemblytable.recipes.ATMRecipes;
import dev.sixik.assemblytable.screens.assembly_table.AssemblyTableMenu;
import dev.sixik.assemblytable.utils.Vec4i;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ATMRegistry {

    public static final DeferredRegister.Items ITEMS    = DeferredRegister.createItems(ATM.MODID);
    public static final DeferredRegister.Blocks BLOCKS  = DeferredRegister.createBlocks(ATM.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ATM.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ATM.MODID);

    public static DeferredBlock<Block> ASSEMBLY_TABLE;
    public static Supplier<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE_TYPE;

    public static DeferredBlock<Block> LASER_BASIC;
    public static Supplier<BlockEntityType<LaserBlockEntity>> LASER_TYPE;


    public static DeferredHolder<MenuType<?>, MenuType<AssemblyTableMenu>> ASSEMBLY_TABLE_MENU;

    public static void register(IEventBus modEventBus) {
        ASSEMBLY_TABLE = registerBlock("assembly_table", (properties) ->
                new LaserTableBlock(LaserTableBlock.Type.ASSEMBLY_TABLE, properties));

        ASSEMBLY_TABLE_TYPE = BLOCK_ENTITIES.register("assembly_table_be", () ->
                BlockEntityType.Builder.of(AssemblyTableBlockEntity::new,
                        ASSEMBLY_TABLE.get()).build(Util.fetchChoiceType(References.BLOCK_ENTITY, "assembly_table_be")));

        ASSEMBLY_TABLE_MENU = registerMenuType("assembly_table_menu", AssemblyTableMenu::new);

        LASER_BASIC = registerBlock("laser_basic", (properties) ->
                new LaserBlock(properties, 6, 1000, 1000, new Vec4i(30, 100, 255, 200))
        );
        LASER_TYPE = BLOCK_ENTITIES.register("laser_basic_be", () ->
                BlockEntityType.Builder.of(LaserBlockEntity::new,
                        LASER_BASIC.get()).build(Util.fetchChoiceType(References.BLOCK_ENTITY, "laser_be")));


        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);

        ATMRecipes.registerType(modEventBus);
    }




    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                               IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
