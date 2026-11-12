package ru.sandevistan;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class CyberwareScreenHandler extends ScreenHandler {
    private final Inventory cyberwareInv;
    public static final TagKey<Item> EYE_IMPLANTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("sandevistan", "eye_implants"));

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CyberwareStorage.SLOT_COUNT));
    }

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory, Inventory cyberwareInv) {
        super(SandevistanMod.CYBERWARE_HANDLER, syncId);
        checkSize(cyberwareInv, CyberwareStorage.SLOT_COUNT);
        this.cyberwareInv = cyberwareInv;
        cyberwareInv.onOpen(playerInventory.player);

        // Слот 0: ОС / Спина (справа сверху — под Сандевистан)
        this.addSlot(new Slot(cyberwareInv, 0, 152, 32) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.SANDEVISTAN); }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 1: Глаза / Лицо (справа по центру — под оптику Кироши и глазные импланты)
        this.addSlot(new Slot(cyberwareInv, 1, 152, 64) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isIn(EYE_IMPLANTS); }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 2: Руки (слева сверху)
        this.addSlot(new Slot(cyberwareInv, 2, 48, 36) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 3: Кожа / Броня (слева снизу)
        this.addSlot(new Slot(cyberwareInv, 3, 48, 84) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Слот 4: Ноги (справа снизу)
        this.addSlot(new Slot(cyberwareInv, 4, 152, 96) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        // Инвентарь игрока (3 ряда по 9 слотов)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 28 + col * 18, 134 + row * 18));
            }
        }
        // Хотбар игрока (9 слотов)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 28 + col * 18, 192));
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
            int totalSlots = CyberwareStorage.SLOT_COUNT;
            if (invSlot < totalSlots) {
                if (!this.insertItem(originalStack, totalSlots, totalSlots + 36, true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(originalStack, 0, totalSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }
}
