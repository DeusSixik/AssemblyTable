package dev.sixik.assemblytable.network;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import dev.sixik.assemblytable.utils.AssemblyRecipeState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyTableRecipeClickPacket(BlockPos pos, ResourceLocation recipeId, AssemblyRecipeState state) implements CustomPacketPayload {

    public static final Type<AssemblyTableRecipeClickPacket> TYPE = new Type<>(ResourceLocation.tryBuild(ATM.MODID, "recipe_click"));

    public static final StreamCodec<FriendlyByteBuf, AssemblyTableRecipeClickPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AssemblyTableRecipeClickPacket::pos,
            ResourceLocation.STREAM_CODEC, AssemblyTableRecipeClickPacket::recipeId,
            ByteBufCodecs.INT.map(i -> AssemblyRecipeState.values()[i], AssemblyRecipeState::ordinal), AssemblyTableRecipeClickPacket::state,
            AssemblyTableRecipeClickPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyTableRecipeClickPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.isLoaded(payload.pos()) && level.getBlockEntity(payload.pos()) instanceof AssemblyTableBlockEntity blockEntity) {

                if (blockEntity.recipeStates.containsKey(payload.recipeId())) {
                    blockEntity.recipeStates.put(payload.recipeId(), payload.state());

                    blockEntity.setChanged();
                    blockEntity.updateRecipeStates();
                }
            }
        });
    }
}
