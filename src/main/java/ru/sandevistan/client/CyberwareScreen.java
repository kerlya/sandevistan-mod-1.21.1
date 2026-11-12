package ru.sandevistan.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import ru.sandevistan.CyberwareScreenHandler;
import ru.sandevistan.CyberwareStorage;

public class CyberwareScreen extends HandledScreen<CyberwareScreenHandler> {

    public CyberwareScreen(CyberwareScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 216;
        this.backgroundHeight = 216;
        this.playerInventoryTitleX = 28;
        this.playerInventoryTitleY = 122;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xF412070B);

        int redNeon = 0xFFFF2A55;
        int cyanNeon = 0xFF00F0FF;
        ctx.fill(x, y, x + backgroundWidth, y + 1, redNeon);
        ctx.fill(x, y + backgroundHeight - 1, x + backgroundWidth, y + backgroundHeight, redNeon);
        ctx.fill(x, y, x + 1, y + backgroundHeight, redNeon);
        ctx.fill(x + backgroundWidth - 1, y, x + backgroundWidth, y + backgroundHeight, redNeon);

        ctx.fill(x + 1, y + 1, x + backgroundWidth - 1, y + 15, 0xFF1E0B11);
        ctx.fill(x + 8, y + 14, x + 84, y + 15, cyanNeon);

        int installedCount = 0;
        for (int i = 0; i < CyberwareStorage.SLOT_COUNT; i++) {
            if (this.handler.getSlot(i).hasStack()) installedCount++;
        }
        int barX = x + 10;
        int barTopY = y + 26;
        ctx.drawText(this.textRenderer, "§c" + installedCount + "/" + CyberwareStorage.SLOT_COUNT, barX - 2, barTopY - 8, 0xFF2A55, false);
        for (int seg = 0; seg < 15; seg++) {
            int sy = barTopY + seg * 5;
            boolean filled = (15 - seg) <= installedCount * 3;
            int segColor = filled ? (installedCount >= 4 ? 0xFFFF2A55 : 0xFFFCEE09) : 0xFF33151E;
            ctx.fill(barX, sy, barX + 10, sy + 3, segColor);
        }

        int cx = x + 108;
        int cy = y + 24;
        int bodyFill = 0x38FF5E7E;
        int bodyEdge = 0x9970D6FF;

        drawHoloRect(ctx, cx - 6, cy, 12, 13, bodyFill, bodyEdge);
        ctx.fill(cx - 4, cy + 4, cx + 4, cy + 6, cyanNeon);

        ctx.fill(cx - 2, cy + 13, cx + 2, cy + 16, bodyFill);
        drawHoloRect(ctx, cx - 10, cy + 16, 20, 32, bodyFill, bodyEdge);
        ctx.fill(cx - 1, cy + 18, cx + 1, cy + 46, 0xCC00F0FF);

        for (int step = 0; step < 5; step++) {
            ctx.fill(cx - 13 - step * 2, cy + 18 + step * 5, cx - 10 - step * 2, cy + 24 + step * 5, bodyEdge);
            ctx.fill(cx + 10 + step * 2, cy + 18 + step * 5, cx + 13 + step * 2, cy + 24 + step * 5, bodyEdge);
        }

        drawHoloRect(ctx, cx - 9, cy + 49, 7, 34, bodyFill, bodyEdge);
        drawHoloRect(ctx, cx + 2, cy + 49, 7, 34, bodyFill, bodyEdge);

        ctx.fill(cx + 1, y + 39, x + 150, y + 40, cyanNeon);
        ctx.fill(cx + 4, y + 29, x + 144, y + 30, 0x6600F0FF);
        ctx.fill(x + 144, y + 29, x + 145, y + 72, 0x6600F0FF);
        ctx.fill(x + 144, y + 71, x + 150, y + 72, cyanNeon);
        ctx.fill(x + 66, y + 44, cx - 13, y + 45, 0x66FF2A55);
        ctx.fill(x + 66, y + 92, cx - 10, y + 93, 0x66FF2A55);
        ctx.fill(cx + 9, y + 104, x + 150, y + 105, 0x66FF2A55);

        drawCyberSlot(ctx, x + 152, y + 32, Text.translatable("gui.sandevistan.slot.os").getString(), true);
        drawCyberSlot(ctx, x + 152, y + 64, Text.translatable("gui.sandevistan.slot.eyes").getString(), true);
        drawCyberSlot(ctx, x + 48, y + 36, Text.translatable("gui.sandevistan.slot.arms").getString(), false);
        drawCyberSlot(ctx, x + 48, y + 84, Text.translatable("gui.sandevistan.slot.skin").getString(), false);
        drawCyberSlot(ctx, x + 152, y + 96, Text.translatable("gui.sandevistan.slot.legs").getString(), false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInvSlot(ctx, x + 28 + col * 18, y + 134 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawInvSlot(ctx, x + 28 + col * 18, y + 192);
        }
    }

    private void drawHoloRect(DrawContext ctx, int px, int py, int w, int h, int fill, int border) {
        ctx.fill(px, py, px + w, py + h, fill);
        ctx.fill(px, py, px + w, py + 1, border);
        ctx.fill(px, py + h - 1, px + w, py + h, border);
        ctx.fill(px, py, px + 1, py + h, border);
        ctx.fill(px + w - 1, py, px + w, py + h, border);
    }

    private void drawCyberSlot(DrawContext ctx, int sx, int sy, String label, boolean unlocked) {
        int outer = unlocked ? 0xFFFF2A55 : 0xFF4A1826;
        int innerCorner = unlocked ? 0xFF00F0FF : 0xFF2B1018;
        ctx.fill(sx - 2, sy - 2, sx + 18, sy + 18, outer);
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A090E);
        ctx.fill(sx - 2, sy - 2, sx + 2, sy - 1, innerCorner);
        ctx.fill(sx + 14, sy + 17, sx + 18, sy + 18, innerCorner);

        int tw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, sx + 8 - tw / 2, sy - 10, unlocked ? 0xFF4D70 : 0x7A3B4C, false);
    }

    private void drawInvSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF3B1622);
        ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF180A0F);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.title"), 8, 4, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.inventory"), this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF2A55, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
