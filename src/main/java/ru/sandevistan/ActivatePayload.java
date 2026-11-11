package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ActivatePayload() implements CustomPayload {
    public static final Id<ActivatePayload> ID = new Id<>(Identifier.of("sandevistan", "activate"));
    public static final PacketCodec<RegistryByteBuf, ActivatePayload> CODEC = PacketCodec.unit(new ActivatePayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
