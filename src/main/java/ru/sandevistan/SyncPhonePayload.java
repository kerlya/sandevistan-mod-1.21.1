package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncPhonePayload(
    long eddies,
    String historyJoined,
    String smsJoined,
    boolean hasUnreadSms,
    int callState,
    String fixerName,
    String kind,
    String targetType,
    int progress,
    int goal,
    long reward,
    long deadlineMs
) implements CustomPayload {
    public static final Id<SyncPhonePayload> ID = new Id<>(Identifier.of("sandevistan", "sync_phone"));
    public static final PacketCodec<RegistryByteBuf, SyncPhonePayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeVarLong(value.eddies);
            buf.writeString(value.historyJoined);
            buf.writeString(value.smsJoined);
            buf.writeBoolean(value.hasUnreadSms);
            buf.writeVarInt(value.callState);
            buf.writeString(value.fixerName);
            buf.writeString(value.kind);
            buf.writeString(value.targetType);
            buf.writeVarInt(value.progress);
            buf.writeVarInt(value.goal);
            buf.writeVarLong(value.reward);
            buf.writeVarLong(value.deadlineMs);
        },
        buf -> new SyncPhonePayload(
            buf.readVarLong(), buf.readString(), buf.readString(), buf.readBoolean(),
            buf.readVarInt(), buf.readString(), buf.readString(), buf.readString(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong()
        )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
