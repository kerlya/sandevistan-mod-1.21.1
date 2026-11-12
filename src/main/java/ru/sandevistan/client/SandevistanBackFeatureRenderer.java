package ru.sandevistan.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import ru.sandevistan.SandevistanMod;

public class SandevistanBackFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final ItemStack DISPLAY_STACK = new ItemStack(SandevistanMod.SANDEVISTAN);

    public SandevistanBackFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible() || !SandevistanClient.hasBackImplant(player.getId())) return;

        matrices.push();
        // Привязываем имплант к телу игрока (повторяет наклон при приседании)
        this.getContextModel().body.rotate(matrices);

        // Смещаем на заднюю поверхность спины вдоль позвоночника
        matrices.translate(0.0F, 0.34F, 0.145F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(0.68F, 0.78F, 0.68F);

        // Рисуем 3D-модель предмета на спине (использует вашу новую текстуру sandevistan.png!)
        MinecraftClient.getInstance().getItemRenderer().renderItem(
            DISPLAY_STACK,
            ModelTransformationMode.FIXED,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            player.getWorld(),
            player.getId()
        );

        matrices.pop();
    }
}
