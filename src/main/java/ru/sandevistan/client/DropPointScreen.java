package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import ru.sandevistan.DropPointManager;
import ru.sandevistan.SellItemPayload;

import java.util.ArrayList;
import java.util.List;

public class DropPointScreen extends Screen {
    private static final String[] CATEGORIES = {"all", "weapons", "cyberware", "resources"};

    private String selectedCategory = "all";
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public DropPointScreen() {
        super(Text.translatable("droppoint.sandevistan.title"));
        if (DropPointManager.ENTRIES.isEmpty()) {
            DropPointManager.init();
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    private List<DropPointManager.SellEntry> getFilteredEntries() {
        if ("all".equals(this.selectedCategory)) return DropPointManager.ENTRIES;
        List<DropPointManager.SellEntry> filtered = new ArrayList<>();
        for (DropPointManager.SellEntry e : DropPointManager.ENTRIES) {
            if (e.category().equals(this.selectedCategory)) filtered.add(e);
        }
        return filtered;
    }

    private int countInInventory(Item item) {
        if (this.client == null || this.client.player == null) return 0;
        int total = 0;
        for (int i = 0; i != this.client.player.getInventory().size(); i++) {
            ItemStack st = this.client.player.getInventory().getStack(i);
            if (st.isOf(item)) total += st.getCount();
        }
        return total;
    }

    private void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }

    private boolean isHovered(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<DropPointManager.SellEntry> list = getFilteredEntries();
        int maxScroll = Math.max(0, list.size() - 5);
        if (verticalAmount > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            return true;
        } else if (verticalAmount != 0 && 0 - verticalAmount > 0 && this.scrollOffset != maxScroll) {
            this.scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int w = 264;
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Верхние вкладки категорий
        int tabW = 60;
        for (int i = 0; i != CATEGORIES.length; i++) {
            int tx = x + 8 + i * (tabW + 3);
            if (isHovered(mouseX, mouseY, tx, y + 20, tabW, 16)) {
                this.selectedCategory = CATEGORIES[i];
                this.selectedIndex = 0;
                this.scrollOffset = 0;
                playClick();
                return true;
            }
        }

        // Строки предметов в списке
        List<DropPointManager.SellEntry> list = getFilteredEntries();
        int listTop = y + 42;
        int visibleRows = Math.min(5, list.size() - this.scrollOffset);
        for (int r = 0; r != visibleRows; r++) {
            int rowY = listTop + r * 26;
            if (isHovered(mouseX, mouseY, x + 8, rowY, w - 16, 24)) {
                this.selectedIndex = this.scrollOffset + r;
                playClick();
                return true;
            }
        }

        // Нижние кнопки «ПРОДАТЬ 1» и «ПРОДАТЬ ВСЁ»
        if (!list.isEmpty() && this.selectedIndex >= 0 && this.selectedIndex != list.size()) {
            DropPointManager.SellEntry sel = list.get(this.selectedIndex);
            int owned = countInInventory(sel.item());
            if (owned >= sel.batchSize()) {
                if (isHovered(mouseX, mouseY, x + w - 156, y + h - 24, 72, 18)) {
                    ClientPlayNetworking.send(new SellItemPayload(sel.id(), false));
                    playClick();
                    return true;
                }
                if (isHovered(mouseX, mouseY, x + w - 80, y + h - 24, 72, 18)) {
                    ClientPlayNetworking.send(new SellItemPayload(sel.id(), true));
                    playClick();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void drawCyberButton(DrawContext ctx, int bx, int by, int bw, int bh, Text label, boolean active, boolean hovered, boolean enabled) {
        int bg = !enabled ? 0xFF14070B : (active ? 0xFF3A101E : (hovered ? 0xFF2B0E18 : 0xFF19080E));
        int border = !enabled ? 0xFF3B1420 : (active ? 0xFF00F0FF : (hovered ? 0xFFFF2A55 : 0xFF6B1E34));
        int textColor = !enabled ? 0x5C2A39 : (active ? 0x00F0FF : (hovered ? 0xFFFFFF : 0xFF4D70));

        ctx.fill(bx, by, bx + bw, by + bh, bg);
        ctx.fill(bx, by, bx + bw, by + 1, border);
        ctx.fill(bx, by + bh - 1, bx + bw, by + bh, border);
        ctx.fill(bx, by, bx + 1, by + bh, border);
        ctx.fill(bx + bw - 1, by, bx + bw, by + bh, border);
        if (active && enabled) {
            ctx.fill(bx, by + bh - 2, bx + bw, by + bh, 0xFF00F0FF);
        }

        int tw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, bx + (bw - tw) / 2, by + (bh - 8) / 2, textColor, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int w = 264;
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Основная рамка и шапка один в один как у рипердока
        ctx.fill(x, y, x + w, y + h, 0xF512070B);
        ctx.fill(x, y, x + w, y + 1, 0xFFFF2A55);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFF2A55);
        ctx.fill(x, y, x + 1, y + h, 0xFFFF2A55);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFF2A55);

        ctx.fill(x + 1, y + 1, x + w - 1, y + 16, 0xFF1E0B11);
        ctx.drawText(this.textRenderer, Text.translatable("droppoint.sandevistan.title"), x + 8, y + 4, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.literal("§e€$ " + SandevistanClient.clientEddies), x + w - 74, y + 4, 0xFCEE09, false);

        // Верхние вкладки категорий
        int tabW = 60;
        for (int i = 0; i != CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            int tx = x + 8 + i * (tabW + 3);
            boolean active = this.selectedCategory.equals(cat);
            boolean hover = isHovered(mouseX, mouseY, tx, y + 20, tabW, 16);
            drawCyberButton(ctx, tx, y + 20, tabW, 16, Text.translatable("droppoint.sandevistan.cat." + cat), active, hover, true);
        }
        // Вертикальный список принимаемых предметов
        List<DropPointManager.SellEntry> list = getFilteredEntries();
        if (this.selectedIndex >= list.size()) this.selectedIndex = Math.max(0, list.size() - 1);
        int maxScroll = Math.max(0, list.size() - 5);
        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;

        int listTop = y + 42;
        int visibleRows = Math.min(5, list.size() - this.scrollOffset);
        for (int r = 0; r != visibleRows; r++) {
            int idx = this.scrollOffset + r;
            DropPointManager.SellEntry entry = list.get(idx);
            int rowY = listTop + r * 26;
            boolean isSelected = (idx == this.selectedIndex);

            int rowBg = isSelected ? 0xDD38111E : 0xAA1B0A10;
            int rowBorder = isSelected ? 0xFF00F0FF : 0xFF4A1826;
            ctx.fill(x + 8, rowY, x + w - 8, rowY + 24, rowBg);
            ctx.fill(x + 8, rowY, x + 10, rowY + 24, rowBorder);
            ctx.fill(x + 8, rowY + 23, x + w - 8, rowY + 24, rowBorder);

            ctx.drawItem(new ItemStack(entry.item(), entry.batchSize()), x + 14, rowY + 4);
            ctx.drawText(this.textRenderer, entry.item().getName(), x + 36, rowY + 3, 0xFFFFFF, false);

            int owned = countInInventory(entry.item());
            String ownedColor = owned >= entry.batchSize() ? "§a" : "§7";
            String batchStr = entry.batchSize() > 1 ? " §8(x" + entry.batchSize() + ")" : "";
            Text subLine = Text.literal("§e€$ " + entry.price() + batchStr + "  " + ownedColor + "[" + owned + " шт.]");
            ctx.drawText(this.textRenderer, subLine, x + 36, rowY + 13, 0xFCEE09, false);
        }

        // Нижняя панель с информацией и двумя неоновыми кнопками
        ctx.fill(x + 8, y + h - 24, x + w - 162, y + h - 6, 0xFF1A090E);
        boolean canSell = false;
        long totalEarn = 0L;
        if (!list.isEmpty()) {
            DropPointManager.SellEntry sel = list.get(this.selectedIndex);
            int owned = countInInventory(sel.item());
            int batches = owned / sel.batchSize();
            canSell = batches > 0;
            totalEarn = batches * sel.price();
            String statusText = canSell ? "§a+€$" + totalEarn : "§7НЕТ В НАЛИЧИИ";
            ctx.drawText(this.textRenderer, Text.literal(statusText), x + 12, y + h - 19, 0xFFFFFF, false);
        }

        boolean hoverOne = canSell && isHovered(mouseX, mouseY, x + w - 156, y + h - 24, 72, 18);
        boolean hoverAll = canSell && isHovered(mouseX, mouseY, x + w - 80, y + h - 24, 72, 18);
        drawCyberButton(ctx, x + w - 156, y + h - 24, 72, 18, Text.literal("ПРОДАТЬ 1"), false, hoverOne, canSell);
        drawCyberButton(ctx, x + w - 80, y + h - 24, 72, 18, Text.literal("ПРОДАТЬ ВСЁ"), false, hoverAll, canSell);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
