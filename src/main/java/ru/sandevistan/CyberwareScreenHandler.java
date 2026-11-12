package ru.sandevistan;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CyberwareScreenHandler extends ScreenHandler {
    private final Inventory cyberwareInv;

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4));
    }

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory, Inventory cyberwareInv) {
        super(SandevistanMod.CYBERWARE_HANDLER, syncId);
        checkSize(cyberwareInv, 4);
        this.cyberwareInv = cyberwareInv;
        cyberwareInv.onOpen(playerInventory.player);

        // Слот 0: Спина (слева сверху)
        this.addSlot(new Slot(cyberwareInv, 0, 24, 30) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.SANDEVISTAN); }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 1: Руки (справа сверху)
        this.addSlot(new Slot(cyberwareInv, 1, 156, 30) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 2: Ноги (справа снизу)
        this.addSlot(new Slot(cyberwareInv, 2, 156, 74) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 3: Кожа (слева снизу)
        this.addSlot(new Slot(cyberwareInv, 3, 24, 74) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        // Основной инвентарь игрока (сдвинут вниз под силуэт)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 18 + col * 18, 124 + row * 18));
            }
        }
        // Хотбар игрока
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 18 + col * 18, 182));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.cyberwareInv.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 4) {
                if (!this.insertItem(originalStack, 4, 40, true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(originalStack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }
}
