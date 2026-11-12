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
        ctx.drawHorizontalLine(x1, x2 - 1, y1, color);
        ctx.drawHorizontalLine(x1, x2 - 1, y2 - 1, color);
        ctx.drawVerticalLine(x1, y1, y2 - 1, color);
        ctx.drawVerticalLine(x2 - 1, y1, y2 - 1, color);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        int w = this.backgroundWidth;
        int h = this.backgroundHeight;

        ctx.fill(x, y, x + w, y + h, 0xEE18080D);
        drawNeonBox(ctx, x, y, x + w, y + h, 0xFFFF2A55);

        // Подчеркивание заголовка и шкала уровня/опыта справа
        ctx.drawHorizontalLine(x + 7, x + 94, y + 15, 0xFF00F0FF);

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

        // Индикатор X/5 и вертикальная шкала слева
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

        // Неоновый силуэт человека по центру с руками в А-позе
        int cx = x + 96;
        int cy = y + 32;
        int cyan = 0xFF00F0FF;
        int red = 0xFF7A1C32;

        // Голова и визор
        drawNeonBox(ctx, cx - 7, cy, cx + 7, cy + 12, cyan);
        ctx.fill(cx - 4, cy + 4, cx + 4, cy + 6, cyan);

        // Торс и позвоночник
        drawNeonBox(ctx, cx - 10, cy + 14, cx + 10, cy + 44, cyan);
        ctx.fill(cx - 1, cy + 16, cx + 1, cy + 42, red);

        // Левая рука в А-позе (плечо -> предплечье -> кисть со сдвигом влево)
        drawNeonBox(ctx, cx - 15, cy + 15, cx - 11, cy + 25, cyan);
        drawNeonBox(ctx, cx - 17, cy + 24, cx - 13, cy + 34, cyan);
        drawNeonBox(ctx, cx - 19, cy + 33, cx - 15, cy + 41, cyan);

        // Правая рука в А-позе (плечо -> предплечье -> кисть со сдвигом вправо)
        drawNeonBox(ctx, cx + 11, cy + 15, cx + 15, cy + 25, cyan);
        drawNeonBox(ctx, cx + 13, cy + 24, cx + 17, cy + 34, cyan);
        drawNeonBox(ctx, cx + 15, cy + 33, cx + 19, cy + 41, cyan);

        // Ноги
        drawNeonBox(ctx, cx - 9, cy + 46, cx - 2, cy + 74, cyan);
        drawNeonBox(ctx, cx + 2, cy + 46, cx + 9, cy + 74, cyan);

        // Соединительные линии от всех 5 слотов к телу
        int cOs = this.handler.slots.get(0).hasStack() ? cyan : red;
        int cEyes = this.handler.slots.get(1).hasStack() ? cyan : red;
        int cArms = this.handler.slots.get(2).hasStack() ? cyan : red;
        int cSkin = this.handler.slots.get(3).hasStack() ? cyan : red;
        int cLegs = this.handler.slots.get(4).hasStack() ? cyan : red;

        // 1. ОС (справа сверху) -> верх торса
        ctx.drawHorizontalLine(cx + 10, x + 132, y + 46, cOs);

        // 2. ГЛАЗА (справа по центру) -> визор головы
        ctx.drawHorizontalLine(cx + 7, cx + 19, y + 37, cEyes);
        ctx.drawVerticalLine(cx + 19, y + 37, y + 70, cEyes);
        ctx.drawHorizontalLine(cx + 19, x + 132, y + 70, cEyes);

        // 3. РУКИ (слева сверху) -> левая рука в А-позе
        ctx.drawHorizontalLine(x + 59, cx - 15, y + 49, cArms);

        // 4. КОЖА (слева снизу) -> правый край торса через низ
        ctx.drawHorizontalLine(x + 59, cx - 12, y + 82, cSkin);
        ctx.drawVerticalLine(cx - 12, y + 76, y + 82, cSkin);
        ctx.drawHorizontalLine(cx - 12, cx + 10, y + 76, cSkin);
        ctx.drawVerticalLine(cx + 10, y + 44, y + 76, cSkin);

        // 5. НОГИ (справа снизу) -> правая нога
        ctx.drawHorizontalLine(cx + 9, x + 132, y + 98, cLegs);

        // Слоты
        for (Slot slot : this.handler.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean isImplantSlot = slot.id < 5;
            int border = isImplantSlot ? (slot.hasStack() ? 0xFFFF2A55 : 0xFF6B1E34) : 0xFF521826;
            int fill = isImplantSlot ? 0xFF230B13 : 0xFF1A090E;
            ctx.fill(sx, sy, sx + 18, sy + 18, fill);
            drawNeonBox(ctx, sx, sy, sx + 18, sy + 18, border);
        }

        // Подписи слотов
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.arms"), x + 38, y + 30, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.skin"), x + 38, y + 64, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.os"), x + 137, y + 24, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.eyes"), x + 130, y + 52, 0xFF4D70, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.legs"), x + 132, y + 80, 0xFF4D70, false);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.inventory"), this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF4D70, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
