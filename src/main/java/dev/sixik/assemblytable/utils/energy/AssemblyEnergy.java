package dev.sixik.assemblytable.utils.energy;

import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.energy.EnergyStorage;

public class AssemblyEnergy extends EnergyStorage {

    private int stored = 0;

    protected final AssemblyTableBlockEntity tableBlock;

    public AssemblyEnergy(AssemblyTableBlockEntity tableBlock) {
        super(0,0,0);
        this.tableBlock = tableBlock;
    }

    @Override
    public int getEnergyStored() {
        return stored;
    }

    @Override
    public int getMaxEnergyStored() {
        return tableBlock.currentActiveRecipe != null ? tableBlock.currentActiveRecipe.value().getEnergyRequired() : 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return tableBlock.supportsDirectFeInput();
    }

    public int receiveLaserEnergy(int maxReceive, boolean simulate) {
        int max = getMaxEnergyStored();
        if (max == 0) return 0;

        int energyReceived = Math.min(max - stored, maxReceive);
        if (!simulate) {
            stored += energyReceived;
            var level = tableBlock.getLevel();
            tableBlock.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(tableBlock.getBlockPos(), tableBlock.getBlockState(), tableBlock.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return energyReceived;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!tableBlock.supportsDirectFeInput()) {
            return 0;
        }
        return receiveLaserEnergy(maxReceive, simulate);
    }

    public void consumeEnergy(int amount) {
        stored = Math.max(0, stored - amount);
        tableBlock.setChanged();
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return IntTag.valueOf(stored);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof IntTag intTag) {
            stored = intTag.getAsInt();
        }
    }
}
