package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncCyberwarePayload(boolean hasSandevistan, boolean hasKiroshi) implements CustomPayload {
    public static final Id<SyncCyberwarePayload> ID = new Id<>(Identifier.of("sandevistan", "sync_cyberware"));
    public static final PacketCodec<RegistryByteBuf, SyncCyberwarePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOL, SyncCyberwarePayload::hasSandevistan,
        PacketCodecs.BOOL, SyncCyberwarePayload::hasKiroshi,
        SyncCyberwarePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
