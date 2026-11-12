package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncPhonePayload(
    long eddies,
    String historyJoined,
    int callState,
    String fixerName,
    String targetType,
    int progress,
    int goal,
    long reward
) implements CustomPayload {
    public static final Id<SyncPhonePayload> ID = new Id<>(Identifier.of("sandevistan", "sync_phone"));
    public static final PacketCodec<RegistryByteBuf, SyncPhonePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG, SyncPhonePayload::eddies,
        PacketCodecs.STRING, SyncPhonePayload::historyJoined,
        PacketCodecs.VAR_INT, SyncPhonePayload::callState,
        PacketCodecs.STRING, SyncPhonePayload::fixerName,
        PacketCodecs.STRING, SyncPhonePayload::targetType,
        PacketCodecs.VAR_INT, SyncPhonePayload::progress,
        PacketCodecs.VAR_INT, SyncPhonePayload::goal,
        PacketCodecs.VAR_LONG, SyncPhonePayload::reward,
        SyncPhonePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
