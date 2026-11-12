package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncBackVisualPayload(int entityId, boolean equipped) implements CustomPayload {
    public static final Id<SyncBackVisualPayload> ID = new Id<>(Identifier.of("sandevistan", "sync_back_visual"));
    public static final PacketCodec<RegistryByteBuf, SyncBackVisualPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, SyncBackVisualPayload::entityId,
        PacketCodecs.BOOL, SyncBackVisualPayload::equipped,
        SyncBackVisualPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
