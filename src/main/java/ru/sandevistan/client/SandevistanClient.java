package ru.sandevistan.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.sandevistan.ActivatePayload;
import ru.sandevistan.OpenCyberwarePayload;
import ru.sandevistan.SandevistanMod;
import ru.sandevistan.SyncCyberwarePayload;

public final class SandevistanClient implements ClientModInitializer {
    private KeyBinding activateKey;
    private KeyBinding menuKey;
    private static KeyBinding scanKey;
    public static long lastActivateMs = 0L;
    public static boolean clientHasSandevistan = false;
    public static boolean clientHasKiroshi = false;

    public static boolean isEquipped(MinecraftClient client) {
        return client.player != null && clientHasSandevistan;
    }

    public static boolean isScanning() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.currentScreen == null && clientHasKiroshi && scanKey != null && scanKey.isPressed();
    }

    @Override
    public void onInitializeClient() {
        HandledScreens.register(SandevistanMod.CYBERWARE_HANDLER, CyberwareScreen::new);
        SandevistanHud.register();

        ClientPlayNetworking.registerGlobalReceiver(SyncCyberwarePayload.ID,
            (payload, context) -> context.client().execute(() -> {
                clientHasSandevistan = payload.hasSandevistan();
                clientHasKiroshi = payload.hasKiroshi();
            }));

        activateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.sandevistan.activate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.sandevistan"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.sandevistan.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.sandevistan"));
        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.sandevistan.scan", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.sandevistan"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(new OpenCyberwarePayload());
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
