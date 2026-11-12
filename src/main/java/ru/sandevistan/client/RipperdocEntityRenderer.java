package ru.sandevistan.client;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import ru.sandevistan.RipperdocEntity;

public class RipperdocEntityRenderer extends BipedEntityRenderer<RipperdocEntity, PlayerEntityModel<RipperdocEntity>> {
    private static final Identifier TEXTURE = Identifier.of("sandevistan", "textures/entity/ripperdoc.png");

    public RipperdocEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(RipperdocEntity entity) {
        return TEXTURE;
    }
}
