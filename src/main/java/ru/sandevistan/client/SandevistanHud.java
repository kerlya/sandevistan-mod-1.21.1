package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.sandevistan.RipperdocEntity;
import ru.sandevistan.SandevistanMod;
import ru.sandevistan.SaratogaItem;
import ru.sandevistan.ScavengerEntity;

public final class SandevistanHud {
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");

    public static void register() {
        HudRenderCallback.EVENT.register(SandevistanHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        TextRenderer tr = client.textRenderer;
        long now = System.currentTimeMillis();

        renderPhoneAndCallHud(context, tr, sw, sh, now);
        renderWeaponHud(context, tr, client, sw, sh);

        if (SandevistanClient.isEquipped(client)) {
            long activeLeft = (SandevistanClient.lastActivateMs + 8000L) - now;
            long cdLeft = (SandevistanClient.lastActivateMs + 30000L) - now;

            Text text;
            int color;
            if (SandevistanClient.lastActivateMs > 0L && activeLeft > 0) {
                text = Text.translatable("hud.sandevistan.active", String.format("%.1f", activeLeft / 1000.0));
                color = 0xC8E61E;
            } else if (SandevistanClient.lastActivateMs > 0L && cdLeft > 0) {
                text = Text.translatable("hud.sandevistan.cooldown", (cdLeft / 1000) + 1);
                color = 0xFF5555;
            } else {
                String keyName = SandevistanClient.getKeyLabel(SandevistanClient.activateKey, "R");
                text = Text.translatable("hud.sandevistan.ready", keyName);
                color = 0x55FF55;
            }
            context.drawTextWithShadow(tr, text, 10, sh - 22, color);
        }

        if (SandevistanClient.isScanning()) {
            renderKiroshiScanner(context, client, sw, sh);
        }
    }
    private static void renderWeaponHud(DrawContext ctx, TextRenderer tr, MinecraftClient client, int sw, int sh) {
        ItemStack main = client.player.getMainHandStack();
        if (!main.isOf(SandevistanMod.SARATOGA_M221)) return;

        int ammo = SaratogaItem.getAmmo(main);
        int reserve = 0;
        for (int i = 0; i != client.player.getInventory().size(); i++) {
            ItemStack st = client.player.getInventory().getStack(i);
            if (st.isOf(SandevistanMod.SMG_AMMO)) {
                reserve += st.getCount();
            }
        }

        boolean reloading = client.player.getItemCooldownManager().isCoolingDown(SandevistanMod.SARATOGA_M221);
        int boxW = 114;
        int boxH = 26;
        int x = sw - boxW - 10;
        int y = sh - boxH - 10;

        ctx.fill(x, y, x + boxW, y + boxH, 0xCC12060B);
        ctx.fill(x, y, x + boxW, y + 1, 0xFFFF2A55);
        ctx.fill(x, y + boxH - 1, x + boxW, y + boxH, 0xFF00F0FF);
        ctx.fill(x, y, x + 1, y + boxH, 0xFFFF2A55);
        ctx.fill(x + boxW - 1, y, x + boxW, y + boxH, 0xFFFF2A55);

        if (reloading) {
            ctx.drawTextWithShadow(tr, Text.translatable("hud.sandevistan.reloading"), x + 8, y + 9, 0xFFCC00);
        } else {
            String ammoStr = String.format("%02d", ammo);
            int ammoColor = ammo <= 6 ? 0xFF3B3B : 0x00F0FF;
            ctx.drawTextWithShadow(tr, "M221 // " + ammoStr, x + 8, y + 5, ammoColor);
            ctx.drawTextWithShadow(tr, "9MM: " + reserve, x + 8, y + 15, 0xAAAAAA);
        }
    }

    private static void renderPhoneAndCallHud(DrawContext ctx, TextRenderer tr, int sw, int sh, long now) {
        int px = 10;
        int py = sh - 44;
        String phoneKeyName = SandevistanClient.getKeyLabel(SandevistanClient.phoneKey, "Z");
        String phoneLabel = "§b[" + phoneKeyName + "] §e€$" + SandevistanClient.clientEddies;
        if (SandevistanClient.clientHasUnreadSms) {
            phoneLabel += " §c§l[SMS!]";
        }
        ctx.drawTextWithShadow(tr, phoneLabel, px, py, 0xFFFFFF);

        if (SandevistanClient.clientCallState == 1) {
            int bw = 188;
            int bh = 44;
            int bx = sw - bw - 10;
            int by = 10;
            ctx.fill(bx, by, bx + bw, by + bh, 0xDD1A050B);
            ctx.fill(bx, by, bx + bw, by + 2, 0xFFFF2A55);
            ctx.fill(bx, by + bh - 2, bx + bw, by + bh, 0xFFFF2A55);

            ctx.drawTextWithShadow(tr, Text.translatable("call.sandevistan.incoming"), bx + 6, by + 5, 0xFF2A55);
            ctx.drawTextWithShadow(tr, "§f" + SandevistanClient.clientFixerName, bx + 6, by + 15, 0xFFFFFF);
            Text targetName = Text.translatable(SandevistanClient.clientTargetType);
            ctx.drawTextWithShadow(tr, Text.translatable("call.sandevistan.desc", targetName, SandevistanClient.clientGoal, SandevistanClient.clientReward), bx + 6, by + 25, 0xFFE600);

            String ansKey = SandevistanClient.getKeyLabel(SandevistanClient.acceptCallKey, "Y");
            String rejKey = SandevistanClient.getKeyLabel(SandevistanClient.rejectCallKey, "N");
            Text ansBtn = Text.literal("[" + ansKey + "] ").append(Text.translatable("call.sandevistan.answer"));
            Text rejBtn = Text.literal("[" + rejKey + "] ").append(Text.translatable("call.sandevistan.reject"));
            ctx.drawTextWithShadow(tr, ansBtn, bx + 6, by + 35, 0x55FF55);
            ctx.drawTextWithShadow(tr, rejBtn, bx + 100, by + 35, 0xFF5555);
        } else if (SandevistanClient.clientCallState == 2) {
            int bw = 188;
            int bh = 32;
            int bx = sw - bw - 10;
            int by = 10;
            ctx.fill(bx, by, bx + bw, by + bh, 0xCC061820);
            ctx.fill(bx, by, bx + bw, by + 2, 0xFF00F0FF);

            long secLeft = Math.max(0L, (SandevistanClient.clientDeadlineMs - now) / 1000L);
            String timerStr = SandevistanClient.clientDeadlineMs > 0 ? " §c[" + secLeft + "s]" : "";
            ctx.drawTextWithShadow(tr, "§b" + SandevistanClient.clientFixerName + timerStr, bx + 6, by + 5, 0x00F0FF);
            Text targetName = Text.translatable(SandevistanClient.clientTargetType);
            ctx.drawTextWithShadow(tr, Text.translatable("call.sandevistan.progress", targetName, SandevistanClient.clientProgress, SandevistanClient.clientGoal, SandevistanClient.clientReward), bx + 6, by + 17, 0xFFFFFF);
        }
    }
    private static void renderKiroshiScanner(DrawContext ctx, MinecraftClient client, int sw, int sh) {
        TextRenderer tr = client.textRenderer;
        int cyan = 0xFF00E5FF;
        int darkCyan = 0x5500E5FF;
        int panelBg = 0xAA04141C;

        ctx.fill(0, 0, sw, 3, cyan);
        ctx.fill(0, sh - 3, sw, sh, cyan);
        ctx.fill(0, 0, 3, sh, cyan);
        ctx.fill(sw - 3, 0, sw, sh, cyan);

        int cx = sw / 2;
        int cy = sh / 2;
        ctx.fill(cx - 24, cy, cx - 8, cy + 1, cyan);
        ctx.fill(cx + 9, cy, cx + 25, cy + 1, cyan);
        ctx.fill(cx, cy - 24, cx + 1, cy - 8, cyan);
        ctx.fill(cx, cy + 9, cx + 1, cy + 25, cyan);

        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.header"), 14, 14, 0x00E5FF);

        LivingEntity target = raycastLivingEntity(client, 48.0);
        if (target == null) return;

        int boxW = 195;
        int boxH = 88;
        int bx = sw - boxW - 14;
        int by = cy - boxH / 2;

        ctx.fill(bx, by, bx + boxW, by + boxH, panelBg);
        ctx.fill(bx, by, bx + boxW, by + 2, cyan);
        ctx.fill(bx, by + boxH - 2, bx + boxW, by + boxH, cyan);
        ctx.fill(bx, by, bx + 2, by + boxH, cyan);
        ctx.fill(bx + boxW - 2, by, bx + boxW, by + boxH, cyan);
        ctx.fill(bx + 4, by + 16, bx + boxW - 4, by + 17, darkCyan);

        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.data"), bx + 8, by + 6, 0x00E5FF);

        Text name = target.getDisplayName();
        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.target"), bx + 8, by + 21, 0xFF4444);
        ctx.drawTextWithShadow(tr, name, bx + 50, by + 21, 0xFFFFFF);

        Text threat = getThreatStatus(target);
        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.threat"), bx + 8, by + 33, 0xFF4444);
        ctx.drawTextWithShadow(tr, threat, bx + 8, by + 43, 0xFFFFFF);

        Text faction = getFactionName(target);
        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.faction", faction), bx + 8, by + 56, 0xFF4444);

        int hp = (int) Math.ceil(target.getHealth());
        int maxHp = (int) Math.ceil(target.getMaxHealth());
        int dist = (int) Math.round(client.player.distanceTo(target));
        ctx.drawTextWithShadow(tr, Text.translatable("scanner.sandevistan.stats", dist, hp, maxHp), bx + 8, by + 67, 0xAAAAAA);

        int totalHearts = Math.min(10, (maxHp + 1) / 2);
        int fullHearts = hp / 2;
        boolean halfHeart = (hp % 2) == 1;
        for (int i = 0; i != totalHearts; i++) {
            int hx = bx + 8 + i * 8;
            int hy = by + 77;
            ctx.drawGuiTexture(HEART_CONTAINER, hx, hy, 9, 9);
            if (i != fullHearts && fullHearts - i > 0) ctx.drawGuiTexture(HEART_FULL, hx, hy, 9, 9);
            else if (i == fullHearts && halfHeart) ctx.drawGuiTexture(HEART_HALF, hx, hy, 9, 9);
        }
    }
    public static String formatFixerName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] parts = raw.split("\\|");
        if (parts.length == 3) {
            return Text.translatable(parts[0]).getString() + " " + Text.translatable(parts[1]).getString() + " " + Text.translatable(parts[2]).getString();
        }
        return raw;
    }

    public static int countRequiredItems(MinecraftClient client, String targetKey) {
        if (client == null || client.player == null || targetKey == null) return 0;
        String itemName = targetKey.replace("target.sandevistan.", "");
        Identifier id = Identifier.ofVanilla(itemName);
        Item targetItem = Registries.ITEM.get(id);
        int total = 0;
        for (int i = 0; i != client.player.getInventory().size(); i++) {
            ItemStack st = client.player.getInventory().getStack(i);
            if (st.isOf(targetItem)) total += st.getCount();
        }
        return total;
    }

    private static Text getThreatStatus(LivingEntity entity) {
        if (entity instanceof RipperdocEntity) return Text.translatable("threat.sandevistan.peaceful");
        if (entity instanceof ScavengerEntity) return Text.translatable("threat.sandevistan.hostile");
        if (entity instanceof HostileEntity) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) {
                return Text.translatable("threat.sandevistan.neutral");
            }
            if (entity instanceof PiglinEntity piglin && !piglin.isAttacking()) {
                return Text.translatable("threat.sandevistan.neutral");
            }
            return Text.translatable("threat.sandevistan.hostile");
        }
        if (entity instanceof Angerable) return Text.translatable("threat.sandevistan.neutral");
        if (entity instanceof PlayerEntity) return Text.translatable("threat.sandevistan.neutral");
        return Text.translatable("threat.sandevistan.peaceful");
    }
    private static Text getFactionName(LivingEntity entity) {
        if (entity instanceof RipperdocEntity) return Text.translatable("faction.sandevistan.ripperdocs");
        if (entity instanceof ScavengerEntity) return Text.translatable("faction.sandevistan.scavengers");
        if (entity instanceof PlayerEntity) return Text.translatable("faction.sandevistan.players");
        if (entity instanceof MerchantEntity || entity instanceof GolemEntity) return Text.translatable("faction.sandevistan.civilians");
        if (entity instanceof RaiderEntity) return Text.translatable("faction.sandevistan.illagers");
        String path = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        if (path.contains("zombie") || path.contains("skeleton") || path.contains("wither") || path.contains("phantom") || path.contains("drowned") || path.contains("husk") || path.contains("stray")) {
            return Text.translatable("faction.sandevistan.undead");
        }
        if (path.contains("spider") || path.contains("silverfish") || path.contains("endermite") || path.contains("bee")) {
            return Text.translatable("faction.sandevistan.arthropods");
        }
        if (path.contains("piglin") || path.contains("hoglin") || path.contains("ghast") || path.contains("blaze") || path.contains("magma")) {
            return Text.translatable("faction.sandevistan.nether");
        }
        if (path.contains("enderman") || path.contains("shulker") || path.contains("ender_dragon")) {
            return Text.translatable("faction.sandevistan.end");
        }
        if (entity instanceof PassiveEntity) return Text.translatable("faction.sandevistan.fauna");
        return Text.literal(path.toUpperCase());
    }

    private static LivingEntity raycastLivingEntity(MinecraftClient client, double range) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) return null;
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d dir = camera.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(range));

        HitResult blockHit = camera.raycast(range, 1.0f, false);
        double maxDistSq = range * range;
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            maxDistSq = blockHit.getPos().squaredDistanceTo(start);
        }

        Box box = camera.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(camera, start, end, box, e -> !e.isSpectator() && e.canHit() && e instanceof LivingEntity, maxDistSq);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
