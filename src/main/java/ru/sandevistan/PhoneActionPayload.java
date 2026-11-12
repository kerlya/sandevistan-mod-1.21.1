package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PhoneActionPayload(int action, String targetPlayer, String textData, long amount) implements CustomPayload {
    public static final Id<PhoneActionPayload> ID = new Id<>(Identifier.of("sandevistan", "phone_action"));
    public static final PacketCodec<RegistryByteBuf, PhoneActionPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, PhoneActionPayload::action,
        PacketCodecs.STRING, PhoneActionPayload::targetPlayer,
        PacketCodecs.STRING, PhoneActionPayload::textData,
        PacketCodecs.VAR_LONG, PhoneActionPayload::amount,
        PhoneActionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
