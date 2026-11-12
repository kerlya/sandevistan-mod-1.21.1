package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class SandevistanHud {
    private static final long ACTIVE_MS = 8000L;
    private static final long COOLDOWN_MS = 30000L;

    public static void register() {
        HudRenderCallback.EVENT.register(SandevistanHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        if (SandevistanClient.isScanning()) {
            renderKiroshiScanner(ctx, client);
        }

        if (!SandevistanClient.isEquipped(client)) return;

        long elapsed = System.currentTimeMillis() - SandevistanClient.lastActivateMs;
        int x = 12;
        int y = client.getWindow().getScaledHeight() - 42;
        int width = 144;
        int height = 28;

        ctx.fill(x, y, x + width, y + height, 0xAA0D1117);

        float progress;
        int barColor;
        int borderColor;
        Text label;

        if (SandevistanClient.lastActivateMs == 0L || elapsed >= COOLDOWN_MS) {
            progress = 1.0f;
            barColor = 0xFF00E676;
            borderColor = 0xFF00C853;
            label = Text.translatable("hud.sandevistan.ready");
        } else if (elapsed < ACTIVE_MS) {
            progress = 1.0f - (float) elapsed / ACTIVE_MS;
            boolean pulse = (elapsed / 150L) % 2 == 0;
            barColor = pulse ? 0xFFFFEA00 : 0xFFC6FF00;
            borderColor = 0xFFFFD600;
            String leftSec = String.format(Locale.US, "%.1f", (ACTIVE_MS - elapsed) / 1000.0);
            label = Text.translatable("hud.sandevistan.active", leftSec);
        } else {
            long cdElapsed = elapsed - ACTIVE_MS;
            long cdTotal = COOLDOWN_MS - ACTIVE_MS;
            progress = (float) cdElapsed / cdTotal;
            barColor = 0xFFFF5252;
            borderColor = 0xFFD50000;
            String cdLeftSec = String.format(Locale.US, "%.1f", (COOLDOWN_MS - elapsed) / 1000.0);
            label = Text.translatable("hud.sandevistan.cooldown", cdLeftSec);
        }

        ctx.fill(x, y, x + width, y + 1, borderColor);
        ctx.fill(x, y + height - 1, x + width, y + height, borderColor);
        ctx.fill(x, y, x + 1, y + height, borderColor);
        ctx.fill(x + width - 1, y, x + width, y + height, borderColor);

        ctx.drawTextWithShadow(client.textRenderer, label, x + 6, y + 5, 0xFFFFFF);

        int barX = x + 6;
        int barY = y + 17;
        int barMaxW = width - 12;
        int filledW = Math.max(0, Math.min(barMaxW, (int) (barMaxW * progress)));

        ctx.fill(barX, barY, barX + barMaxW, barY + 6, 0xFF263238);
        if (filledW > 0) {
            ctx.fill(barX, barY, barX + filledW, barY + 6, barColor);
        }
    }

    private static void renderKiroshiScanner(DrawContext ctx, MinecraftClient client) {
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int cx = sw / 2;
        int cy = sh / 2;

        ctx.fill(0, 0, sw, sh, 0x2200FFA3);

        int scanCyan = 0xAA00F0FF;
        ctx.fill(16, 16, sw - 16, 17, scanCyan);
        ctx.fill(16, sh - 17, sw - 16, sh - 16, scanCyan);
        ctx.drawTextWithShadow(client.textRenderer, Text.translatable("scanner.sandevistan.header"), 22, 22, 0x00F0FF);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§eZOOM: 2.5x"), 22, sh - 28, 0xFCEE09);

        ctx.fill(cx - 28, cy - 1, cx - 12, cy + 1, 0xCCFF2A55);
        ctx.fill(cx + 12, cy - 1, cx + 28, cy + 1, 0xCCFF2A55);
        ctx.fill(cx - 1, cy - 22, cx + 1, cy - 10, 0xCC00F0FF);
        ctx.fill(cx - 1, cy + 10, cx + 1, cy + 22, 0xCC00F0FF);

        Entity target = raycastEntity(client, 45.0);
        if (target instanceof LivingEntity living) {
            String targetName = living.getName().getString().toUpperCase();
            Text aggression = getAggressionLabel(living);
            Text faction = getFactionLabel(living);
            String dist = String.format(Locale.US, "%.1f", client.player.distanceTo(living));
            String hp = String.format(Locale.US, "%.0f", living.getHealth());
            String maxHp = String.format(Locale.US, "%.0f", living.getMaxHealth());

            int cardX = cx + 36;
            int cardY = cy - 42;
            int cardW = 158;
            int cardH = 84;

            ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xDD14080C);
            ctx.fill(cardX, cardY, cardX + cardW, cardY + 2, 0xFF00F0FF);
            ctx.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFFFF2A55);
            ctx.fill(cardX, cardY, cardX + 2, cardY + cardH, 0xFFFF2A55);

            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.data"), cardX + 6, cardY + 6, 0x00F0FF, false);
            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.target"), cardX + 6, cardY + 19, 0xFF2A55, false);
            ctx.drawText(client.textRenderer, Text.literal("§f" + targetName), cardX + 6, cardY + 28, 0xFFFFFF, false);

            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.threat"), cardX + 6, cardY + 41, 0xFF2A55, false);
            ctx.drawText(client.textRenderer, aggression, cardX + 6, cardY + 50, 0xFFFFFF, false);

            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.faction", faction), cardX + 6, cardY + 63, 0xFCEE09, false);
            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.stats", dist, hp, maxHp), cardX + 6, cardY + 73, 0xA0AEC0, false);
        }
    }

    private static Text getAggressionLabel(LivingEntity entity) {
        if (entity instanceof HostileEntity) {
            return Text.translatable("threat.sandevistan.hostile");
        }
        if (entity instanceof Angerable || entity instanceof GolemEntity || entity instanceof PlayerEntity) {
            return Text.translatable("threat.sandevistan.neutral");
        }
        return Text.translatable("threat.sandevistan.peaceful");
    }

    private static Text getFactionLabel(LivingEntity entity) {
        if (entity instanceof PlayerEntity) return Text.translatable("faction.sandevistan.players");
        if (entity instanceof VillagerEntity || entity instanceof GolemEntity) return Text.translatable("faction.sandevistan.civilians");
        if (entity instanceof IllagerEntity) return Text.translatable("faction.sandevistan.illagers");
        if (entity.getType().isIn(EntityTypeTags.UNDEAD)) return Text.translatable("faction.sandevistan.undead");
        if (entity.getType().isIn(EntityTypeTags.ARTHROPOD)) return Text.translatable("faction.sandevistan.arthropods");
        if (entity.getWorld().getRegistryKey().getValue().getPath().contains("nether")) return Text.translatable("faction.sandevistan.nether");
        if (entity.getWorld().getRegistryKey().getValue().getPath().contains("end")) return Text.translatable("faction.sandevistan.end");
        return Text.translatable("faction.sandevistan.fauna");
    }

    private static Entity raycastEntity(MinecraftClient client, double maxDist) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) return null;
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d dir = camera.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(maxDist));
        Box box = camera.getBoundingBox().stretch(dir.multiply(maxDist)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(camera, start, end, box, e -> !e.isSpectator() && e.canHit(), maxDist * maxDist);
        return hit != null ? hit.getEntity() : null;
    }
}
