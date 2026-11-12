package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenCyberwarePayload() implements CustomPayload {
    public static final Id<OpenCyberwarePayload> ID = new Id<>(Identifier.of("sandevistan", "open_cyberware"));
    public static final PacketCodec<RegistryByteBuf, OpenCyberwarePayload> CODEC = PacketCodec.unit(new OpenCyberwarePayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
