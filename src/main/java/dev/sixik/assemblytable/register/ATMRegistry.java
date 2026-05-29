package dev.sixik.assemblytable.register;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.api.laser.LaserConfig;
import dev.sixik.assemblytable.block.LaserBlock;
import dev.sixik.assemblytable.block.LaserTableBlock;
import dev.sixik.assemblytable.block.PoweredAssemblyTableBlock;
import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import dev.sixik.assemblytable.blockentity.LaserBlockEntity;
import dev.sixik.assemblytable.blockentity.PoweredAssemblyTableBlockEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
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
    public static DeferredBlock<Block> POWERED_ASSEMBLY_TABLE;
    public static Supplier<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE_TYPE;
    public static Supplier<BlockEntityType<PoweredAssemblyTableBlockEntity>> POWERED_ASSEMBLY_TABLE_TYPE;

    public static DeferredBlock<Block> LASER_BASIC;
    public static DeferredBlock<Block> LASER_BASIC_WARMUP;
    public static Supplier<BlockEntityType<LaserBlockEntity>> LASER_TYPE;
    private static final List<DeferredBlock<? extends Block>> LASER_BLOCKS = new ArrayList<>();
    private static final List<DeferredHolder<Item, ? extends Item>> REDSTONE_CHIPSETS = new ArrayList<>();

    public static DeferredHolder<Item, Item> REDSTONE_CHIPSET_RED;
    public static DeferredHolder<Item, Item> REDSTONE_CHIPSET_IRON;
    public static DeferredHolder<Item, Item> REDSTONE_CHIPSET_GOLD;
    public static DeferredHolder<Item, Item> REDSTONE_CHIPSET_DIAMOND;
    public static DeferredHolder<Item, Item> REDSTONE_CHIPSET_QUARTZ;


    public static DeferredHolder<MenuType<?>, MenuType<AssemblyTableMenu>> ASSEMBLY_TABLE_MENU;

    public static void register(IEventBus modEventBus) {
        ASSEMBLY_TABLE = registerBlock("assembly_table", (properties) ->
                new LaserTableBlock(LaserTableBlock.Type.ASSEMBLY_TABLE, properties));

        ASSEMBLY_TABLE_TYPE = BLOCK_ENTITIES.register("assembly_table_be", () ->
                BlockEntityType.Builder.of(AssemblyTableBlockEntity::new,
                        ASSEMBLY_TABLE.get()).build(Util.fetchChoiceType(References.BLOCK_ENTITY, "assembly_table_be")));

        POWERED_ASSEMBLY_TABLE = registerBlock("powered_assembly_table", (properties) ->
                new PoweredAssemblyTableBlock(LaserTableBlock.Type.ASSEMBLY_TABLE, properties));

        POWERED_ASSEMBLY_TABLE_TYPE = BLOCK_ENTITIES.register("powered_assembly_table_be", () ->
                BlockEntityType.Builder.of(PoweredAssemblyTableBlockEntity::new,
                        POWERED_ASSEMBLY_TABLE.get()).build(Util.fetchChoiceType(References.BLOCK_ENTITY, "powered_assembly_table_be")));

        ASSEMBLY_TABLE_MENU = registerMenuType("assembly_table_menu", AssemblyTableMenu::new);

        LASER_BASIC = registerLaserBlock("laser_basic", (properties) ->
                new LaserBlock(properties, LaserConfig.builder()
                        .targetRange(6)
                        .energyBuffer(1000)
                        .maxSpeedTransferPerTick(1000)
                        .maxReceivePerTick(5000)
                        .beamColor(new Vec4i(30, 100, 255, 200))
                        .warmupDisabled()
                        .build())
        );

        LASER_BASIC_WARMUP = registerLaserBlock("laser_basic_warmup", (properties) ->
                new LaserBlock(properties, LaserConfig.builder()
                        .targetRange(6)
                        .energyBuffer(1000)
                        .maxSpeedTransferPerTick(1000)
                        .maxReceivePerTick(5000)
                        .rampUpTicks(60)
                        .warmupColors(
                                new Vec4i(255, 40, 20, 200),
                                new Vec4i(255, 220, 30, 200),
                                new Vec4i(30, 100, 255, 200)
                        )
                        .build())
        );

        LASER_TYPE = BLOCK_ENTITIES.register("laser_basic_be", () ->
                BlockEntityType.Builder.of(LaserBlockEntity::new,
                        LASER_BASIC.get(),
                        LASER_BASIC_WARMUP.get()).build(Util.fetchChoiceType(References.BLOCK_ENTITY, "laser_be")));

        REDSTONE_CHIPSET_RED = registerChipsetItem("redstone_chipset_red");
        REDSTONE_CHIPSET_IRON = registerChipsetItem("redstone_chipset_iron");
        REDSTONE_CHIPSET_GOLD = registerChipsetItem("redstone_chipset_gold");
        REDSTONE_CHIPSET_DIAMOND = registerChipsetItem("redstone_chipset_diamond");
        REDSTONE_CHIPSET_QUARTZ = registerChipsetItem("redstone_chipset_quartz");


        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);

        ATMRecipes.registerType(modEventBus);
    }

    public static List<DeferredBlock<? extends Block>> getLaserBlocks() {
        return List.copyOf(LASER_BLOCKS);
    }

    public static List<DeferredHolder<Item, ? extends Item>> getRedstoneChipsets() {
        return List.copyOf(REDSTONE_CHIPSETS);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function, machineBlockProperties());
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static BlockBehaviour.Properties machineBlockProperties() {
        return BlockBehaviour.Properties.of()
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops();
    }

    private static <T extends Block> DeferredBlock<T> registerLaserBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = registerBlock(name, function);
        LASER_BLOCKS.add(toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties));
    }

    private static DeferredHolder<Item, Item> registerChipsetItem(String name) {
        DeferredHolder<Item, Item> item = ITEMS.registerItem(name, Item::new);
        REDSTONE_CHIPSETS.add(item);
        return item;
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                               IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
