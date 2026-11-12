package ru.sandevistan.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import ru.sandevistan.CyberwareScreenHandler;

public class CyberwareScreen extends HandledScreen<CyberwareScreenHandler> {
    public CyberwareScreen(CyberwareScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 196;
        this.backgroundHeight = 208;
        this.titleX = 8;
        this.titleY = 5;
        this.playerInventoryTitleX = 18;
        this.playerInventoryTitleY = 114;
    }

    private void drawNeonBox(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        int w = this.backgroundWidth;
        int h = this.backgroundHeight;

        ctx.fill(x, y, x + w, y + h, 0xEE18080D);
        drawNeonBox(ctx, x, y, x + w, y + h, 0xFFFF2A55);

        // Шапка с подчеркиванием и уровнем + шкалой опыта
        ctx.fill(x + 7, y + 15, x + 94, y + 16, 0xFF00F0FF);

        int lvl = SandevistanClient.clientCyberLevel;
        int xp = SandevistanClient.clientCyberXp;
        int nextXp = Math.max(1, SandevistanClient.clientCyberNextXp);
        String lvlText = "§eLVL " + lvl + " §7(" + xp + "/" + nextXp + ")";
        int lvlW = this.textRenderer.getWidth(lvlText);
        ctx.drawText(this.textRenderer, lvlText, x + w - lvlW - 7, y + 5, 0xFCEE09, false);

        int xpBarX = x + 104;
        int xpBarY = y + 15;
        int xpBarW = w - 111;
        int xpFillW = Math.max(0, Math.min(xpBarW, (int) ((long) xpBarW * xp / nextXp)));
        ctx.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + 3, 0xFF280D15);
        if (xpFillW > 0) {
            ctx.fill(xpBarX, xpBarY, xpBarX + xpFillW, xpBarY + 3, 0xFF00F0FF);
        }

        // Подсчет установленных имплантов (X/5) и вертикальная желтая шкала слева
        int equippedCount = 0;
        for (int i = 0; i < 5; i++) {
            if (this.handler.slots.get(i).hasStack()) equippedCount++;
        }
        ctx.drawText(this.textRenderer, Text.literal(equippedCount + "/5"), x + 8, y + 20, 0xFF4D70, false);

        int totalBars = 15;
        int litBars = equippedCount * 3;
        for (int i = 0; i < totalBars; i++) {
            int by = y + 98 - i * 4;
            int barColor = (i < litBars) ? 0xFFFCEE09 : 0xFF33141E;
            ctx.fill(x + 11, by, x + 22, by + 2, barColor);
        }

        // Неоновый силуэт тела по центру (как в оригинале)
        int cx = x + 96;
        int cy = y + 32;
        int cyan = 0xFF00F0FF;
        int red = 0xFF8A2238;

        // Голова и визор
        drawNeonBox(ctx, cx - 7, cy, cx + 7, cy + 12, cyan);
        ctx.fill(cx - 4, cy + 4, cx + 4, cy + 6, cyan);

        // Торс и позвоночник
        drawNeonBox(ctx, cx - 10, cy + 14, cx + 10, cy + 44, cyan);
        ctx.fill(cx - 1, cy + 16, cx + 1, cy + 42, red);

        // Левая и правая руки
        drawNeonBox(ctx, cx - 16, cy + 15, cx - 11, cy + 40, cyan);
        drawNeonBox(ctx, cx + 11, cy + 15, cx + 16, cy + 40, cyan);

        // Левая и правая ноги
        drawNeonBox(ctx, cx - 9, cy + 46, cx - 2, cy + 74, cyan);
        drawNeonBox(ctx, cx + 2, cy + 46, cx + 9, cy + 74, cyan);

        // Линии от слотов к телу
        // ОС -> голова
        ctx.fill(cx + 7, cy + 6, x + 133, cy + 6, cyan);
        // ГЛАЗА -> визор
        ctx.fill(cx + 4, cy + 9, cx + 18, cy + 9, cyan);
        ctx.fill(cx + 18, cy + 9, cx + 19, y + 70, cyan);
        ctx.fill(cx + 18, y + 70, x + 133, y + 70, cyan);
        // РУКИ -> левая рука
        ctx.fill(x + 59, y + 48, cx - 16, y + 48, red);
        // КОЖА -> торс
        ctx.fill(x + 59, y + 82, cx - 10, y + 82, red);
        // НОГИ -> правая нога
        ctx.fill(cx + 9, y + 98, x + 133, y + 98, red);

        // Отрисовка слотов
        for (Slot slot : this.handler.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean isImplantSlot = slot.id < 5;
            int border = isImplantSlot ? (slot.hasStack() ? 0xFFFF2A55 : 0xFF6B1E34) : 0xFF521826;
            int fill = isImplantSlot ? 0xFF230B13 : 0xFF1A090E;
            ctx.fill(sx, sy, sx + 18, sy + 18, fill);
            drawNeonBox(ctx, sx, sy, sx + 18, sy + 18, border);
        }

        // Подписи слотов точно над каждым слотом
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.arms"), x + 38, y + 30, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.skin"), x + 38, y + 64, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.os"), x + 137, y + 24, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.eyes"), x + 130, y + 52, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.legs"), x + 132, y + 80, 0xFF4D70, false);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF4D70, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
