package ru.sandevistan.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.sandevistan.ActivatePayload;

public final class SandevistanClient implements ClientModInitializer {
    private KeyBinding key;
    @Override public void onInitializeClient() {
        SandevistanHud.register();
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.sandevistan.activate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.sandevistan"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.wasPressed()) {
                if (client.player != null && client.getNetworkHandler() != null)
                    ClientPlayNetworking.send(new ActivatePayload());
            }
        });
    }
}
