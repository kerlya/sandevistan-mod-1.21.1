package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuyImplantPayload(String implantId) implements CustomPayload {
    public static final Id<BuyImplantPayload> ID = new Id<>(Identifier.of("sandevistan", "buy_implant"));
    public static final PacketCodec<RegistryByteBuf, BuyImplantPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, BuyImplantPayload::implantId,
        BuyImplantPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
