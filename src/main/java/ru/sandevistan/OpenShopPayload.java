package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenShopPayload() implements CustomPayload {
    public static final Id<OpenShopPayload> ID = new Id<>(Identifier.of("sandevistan", "open_shop"));
    public static final PacketCodec<RegistryByteBuf, OpenShopPayload> CODEC = PacketCodec.unit(new OpenShopPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
