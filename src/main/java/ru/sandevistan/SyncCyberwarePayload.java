package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncCyberwarePayload(
    boolean hasSandevistan,
    boolean hasKiroshi,
    boolean hasMetalArms,
    int cyberLevel,
    int cyberXp,
    int cyberNextXp
) implements CustomPayload {
    public static final Id<SyncCyberwarePayload> ID = new Id<>(Identifier.of("sandevistan", "sync_cyberware"));
    public static final PacketCodec<RegistryByteBuf, SyncCyberwarePayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeBoolean(value.hasSandevistan);
            buf.writeBoolean(value.hasKiroshi);
            buf.writeBoolean(value.hasMetalArms);
            buf.writeVarInt(value.cyberLevel);
            buf.writeVarInt(value.cyberXp);
            buf.writeVarInt(value.cyberNextXp);
        },
        buf -> new SyncCyberwarePayload(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
