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

        // 4 специализированных слота имплантов (координаты подобраны под неоновую панель)
        // Слот 0: Спина (Нервная система)
        this.addSlot(new Slot(cyberwareInv, 0, 26, 32) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.SANDEVISTAN); }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 1: Руки (зарезервировано)
        this.addSlot(new Slot(cyberwareInv, 1, 62, 32) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 2: Ноги (зарезервировано)
        this.addSlot(new Slot(cyberwareInv, 2, 98, 32) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 3: Кожа (зарезервировано)
        this.addSlot(new Slot(cyberwareInv, 3, 134, 32) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        // Основной инвентарь игрока (3 ряда по 9 слотов)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Хотбар игрока (9 слотов)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
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
