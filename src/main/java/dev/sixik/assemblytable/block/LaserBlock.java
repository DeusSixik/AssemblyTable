package dev.sixik.assemblytable.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sixik.assemblytable.api.laser.LaserConfig;
import dev.sixik.assemblytable.blockentity.LaserBlockEntity;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.utils.Vec4i;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LaserBlock extends BaseEntityBlock {

    public static final MapCodec<LaserBlock> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    propertiesCodec(),
                    Codec.INT.fieldOf("target_range").forGetter(s -> s.targetRange),
                    Codec.INT.fieldOf("max_transfer_per_tickf").forGetter(s -> s.maxTransferPerTick),
                    Codec.INT.fieldOf("energy_cap").forGetter(s -> s.energyCap),
                    Vec4i.CODEC.fieldOf("beam_color").forGetter(s -> new Vec4i(s.colorRed, s.colorGreen, s.colorBlue, s.colorAlpha))
            ).apply(instance, LaserBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_UP = createLaserShape(Direction.UP);
    private static final VoxelShape SHAPE_DOWN = createLaserShape(Direction.DOWN);
    private static final VoxelShape SHAPE_NORTH = createLaserShape(Direction.NORTH);
    private static final VoxelShape SHAPE_SOUTH = createLaserShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_EAST = createLaserShape(Direction.EAST);
    private static final VoxelShape SHAPE_WEST = createLaserShape(Direction.WEST);

    public final int targetRange;
    public final int maxTransferPerTick;
    public final int energyCap;
    public final int colorRed;
    public final int colorGreen;
    public final int colorBlue;
    public final int colorAlpha;
    public final LaserConfig laserConfig;

    public LaserBlock(Properties p_49795_, int targetRange, int maxTransferPerTick, int energyCap, Vec4i beamColor) {
        this(p_49795_, LaserConfig.builder()
                .targetRange(targetRange)
                .maxTransferPerTick(maxTransferPerTick)
                .energyBuffer(energyCap)
                .beamColor(beamColor)
                .warmupDisabled()
                .build());
    }

    public LaserBlock(Properties p_49795_, LaserConfig laserConfig) {
        super(p_49795_.noOcclusion());
        if (laserConfig == null) {
            throw new IllegalArgumentException("laserConfig cannot be null");
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
        this.laserConfig = laserConfig;
        this.targetRange = laserConfig.targetRange();
        this.maxTransferPerTick = laserConfig.maxTransferPerTick();
        this.energyCap = laserConfig.energyBuffer();
        this.colorRed = laserConfig.beamColor().x();
        this.colorGreen = laserConfig.beamColor().y();
        this.colorBlue = laserConfig.beamColor().z();
        this.colorAlpha = laserConfig.beamColor().w();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_UP;
        };
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
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new LaserBlockEntity(blockPos, blockState, laserConfig);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ATMRegistry.LASER_TYPE.get(),
                ((level1, blockPos, blockState, assemblyTableBlockEntity) ->
                        assemblyTableBlockEntity.tick(level1, blockPos, blockState)
                )
        );
    }

    private static VoxelShape createLaserShape(Direction direction) {
        VoxelShape stand;
        VoxelShape body = switch (direction) {
            case DOWN -> {
                stand = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                yield Block.box(5.0D, 3.0D, 5.0D, 11.0D, 12.0D, 11.0D);
            }
            case NORTH -> {
                stand = Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
                yield Block.box(5.0D, 5.0D, 3.0D, 11.0D, 11.0D, 12.0D);
            }
            case SOUTH -> {
                stand = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
                yield Block.box(5.0D, 5.0D, 4.0D, 11.0D, 11.0D, 13.0D);
            }
            case EAST -> {
                stand = Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);
                yield Block.box(4.0D, 5.0D, 5.0D, 13.0D, 11.0D, 11.0D);
            }
            case WEST -> {
                stand = Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                yield Block.box(3.0D, 5.0D, 5.0D, 12.0D, 11.0D, 11.0D);
            }
            default -> {
                stand = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
                yield Block.box(5.0D, 4.0D, 5.0D, 11.0D, 13.0D, 11.0D);
            }
        };
        return Shapes.or(stand, body);
    }

}
