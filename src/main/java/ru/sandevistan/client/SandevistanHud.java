package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class SandevistanHud {
    private static final long ACTIVE_MS = 8000L;
    private static final long COOLDOWN_MS = 30000L;

    public static void register() {
        HudRenderCallback.EVENT.register(SandevistanHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!SandevistanClient.isEquipped(client)) return;

        long elapsed = System.currentTimeMillis() - SandevistanClient.lastActivateMs;
        int x = 12;
        int y = client.getWindow().getScaledHeight() - 42;
        int width = 132;
        int height = 28;

        // Полупрозрачный темный фон и рамка
        ctx.fill(x, y, x + width, y + height, 0xAA0D1117);

        float progress;
        int barColor;
        int borderColor;
        String label;

        if (SandevistanClient.lastActivateMs == 0L || elapsed >= COOLDOWN_MS) {
            progress = 1.0f;
            barColor = 0xFF00E676; // Неоновый зеленый
            borderColor = 0xFF00C853;
            label = "§aСАНДЕВИСТАН: ГОТОВ [R]";
        } else if (elapsed < ACTIVE_MS) {
            progress = 1.0f - (float) elapsed / ACTIVE_MS;
            // Легкая пульсация цвета во время работы
            boolean pulse = (elapsed / 150L) % 2 == 0;
            barColor = pulse ? 0xFFFFEA00 : 0xFFC6FF00;
            borderColor = 0xFFFFD600;
            double leftSec = (ACTIVE_MS - elapsed) / 1000.0;
            label = String.format("§eАКТИВЕН: %.1fс", leftSec);
        } else {
            long cdElapsed = elapsed - ACTIVE_MS;
            long cdTotal = COOLDOWN_MS - ACTIVE_MS;
            progress = (float) cdElapsed / cdTotal;
            barColor = 0xFFFF5252; // Красно-оранжевый откат
            borderColor = 0xFFD50000;
            double cdLeftSec = (COOLDOWN_MS - elapsed) / 1000.0;
            label = String.format("§cОЖИДАНИЕ: %.1fс", cdLeftSec);
        }

        // Тонкая цветная обводка по краям
        ctx.fill(x, y, x + width, y + 1, borderColor);
        ctx.fill(x, y + height - 1, x + width, y + height, borderColor);
        ctx.fill(x, y, x + 1, y + height, borderColor);
        ctx.fill(x + width - 1, y, x + width, y + height, borderColor);

        // Текст статуса
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 6, y + 5, 0xFFFFFF);

        // Подложка и движущаяся полоска прогресса
        int barX = x + 6;
        int barY = y + 17;
        int barMaxW = width - 12;
        int filledW = Math.max(0, Math.min(barMaxW, (int) (barMaxW * progress)));

        ctx.fill(barX, barY, barX + barMaxW, barY + 6, 0xFF263238);
        if (filledW > 0) {
            ctx.fill(barX, barY, barX + filledW, barY + 6, barColor);
        }
    }
}

