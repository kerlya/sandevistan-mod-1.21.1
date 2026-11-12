package ru.sandevistan.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import ru.sandevistan.CyberwareScreenHandler;

public class CyberwareScreen extends HandledScreen<CyberwareScreenHandler> {
    public CyberwareScreen(CyberwareScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 216;
        this.titleX = 8;
        this.titleY = 5;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 122;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        int w = this.backgroundWidth;
        int h = this.backgroundHeight;

        ctx.fill(x, y, x + w, y + h, 0xEE18080D);
        ctx.fill(x, y, x + w, y + 1, 0xFFFF2A55);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFF2A55);
        ctx.fill(x, y, x + 1, y + h, 0xFFFF2A55);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFF2A55);

        ctx.fill(x + 1, y + 1, x + w - 1, y + 23, 0xFF280D15);
        ctx.fill(x + 1, y + 23, x + w - 1, y + 24, 0xFF00F0FF);

        // Шкала опыта и уровень в шапке меню имплантов
        int lvl = SandevistanClient.clientCyberLevel;
        int xp = SandevistanClient.clientCyberXp;
        int nextXp = Math.max(1, SandevistanClient.clientCyberNextXp);
        String lvlText = "§eLVL " + lvl + " §7(" + xp + "/" + nextXp + ")";
        int lvlW = this.textRenderer.getWidth(lvlText);
        ctx.drawText(this.textRenderer, lvlText, x + w - lvlW - 7, y + 5, 0xFCEE09, false);

        int barX = x + 8;
        int barY = y + 16;
        int barW = w - 16;
        int fillW = Math.max(0, Math.min(barW, (int) ((long) barW * xp / nextXp)));
        ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF14070B);
        if (fillW > 0) {
            ctx.fill(barX, barY, barX + fillW, barY + 4, 0xFF00F0FF);
        }

        ctx.fill(x + 54, y + 27, x + 122, y + 115, 0xFF110609);
        ctx.fill(x + 54, y + 27, x + 122, y + 28, 0xFF00F0FF);
        ctx.fill(x + 54, y + 114, x + 122, y + 115, 0xFF00F0FF);
        ctx.fill(x + 54, y + 27, x + 55, y + 115, 0xFF00F0FF);
        ctx.fill(x + 121, y + 27, x + 122, y + 115, 0xFF00F0FF);

        if (this.client != null && this.client.player != null) {
            InventoryScreen.drawEntity(ctx, x + 56, y + 30, x + 120, y + 112, 34, 0.0625F, mouseX, mouseY, this.client.player);
        }

        for (Slot slot : this.handler.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean isImplantSlot = slot.id < 5;
            int border = isImplantSlot ? 0xFF00F0FF : 0xFF521826;
            int fill = isImplantSlot ? 0xFF230B13 : 0xFF1A090E;
            ctx.fill(sx, sy, sx + 18, sy + 18, fill);
            ctx.fill(sx, sy, sx + 18, sy + 1, border);
            ctx.fill(sx, sy + 17, sx + 18, sy + 18, border);
            ctx.fill(sx, sy, sx + 1, sy + 18, border);
            ctx.fill(sx + 17, sy, sx + 18, sy + 18, border);
        }

        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.os"), x + 21, y + 24, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.eyes"), x + 21, y + 52, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.arms"), x + 137, y + 24, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.skin"), x + 137, y + 52, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.translatable("gui.sandevistan.slot.legs"), x + 137, y + 80, 0x00F0FF, false);
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
