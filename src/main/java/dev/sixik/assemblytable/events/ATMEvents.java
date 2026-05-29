package dev.sixik.assemblytable.events;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.network.AssemblyTableRecipeClickPacket;
import dev.sixik.assemblytable.register.ATMRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ATM.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ATMEvents {

    @SubscribeEvent
    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        /*
        registrar.playBidirectional(
                MyData.TYPE,
                MyData.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleDataOnMain,
                        ServerPayloadHandler::handleDataOnMain
                )
        );
        */

        event.registrar(ATM.MODID).playToServer(
                AssemblyTableRecipeClickPacket.TYPE,
                AssemblyTableRecipeClickPacket.STREAM_CODEC,
                AssemblyTableRecipeClickPacket::handle
        );
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ATMRegistry.ASSEMBLY_TABLE_TYPE.get(),
                (blockEntity, side) -> {
                    return blockEntity.inventory;
                }
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ATMRegistry.POWERED_ASSEMBLY_TABLE_TYPE.get(),
                (blockEntity, side) -> {
                    return blockEntity.inventory;
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ATMRegistry.ASSEMBLY_TABLE_TYPE.get(),
                (blockEntity, side) -> {
                    return blockEntity.energy;
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ATMRegistry.POWERED_ASSEMBLY_TABLE_TYPE.get(),
                (blockEntity, side) -> {
                    return blockEntity.energy;
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ATMRegistry.LASER_TYPE.get(),
                (blockEntity, side) -> blockEntity.energy
        );
    }
}
