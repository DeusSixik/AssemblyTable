package dev.sixik.assemblytable;

import com.mojang.logging.LogUtils;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.render.LaserBlockEntityRenderer;
import dev.sixik.assemblytable.screens.assembly_table.AssemblyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ATM.MODID)
public class ATM {
    public static final String MODID = "assemblytable";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.assemblytable.main"))
                    .icon(() -> new ItemStack(ATMRegistry.ASSEMBLY_TABLE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ATMRegistry.ASSEMBLY_TABLE.get());
                        output.accept(ATMRegistry.POWERED_ASSEMBLY_TABLE.get());

                        for (var laserBlock : ATMRegistry.getLaserBlocks()) {
                            output.accept(laserBlock.get());
                        }

                        for (var chipset : ATMRegistry.getRedstoneChipsets()) {
                            output.accept(chipset.get());
                        }
                    })
                    .build());

    public ATM(IEventBus modEventBus, ModContainer modContainer) {
        ATMRegistry.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ATMRegistry.LASER_TYPE.get(), context -> new LaserBlockEntityRenderer());
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ATMRegistry.ASSEMBLY_TABLE_MENU.get(), AssemblyTableScreen::new);
        }
    }
}
