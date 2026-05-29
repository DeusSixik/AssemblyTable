package dev.sixik.assemblytable.block;

import com.mojang.serialization.MapCodec;
import dev.sixik.assemblytable.blockentity.PoweredAssemblyTableBlockEntity;
import dev.sixik.assemblytable.register.ATMRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PoweredAssemblyTableBlock extends LaserTableBlock {

    public PoweredAssemblyTableBlock(Type type, Properties properties) {
        super(type, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PoweredAssemblyTableBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ATMRegistry.POWERED_ASSEMBLY_TABLE_TYPE.get(),
                (lvl, pos, st, blockEntity) -> blockEntity.tick(lvl, pos, st));
    }
}
