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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.sandevistan.RipperdocEntity;
import java.util.List;
import java.util.Locale;

public final class SandevistanHud {
    private static final long ACTIVE_MS = 8000L;
    private static final long COOLDOWN_MS = 30000L;

    public static void register() {
        HudRenderCallback.EVENT.register(SandevistanHud::render);
    }

    public static String formatFixerName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        List<String> parts = List.of(raw.split(":"));
        if (parts.size() == 3) {
            String first = Text.translatable("fixer.sandevistan.first." + parts.get(0)).getString();
            String nick = Text.translatable("fixer.sandevistan.nick." + parts.get(1)).getString();
            String last = Text.translatable("fixer.sandevistan.last." + parts.get(2)).getString();
            return first + " " + nick + " " + last;
        }
        return raw;
    }

    public static int countRequiredItems(MinecraftClient client, String targetType) {
        if (client.player == null) return 0;
        Item req = switch (targetType) {
            case "iron_ingot" -> Items.IRON_INGOT;
            case "redstone" -> Items.REDSTONE;
            case "gold_ingot" -> Items.GOLD_INGOT;
            case "diamond" -> Items.DIAMOND;
            default -> null;
        };
        if (req == null) return 0;
        int total = 0;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack st = client.player.getInventory().getStack(i);
            if (st.isOf(req)) total += st.getCount();
        }
        return total;
    }

    private static String formatTimer(long msLeft) {
        long sec = Math.max(0L, msLeft / 1000L);
        return String.format(Locale.US, "%02d:%02d", sec / 60L, sec % 60L);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        if (SandevistanClient.isScanning()) {
            renderKiroshiScanner(ctx, client);
        }

        renderPhoneWidget(ctx, client);

        if (SandevistanClient.clientCallState == 1) {
            renderIncomingCall(ctx, client);
        } else if (SandevistanClient.clientCallState == 2) {
            renderActiveContractTracker(ctx, client);
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
            String actKey = SandevistanClient.getKeyLabel(SandevistanClient.activateKey, "R");
            label = Text.translatable("hud.sandevistan.ready", actKey);
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

    private static void renderPhoneWidget(DrawContext ctx, MinecraftClient client) {
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        String phoneKeyName = SandevistanClient.getKeyLabel(SandevistanClient.phoneKey, "Z");
        int keyTextW = client.textRenderer.getWidth("[" + phoneKeyName + "]");
        int px = sw - 24 - keyTextW;
        int py = sh - 34;

        ctx.fill(px, py, px + 14, py + 22, 0xCC0D1821);
        ctx.fill(px, py, px + 14, py + 1, 0xFF00F0FF);
        ctx.fill(px, py + 21, px + 14, py + 22, 0xFF00F0FF);
        ctx.fill(px, py, px + 1, py + 22, 0xFF00F0FF);
        ctx.fill(px + 13, py, px + 14, py + 22, 0xFF00F0FF);
        ctx.fill(px + 2, py + 3, px + 12, py + 17, 0x6600F0FF);
        ctx.fill(px + 5, py + 19, px + 9, py + 20, 0xFF00F0FF);

        if (SandevistanClient.clientCallState > 0 || SandevistanClient.clientHasUnreadSms) {
            ctx.fill(px + 11, py - 2, px + 15, py + 2, 0xFFFF2A55);
        }

        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§b[" + phoneKeyName + "]"), px + 17, py + 7, 0x00F0FF);
    }

    private static void renderIncomingCall(DrawContext ctx, MinecraftClient client) {
        int x = 14;
        int y = 52;
        int red = 0xFFFF2A55;
        int cyan = 0xFF00F0FF;

        ctx.fill(x + 18, y - 4, x + 52, y + 36, 0x33FF2A55);
        ctx.fill(x + 15, y - 2, x + 49, y + 38, 0x55FF2A55);
        ctx.fill(x + 12, y, x + 46, y + 40, 0xDD1A080D);
        ctx.fill(x + 12, y, x + 46, y + 1, red);
        ctx.fill(x + 12, y + 39, x + 46, y + 40, red);
        ctx.fill(x + 12, y, x + 13, y + 40, red);
        ctx.fill(x + 45, y, x + 46, y + 40, red);

        ctx.fill(x + 23, y + 7, x + 35, y + 19, 0xAAFF4D70);
        ctx.fill(x + 19, y + 21, x + 39, y + 36, 0x88FF4D70);
        ctx.fill(x + 25, y + 11, x + 33, y + 13, cyan);

        ctx.fill(x, y + 6, x + 8, y + 19, 0xFF00F0FF);
        ctx.fill(x + 2, y + 8, x + 6, y + 15, 0xFF0B141A);

        int tx = x + 52;
        ctx.drawText(client.textRenderer, Text.literal("§7NC_NET // 5G"), tx, y - 2, 0x8899A6, false);
        String resolvedFixer = formatFixerName(SandevistanClient.clientFixerName).toUpperCase();
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§b§l" + resolvedFixer), tx, y + 8, 0x00F0FF);
        ctx.drawTextWithShadow(client.textRenderer, Text.translatable("call.sandevistan.incoming"), tx, y + 19, 0xFF2A55);

        Text targetName = Text.translatable("target.sandevistan." + SandevistanClient.clientTargetType);
        String timeTag = SandevistanClient.clientDeadlineMs > 0L ? " §c[" + formatTimer(SandevistanClient.clientDeadlineMs) + "]" : "";
        ctx.drawTextWithShadow(client.textRenderer,
            Text.translatable("call.sandevistan.desc", targetName, SandevistanClient.clientGoal, SandevistanClient.clientReward).append(timeTag),
            tx, y + 30, 0xFCEE09);

        int btnY = y + 43;
        String acceptKeyName = SandevistanClient.getKeyLabel(SandevistanClient.acceptCallKey, "Y");
        String rejectKeyName = SandevistanClient.getKeyLabel(SandevistanClient.rejectCallKey, "N");
        int nextBtnX = drawCallKeyButton(ctx, client, tx, btnY, acceptKeyName, Text.translatable("call.sandevistan.answer"));
        drawCallKeyButton(ctx, client, nextBtnX + 10, btnY, rejectKeyName, Text.translatable("call.sandevistan.reject"));
    }
    private static int drawCallKeyButton(DrawContext ctx, MinecraftClient client, int bx, int by, String key, Text label) {
        int keyW = Math.max(13, client.textRenderer.getWidth(key) + 6);
        ctx.fill(bx, by, bx + keyW, by + 13, 0xCC1A080D);
        ctx.fill(bx, by, bx + keyW, by + 1, 0xFFFF2A55);
        ctx.fill(bx, by + 12, bx + keyW, by + 13, 0xFFFF2A55);
        ctx.fill(bx, by, bx + 1, by + 13, 0xFFFF2A55);
        ctx.fill(bx + keyW - 1, by, bx + keyW, by + 13, 0xFFFF2A55);
        ctx.drawText(client.textRenderer, key, bx + 3, by + 3, 0x00F0FF, false);
        ctx.drawTextWithShadow(client.textRenderer, label, bx + keyW + 4, by + 3, 0x00F0FF);
        return bx + keyW + 4 + client.textRenderer.getWidth(label);
    }

    private static void renderActiveContractTracker(DrawContext ctx, MinecraftClient client) {
        int x = 14;
        int y = 52;
        String timerSuffix = "";
        if (SandevistanClient.clientDeadlineMs > 0L) {
            long left = SandevistanClient.clientDeadlineMs - System.currentTimeMillis();
            timerSuffix = " §c[" + formatTimer(left) + "]";
        }
        Text fixerText = Text.literal("§b" + formatFixerName(SandevistanClient.clientFixerName) + timerSuffix);
        Text targetName = Text.translatable("target.sandevistan." + SandevistanClient.clientTargetType);
        int curProg = "item".equals(SandevistanClient.clientContractKind)
            ? countRequiredItems(client, SandevistanClient.clientTargetType)
            : SandevistanClient.clientProgress;
        Text goalText = Text.translatable("call.sandevistan.progress", targetName, curProg, SandevistanClient.clientGoal, SandevistanClient.clientReward);

        int boxW = Math.max(152, Math.max(client.textRenderer.getWidth(fixerText), client.textRenderer.getWidth(goalText)) + 14);
        ctx.fill(x, y, x + boxW, y + 32, 0xBB14080C);
        ctx.fill(x, y, x + 2, y + 32, 0xFF00F0FF);
        ctx.drawText(client.textRenderer, fixerText, x + 6, y + 4, 0x00F0FF, false);
        ctx.drawText(client.textRenderer, goalText, x + 6, y + 17, 0xFCEE09, false);
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

            Text headerT = Text.translatable("scanner.sandevistan.data");
            Text targetT = Text.literal("§f" + targetName);
            Text factionT = Text.translatable("scanner.sandevistan.faction", faction);
            Text statsT = Text.translatable("scanner.sandevistan.stats", dist, hp, maxHp);
            int maxLineW = Math.max(
                Math.max(client.textRenderer.getWidth(headerT), client.textRenderer.getWidth(targetT)),
                Math.max(client.textRenderer.getWidth(aggression), Math.max(client.textRenderer.getWidth(factionT), client.textRenderer.getWidth(statsT)))
            );

            int cardX = cx + 36;
            int cardY = cy - 42;
            int cardW = Math.max(158, maxLineW + 14);
            int cardH = 84;

            ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xDD14080C);
            ctx.fill(cardX, cardY, cardX + cardW, cardY + 2, 0xFF00F0FF);
            ctx.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFFFF2A55);
            ctx.fill(cardX, cardY, cardX + 2, cardY + cardH, 0xFFFF2A55);

            ctx.drawText(client.textRenderer, headerT, cardX + 6, cardY + 6, 0x00F0FF, false);
            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.target"), cardX + 6, cardY + 19, 0xFF2A55, false);
            ctx.drawText(client.textRenderer, targetT, cardX + 6, cardY + 28, 0xFFFFFF, false);

            ctx.drawText(client.textRenderer, Text.translatable("scanner.sandevistan.threat"), cardX + 6, cardY + 41, 0xFF2A55, false);
            ctx.drawText(client.textRenderer, aggression, cardX + 6, cardY + 50, 0xFFFFFF, false);

            ctx.drawText(client.textRenderer, factionT, cardX + 6, cardY + 63, 0xFCEE09, false);
            ctx.drawText(client.textRenderer, statsT, cardX + 6, cardY + 73, 0xA0AEC0, false);
        }
    }

    private static Text getAggressionLabel(LivingEntity entity) {
        if (entity instanceof HostileEntity) return Text.translatable("threat.sandevistan.hostile");
        if (entity instanceof Angerable || entity instanceof GolemEntity || entity instanceof PlayerEntity || entity instanceof RipperdocEntity) return Text.translatable("threat.sandevistan.neutral");
        return Text.translatable("threat.sandevistan.peaceful");
    }

    private static Text getFactionLabel(LivingEntity entity) {
        if (entity instanceof RipperdocEntity) return Text.translatable("faction.sandevistan.ripperdocs");
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
