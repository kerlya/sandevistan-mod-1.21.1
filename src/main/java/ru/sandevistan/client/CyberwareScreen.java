package ru.sandevistan.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import ru.sandevistan.CyberwareScreenHandler;

public class CyberwareScreen extends HandledScreen<CyberwareScreenHandler> {

    public CyberwareScreen(CyberwareScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = 72;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Основной темный фон окна
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xEE0B0E14);

        // Внешняя неоново-желтая рамка Cyberpunk
        ctx.fill(x, y, x + backgroundWidth, y + 2, 0xFFFCEE09);
        ctx.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFFFCEE09);
        ctx.fill(x, y, x + 2, y + backgroundHeight, 0xFFFCEE09);
        ctx.fill(x + backgroundWidth - 2, y, x + backgroundWidth, y + backgroundHeight, 0xFFFCEE09);

        // Верхняя полоса заголовка
        ctx.fill(x + 2, y + 2, x + backgroundWidth - 2, y + 16, 0xFF1A202C);

        // Подписи над 4 слотами имплантов
        String[] labels = {"СПИНА", "РУКИ", "НОГИ", "КОЖА"};
        int[] slotX = {26, 62, 98, 134};
        for (int i = 0; i < 4; i++) {
            int sx = x + slotX[i];
            int sy = y + 32;
            // Неоновая голубая рамка вокруг слота импланта
            int borderColor = (i == 0) ? 0xFF00F0FF : 0xFF3A4750;
            ctx.fill(sx - 2, sy - 2, sx + 18, sy + 18, borderColor);
            ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF121820);

            // Текст названия слота над ячейкой
            int textW = this.textRenderer.getWidth(labels[i]);
            ctx.drawText(this.textRenderer, labels[i], sx + 8 - textW / 2, sy - 11, (i == 0) ? 0x00F0FF : 0x718096, false);
        }

        // Отрисовка сетки обычного инвентаря игрока (36 слотов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawVanillaSlot(ctx, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawVanillaSlot(ctx, x + 8 + col * 18, y + 142);
        }
    }

    private void drawVanillaSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2D3748);
        ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF171923);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("§e§lКИБЕРИМПЛАНТЫ"), 8, 5, 0xFCEE09, false);
        ctx.drawText(this.textRenderer, this.playerInventoryTitle, 8, this.playerInventoryTitleY, 0xA0AEC0, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
