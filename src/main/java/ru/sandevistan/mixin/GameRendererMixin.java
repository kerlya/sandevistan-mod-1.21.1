package ru.sandevistan.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.sandevistan.client.SandevistanClient;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void sandevistan$kiroshiAndAdsZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double fov = cir.getReturnValue();
        if (SandevistanClient.isScanning()) {
            fov *= 0.4;
        }
        if (SandevistanClient.adsProgress > 0.001f) {
            fov *= (1.0 - 0.30 * SandevistanClient.adsProgress);
        }
        cir.setReturnValue(fov);
    }
}
