package dev.sixik.assemblytable.screens.assembly_table;

import dev.sixik.assemblytable.ATM;
import dev.sixik.assemblytable.network.AssemblyTableRecipeClickPacket;
import dev.sixik.assemblytable.utils.AssemblyRecipeState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {

    private static final ResourceLocation TEXTURE_BASE = ResourceLocation.tryBuild(ATM.MODID,"textures/gui/assembly_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 220;

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Используем уже готовые координаты левого верхнего угла GUI в 1.21.1
        int x = this.leftPos;
        int y = this.topPos;

        // Проверяем точные координаты полоски прогресса BuildCraft:
        // X: от 86 до 90 (86 + 4)
        // Y: от 36 до 106 (36 + 70)
        if (mouseX >= x + 86 && mouseX < x + 86 + 4 && mouseY >= y + 36 && mouseY < y + 36 + 70) {

            Component tooltipText = Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE");

            // В современных версиях списки компонентов рендерятся через этот метод
            guiGraphics.renderComponentTooltip(
                    this.font,
                    java.util.List.of(tooltipText),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(TEXTURE_BASE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int progressX = x + 86;
        int progressY = y + 36;
        int barHeight = 70;

        int currentPower = this.menu.getEnergy();
        int targetPower = this.menu.getMaxEnergy();

        if (targetPower > 0 && currentPower > 0) {
            float ratio = (float) currentPower / targetPower;
            int drawnHeight = (int) Math.ceil(barHeight * Math.min(ratio, 1.0f));

            guiGraphics.blit(TEXTURE_BASE,
                    progressX,
                    progressY + barHeight - drawnHeight,
                    176,
                    48 + barHeight - drawnHeight,
                    4,
                    drawnHeight
            );
        }

        int index = 0;
        for (Map.Entry<ResourceLocation, AssemblyRecipeState> entry : this.menu.blockEntity.recipeStates.entrySet()) {
            if (index >= 12) break;

            int slotRelX = 116 + (index % 3) * 18;
            int slotRelY = 36 + (index / 3) * 18;

            AssemblyRecipeState state = entry.getValue();

            if (state == AssemblyRecipeState.SAVED) {
                guiGraphics.blit(TEXTURE_BASE, x + slotRelX, y + slotRelY, 176, 0, 16, 16);
            } else if (state == AssemblyRecipeState.SAVED_ENOUGH) {
                guiGraphics.blit(TEXTURE_BASE, x + slotRelX, y + slotRelY, 176, 16, 16, 16);
            } else if (state == AssemblyRecipeState.ACTIVE) {
                guiGraphics.blit(TEXTURE_BASE, x + slotRelX, y + slotRelY, 176, 32, 16, 16);
            }
            index++;
        }

        String title = I18n.get("be.assembly_tableBlock.name");
        guiGraphics.drawString(
                this.font,
                title,
                x + (this.imageWidth - this.font.width(title)) / 2,
                y + 15,
                0x404040,
                false
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = 0;
            for (Map.Entry<ResourceLocation, AssemblyRecipeState> entry : this.menu.blockEntity.recipeStates.entrySet()) {
                if (index >= 12) break;

                int slotX = 116 + (index % 3) * 18;
                int slotY = 36 + (index / 3) * 18;

                if (this.isHovering(slotX, slotY, 18, 18, mouseX, mouseY)) {
                    AssemblyRecipeState currentState = entry.getValue();

                    AssemblyRecipeState newState = (currentState == AssemblyRecipeState.POSSIBLE)
                            ? AssemblyRecipeState.SAVED
                            : AssemblyRecipeState.POSSIBLE;

                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new AssemblyTableRecipeClickPacket(this.menu.blockEntity.getBlockPos(), entry.getKey(), newState)
                    );

                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                index++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
