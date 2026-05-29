package dev.sixik.assemblytable.blockentity;

import dev.sixik.assemblytable.register.ATMRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredAssemblyTableBlockEntity extends AssemblyTableBlockEntity {

    public PoweredAssemblyTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ATMRegistry.POWERED_ASSEMBLY_TABLE_TYPE.get(), pos, blockState);
    }

    @Override
    public boolean supportsDirectFeInput() {
        return true;
    }
}
