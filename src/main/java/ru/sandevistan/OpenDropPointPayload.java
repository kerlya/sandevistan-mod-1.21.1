package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenDropPointPayload() implements CustomPayload {
    public static final Id<OpenDropPointPayload> ID = new Id<>(Identifier.of("sandevistan", "open_drop_point"));
    public static final PacketCodec<RegistryByteBuf, OpenDropPointPayload> CODEC = PacketCodec.unit(new OpenDropPointPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
