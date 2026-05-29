package dev.sixik.assemblytable.utils.slots;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotBase extends Slot {

    public SlotBase(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container.canPlaceItem(getContainerSlot(), stack);
    }
}
