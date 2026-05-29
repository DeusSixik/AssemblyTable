package dev.sixik.assemblytable.utils.slots;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntFunction;

public class SlotDisplay extends Slot {
    private static SimpleContainer emptyInventory = new SimpleContainer(0);
    private final IntFunction<ItemStack> getter;

    public SlotDisplay(IntFunction<ItemStack> getter, int index, int xPosition, int yPosition) {
        super(emptyInventory, index, xPosition, yPosition);
        this.getter = getter;
    }

    @Override
    public void onTake(Player player, ItemStack stack) { }

    @Override
    public ItemStack getItem() {
        return getter.apply(getSlotIndex()).copy();
    }

    @Override
    public void set(ItemStack stack) { }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        return stack;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getItem().getMaxStackSize();
    }

    @Override
    public int getMaxStackSize() {
        return getItem().getMaxStackSize();
    }
}
