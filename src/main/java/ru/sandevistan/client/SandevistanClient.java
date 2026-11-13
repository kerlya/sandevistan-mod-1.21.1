package ru.sandevistan.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.sandevistan.*;
import java.util.HashSet;
import java.util.Set;

public final class SandevistanClient implements ClientModInitializer {
    public static KeyBinding activateKey;
    public static KeyBinding menuKey;
    public static KeyBinding scanKey;
    public static KeyBinding phoneKey;
    public static KeyBinding acceptCallKey;
    public static KeyBinding rejectCallKey;
    public static KeyBinding reloadKey;

    public static long lastActivateMs = 0L;
    public static long lastReloadSendMs = 0L;
    public static long lastClientShotMs = 0L;
    public static float adsProgress = 0.0f;

    public static boolean clientHasSandevistan = false;
    public static boolean clientHasKiroshi = false;
    public static boolean clientHasMetalArms = false;
    public static int clientCyberLevel = 1;
    public static int clientCyberXp = 0;
    public static int clientCyberNextXp = 100;
    private static final Set<Integer> PLAYERS_WITH_SANDEVISTAN = new HashSet<>();

    public static long clientEddies = 250L;
    public static String clientBankHistory = "";
    public static String clientSmsHistory = "";
    public static boolean clientHasUnreadSms = false;
    public static int clientCallState = 0;
    public static String clientFixerName = "";
    public static String clientContractKind = "kill";
    public static String clientTargetType = "";
    public static int clientProgress = 0;
    public static int clientGoal = 0;
    public static long clientReward = 0L;
    public static long clientDeadlineMs = 0L;

    public static String getKeyLabel(KeyBinding key, String fallback) {
        if (key == null) return fallback;
        return key.getBoundKeyLocalizedText().getString().toUpperCase();
    }

    public static boolean isEquipped(MinecraftClient client) {
        return client.player != null && clientHasSandevistan;
    }

    public static boolean hasBackImplant(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getId() == entityId) return clientHasSandevistan;
        return PLAYERS_WITH_SANDEVISTAN.contains(entityId);
    }

    public static boolean isScanning() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.currentScreen == null && clientHasKiroshi && scanKey != null && scanKey.isPressed();
    }
    @Override
    public void onInitializeClient() {
        HandledScreens.register(SandevistanMod.CYBERWARE_HANDLER, CyberwareScreen::new);
        EntityRendererRegistry.register(SandevistanMod.RIPPERDOC_ENTITY, RipperdocEntityRenderer::new);
        EntityRendererRegistry.register(SandevistanMod.SCAVENGER_ENTITY, ScavengerEntityRenderer::new);
        SandevistanHud.register();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof CraftingScreen) {
                ScreenEvents.afterRender(screen).register((scr, ctx, mouseX, mouseY, delta) -> {
                    int bx = (scaledWidth - 176) / 2;
                    int by = (scaledHeight - 166) / 2 - 20;
                    Text tabLabel = Text.translatable("craft.sandevistan.tab");
                    int bw = Math.max(114, client.textRenderer.getWidth(tabLabel) + 14);
                    boolean hover = mouseX >= bx && !(mouseX > bx + bw) && mouseY >= by && !(mouseY > by + 18);

                    int bg = hover ? 0xFF3A101E : 0xEE18080D;
                    int border = hover ? 0xFF00F0FF : 0xFFFF2A55;
                    ctx.fill(bx, by, bx + bw, by + 18, bg);
                    ctx.fill(bx, by, bx + bw, by + 1, border);
                    ctx.fill(bx, by + 17, bx + bw, by + 18, border);
                    ctx.fill(bx, by, bx + 1, by + 18, border);
                    ctx.fill(bx + bw - 1, by, bx + bw, by + 18, border);
                    ctx.drawText(client.textRenderer, tabLabel, bx + 7, by + 5, hover ? 0x00F0FF : 0xFFFFFF, false);
                });

                ScreenMouseEvents.afterMouseClick(screen).register((scr, mouseX, mouseY, button) -> {
                    if (button == 0) {
                        int bx = (scaledWidth - 176) / 2;
                        int by = (scaledHeight - 166) / 2 - 20;
                        Text tabLabel = Text.translatable("craft.sandevistan.tab");
                        int bw = Math.max(114, client.textRenderer.getWidth(tabLabel) + 14);
                        if (mouseX >= bx && !(mouseX > bx + bw) && mouseY >= by && !(mouseY > by + 18)) {
                            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
                            client.setScreen(new CyberCraftScreen(scr));
                        }
                    }
                });
            }
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new SandevistanBackFeatureRenderer(playerRenderer));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PLAYERS_WITH_SANDEVISTAN.clear();
            clientCallState = 0;
            clientHasUnreadSms = false;
            adsProgress = 0.0f;
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncCyberwarePayload.ID,
            (payload, context) -> context.client().execute(() -> {
                clientHasSandevistan = payload.hasSandevistan();
                clientHasKiroshi = payload.hasKiroshi();
                clientHasMetalArms = payload.hasMetalArms();
                clientCyberLevel = payload.cyberLevel();
                clientCyberXp = payload.cyberXp();
                clientCyberNextXp = payload.cyberNextXp();
            }));

        ClientPlayNetworking.registerGlobalReceiver(SyncBackVisualPayload.ID,
            (payload, context) -> context.client().execute(() -> {
                if (payload.equipped()) PLAYERS_WITH_SANDEVISTAN.add(payload.entityId());
                else PLAYERS_WITH_SANDEVISTAN.remove(payload.entityId());
            }));

        ClientPlayNetworking.registerGlobalReceiver(SyncPhonePayload.ID,
            (payload, context) -> context.client().execute(() -> {
                clientEddies = payload.eddies();
                clientBankHistory = payload.historyJoined();
                clientSmsHistory = payload.smsJoined();
                clientHasUnreadSms = payload.hasUnreadSms();
                clientCallState = payload.callState();
                clientFixerName = payload.fixerName();
                clientContractKind = payload.kind();
                clientTargetType = payload.targetType();
                clientProgress = payload.progress();
                clientGoal = payload.goal();
                clientReward = payload.reward();
                clientDeadlineMs = payload.deadlineMs();
            }));

        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.ID,
            (payload, context) -> context.client().execute(() ->
                context.client().setScreen(new RipperdocShopScreen())));

        ClientPlayNetworking.registerGlobalReceiver(OpenDropPointPayload.ID,
            (payload, context) -> context.client().execute(() ->
                context.client().setScreen(new DropPointScreen())));

        activateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.activate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.sandevistan"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.sandevistan"));
        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.scan", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.sandevistan"));
        phoneKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.phone", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.sandevistan"));
        acceptCallKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.accept", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.sandevistan"));
        rejectCallKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.reject", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.sandevistan"));
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.sandevistan"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null) ClientPlayNetworking.send(new OpenCyberwarePayload());
            }
            while (phoneKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) client.setScreen(new PhoneScreen());
            }
            while (acceptCallKey.wasPressed()) {
                if (client.player != null && clientCallState == 1) ClientPlayNetworking.send(new PhoneActionPayload(1, "", "", 0L));
            }
            while (rejectCallKey.wasPressed()) {
                if (client.player != null && clientCallState == 1) ClientPlayNetworking.send(new PhoneActionPayload(2, "", "", 0L));
            }

            if (client.player != null && client.currentScreen == null) {
                ItemStack main = client.player.getMainHandStack();
                if (main.isOf(SandevistanMod.SARATOGA_M221)) {
                    client.player.handSwinging = false;
                    client.player.handSwingTicks = 0;
                    client.player.handSwingProgress = 0.0f;
                    client.player.lastHandSwingProgress = 0.0f;

                    boolean reloading = client.player.getItemCooldownManager().isCoolingDown(SandevistanMod.SARATOGA_M221);
                    boolean aimingNow = !reloading && client.options.useKey.isPressed();
                    if (aimingNow) {
                        adsProgress = Math.min(1.0f, adsProgress + 0.25f);
                    } else {
                        adsProgress = Math.max(0.0f, adsProgress - 0.25f);
                    }

                    if (client.options.attackKey.isPressed() && !reloading) {
                        long now = System.currentTimeMillis();
                        long elapsedSinceActivate = now - lastActivateMs;
                        boolean sandyActive = isEquipped(client) && lastActivateMs > 0L && elapsedSinceActivate != 8000L && 8000L - elapsedSinceActivate > 0L;
                        long intervalMs = sandyActive ? 140L : 190L;

                        if (now - lastClientShotMs >= intervalMs) {
                            lastClientShotMs = now;
                            ClientPlayNetworking.send(new ShootWeaponPayload(aimingNow));

                            if (SaratogaItem.getAmmo(main) > 0) {
                                float recoilPitch = aimingNow ? 0.45f : 0.85f;
                                float recoilYaw = (client.player.getRandom().nextFloat() - 0.5f) * (aimingNow ? 0.35f : 0.7f);
                                client.player.setPitch(Math.max(-90.0f, client.player.getPitch() - recoilPitch));
                                client.player.setYaw(client.player.getYaw() + recoilYaw);
                            }
                        }
                    }
                } else {
                    adsProgress = Math.max(0.0f, adsProgress - 0.25f);
                }
            } else {
                adsProgress = 0.0f;
            }

            boolean reloadPressed = false;
            while (reloadKey.wasPressed()) reloadPressed = true;
            if ((reloadPressed || reloadKey.isPressed()) && client.player != null && client.currentScreen == null && client.player.getMainHandStack().isOf(SandevistanMod.SARATOGA_M221)) {
                long now = System.currentTimeMillis();
                if (now - lastReloadSendMs >= 500L) {
                    lastReloadSendMs = now;
                    if (client.player.isUsingItem()) client.player.stopUsingItem();
                    ClientPlayNetworking.send(new ReloadWeaponPayload());
                }
            }
            while (activateKey.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null && isEquipped(client)) {
                    long now = System.currentTimeMillis();
                    if (now - lastActivateMs >= 30000L) {
                        lastActivateMs = now;
                        ClientPlayNetworking.send(new ActivatePayload());
                    }
                }
            }
        });
    }
}
