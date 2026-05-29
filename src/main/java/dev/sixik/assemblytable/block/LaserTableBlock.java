package dev.sixik.assemblytable.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import dev.sixik.assemblytable.register.ATMRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LaserTableBlock extends BaseEntityBlock {

    public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    private final Type type;

    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    public static final MapCodec<LaserTableBlock> CODEC =
            RecordCodecBuilder.mapCodec((instance) ->
                instance.group(
                    propertiesCodec(),
                    Codec.INT.fieldOf("type").forGetter(s -> s.type.ordinal())
                ).apply(instance, LaserTableBlock::new)
            );

    public LaserTableBlock(BlockBehaviour.Properties properties, int type) {
        super(properties.noOcclusion());
        this.type = Type.values()[type];
    }

    public LaserTableBlock(Type type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    protected float getShadeBrightness(BlockState p_308911_, BlockGetter p_308952_, BlockPos p_308918_) {
        return 1.0F;
    }

    protected boolean propagatesSkylightDown(BlockState p_309084_, BlockGetter p_309133_, BlockPos p_309097_) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult hitResult) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof MenuProvider provider) {
                pPlayer.openMenu(new SimpleMenuProvider(provider, Component.literal("Growth Chamber")), pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        switch (type) {
            case ASSEMBLY_TABLE:
                return new AssemblyTableBlockEntity(blockPos, blockState);
        }

        return null;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof DropContainer dropContainer) {
                dropContainer.drops();
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }


    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide())
            return null;

        return createTickerHelper(blockEntityType, ATMRegistry.ASSEMBLY_TABLE_TYPE.get(),
                ((level1, blockPos, blockState, assemblyTableBlockEntity) ->
                        assemblyTableBlockEntity.tick(level1, blockPos, blockState)
                )
        );
    }

    public enum Type {
        ASSEMBLY_TABLE,
        ADVANCED_CRAFTING_TABLE,
        INTEGRATION_TABLE,
        CHARGING_TABLE
    }

    public interface DropContainer {

        void drops();
    }
}
