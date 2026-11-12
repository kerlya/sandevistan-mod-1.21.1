package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CraftImplantPayload(String recipeId) implements CustomPayload {
    public static final Id<CraftImplantPayload> ID = new Id<>(Identifier.of("sandevistan", "craft_implant"));
    public static final PacketCodec<RegistryByteBuf, CraftImplantPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, CraftImplantPayload::recipeId,
        CraftImplantPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
