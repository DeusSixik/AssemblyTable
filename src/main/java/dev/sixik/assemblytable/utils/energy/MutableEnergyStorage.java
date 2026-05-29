package dev.sixik.assemblytable.utils.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

public class MutableEnergyStorage extends EnergyStorage {

    public MutableEnergyStorage(int capacity) {
        super(capacity);
    }

    public MutableEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public MutableEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public MutableEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("capacity", capacity);
        nbt.putInt("energy", this.getEnergyStored());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if(nbt instanceof CompoundTag tag) {
            this.capacity = tag.getInt("capacity");
            this.energy = tag.getInt("energy");
        }
    }
}
