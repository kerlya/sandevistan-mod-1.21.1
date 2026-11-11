package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class SandevistanHud {
    private SandevistanHud() {}
    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player == null) return;
            int x = 10, y = c.getWindow().getScaledHeight() - 38;
            ctx.fill(x, y, x+132, y+34, 0xAA101A1B);
            ctx.drawText(c.textRenderer, Text.literal("САНДЕВИСТАН"), x+6, y+5, 0xFFD9F05A, false);
            ctx.fill(x+6, y+19, x+126, y+25, 0xFF455044);
            ctx.fill(x+6, y+19, x+126, y+25, 0xFFB9D94A);
            ctx.drawText(c.textRenderer, Text.literal("R — активация"), x+6, y+27, 0xFFFFFFFF, false);
        });
    }
}
