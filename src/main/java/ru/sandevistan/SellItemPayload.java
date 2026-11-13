package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SellItemPayload(String itemId, boolean sellAll) implements CustomPayload {
    public static final Id<SellItemPayload> ID = new Id<>(Identifier.of("sandevistan", "sell_item"));
    public static final PacketCodec<RegistryByteBuf, SellItemPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, SellItemPayload::itemId,
        PacketCodecs.BOOL, SellItemPayload::sellAll,
        SellItemPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
