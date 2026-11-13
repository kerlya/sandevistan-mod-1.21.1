package ru.sandevistan.client;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;
import ru.sandevistan.ScavengerEntity;

public class ScavengerEntityRenderer extends BipedEntityRenderer<ScavengerEntity, BipedEntityModel<ScavengerEntity>> {
    private static final Identifier BRUISER_TEX = Identifier.of("sandevistan", "textures/entity/scavenger_bruiser.png");
    private static final Identifier GUNNER_TEX = Identifier.of("sandevistan", "textures/entity/scavenger_gunner.png");

    public ScavengerEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
        this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(ScavengerEntity entity) {
        return entity.isGunner() ? GUNNER_TEX : BRUISER_TEX;
    }
}
