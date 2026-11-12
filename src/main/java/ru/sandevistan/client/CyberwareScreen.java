package ru.sandevistan.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import ru.sandevistan.CyberwareScreenHandler;

public class CyberwareScreen extends HandledScreen<CyberwareScreenHandler> {

    public CyberwareScreen(CyberwareScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 196;
        this.backgroundHeight = 206;
        this.playerInventoryTitleX = 18;
        this.playerInventoryTitleY = 112;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Основной темный фон интерфейса рипердока
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xF0090D16);

        // Внешняя неоново-желтая рамка Cyberpunk
        ctx.fill(x, y, x + backgroundWidth, y + 2, 0xFFFCEE09);
        ctx.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFFFCEE09);
        ctx.fill(x, y, x + 2, y + backgroundHeight, 0xFFFCEE09);
        ctx.fill(x + backgroundWidth - 2, y, x + backgroundWidth, y + backgroundHeight, 0xFFFCEE09);

        // Верхняя плашка заголовка
        ctx.fill(x + 2, y + 2, x + backgroundWidth - 2, y + 16, 0xFF151C28);

        // Центральный голографический силуэт человека
        int cx = x + backgroundWidth / 2;
        int cy = y + 24;
        int holoFill = 0x4400F0FF;
        int holoBorder = 0xAA00F0FF;

        // Голова + визор
        drawHoloPart(ctx, cx - 7, cy, 14, 14, holoFill, holoBorder);
        ctx.fill(cx - 5, cy + 5, cx + 5, cy + 7, 0xFFFCEE09);

        // Шея и торс
        ctx.fill(cx - 3, cy + 14, cx + 3, cy + 17, holoFill);
        drawHoloPart(ctx, cx - 11, cy + 17, 22, 30, holoFill, holoBorder);
        // Линия позвоночника (подсветка под Сандевистан)
        ctx.fill(cx - 1, cy + 19, cx + 1, cy + 45, 0xFF00E676);

        // Руки (левая и правая)
        drawHoloPart(ctx, cx - 18, cy + 18, 5, 26, holoFill, holoBorder);
        drawHoloPart(ctx, cx + 13, cy + 18, 5, 26, holoFill, holoBorder);

        // Ноги (левая и правая)
        drawHoloPart(ctx, cx - 10, cy + 49, 8, 28, holoFill, holoBorder);
        drawHoloPart(ctx, cx + 2, cy + 49, 8, 28, holoFill, holoBorder);

        // Соединительные линии от слотов к телу
        // 0: Спина -> позвоночник
        ctx.fill(x + 42, y + 38, cx - 1, y + 39, 0xFF00F0FF);
        // 1: Руки -> правая рука
        ctx.fill(cx + 18, y + 38, x + 154, y + 39, 0xFF3A4750);
        // 2: Ноги -> правая нога
        ctx.fill(cx + 10, y + 82, x + 154, y + 83, 0xFF3A4750);
        // 3: Кожа -> торс/бок
        ctx.fill(x + 42, y + 82, cx - 11, y + 83, 0xFF3A4750);

        // Отрисовка 4 слотов имплантов и подписей
        drawImplantSlot(ctx, x + 24, y + 30, "СПИНА", true);
        drawImplantSlot(ctx, x + 156, y + 30, "РУКИ", false);
        drawImplantSlot(ctx, x + 156, y + 74, "НОГИ", false);
        drawImplantSlot(ctx, x + 24, y + 74, "КОЖА", false);

        // Сетка инвентаря игрока (36 слотов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawVanillaSlot(ctx, x + 18 + col * 18, y + 124 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawVanillaSlot(ctx, x + 18 + col * 18, y + 182);
        }
    }

    private void drawHoloPart(DrawContext ctx, int px, int py, int w, int h, int fill, int border) {
        ctx.fill(px, py, px + w, py + h, fill);
        ctx.fill(px, py, px + w, py + 1, border);
        ctx.fill(px, py + h - 1, px + w, py + h, border);
        ctx.fill(px, py, px + 1, py + h, border);
        ctx.fill(px + w - 1, py, px + w, py + h, border);
    }

    private void drawImplantSlot(DrawContext ctx, int sx, int sy, String label, boolean active) {
        int border = active ? 0xFF00F0FF : 0xFF3A4750;
        ctx.fill(sx - 2, sy - 2, sx + 18, sy + 18, border);
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF101622);
        int textW = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, sx + 8 - textW / 2, sy - 10, active ? 0x00F0FF : 0x718096, false);
    }

    private void drawVanillaSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2D3748);
        ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF171923);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("§e§lКИБЕРИМПЛАНТЫ // РИПЕРДОК"), 8, 5, 0xFCEE09, false);
        ctx.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xA0AEC0, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
