package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ReloadWeaponPayload() implements CustomPayload {
    public static final Id<ReloadWeaponPayload> ID = new Id<>(Identifier.of("sandevistan", "reload_weapon"));
    public static final PacketCodec<RegistryByteBuf, ReloadWeaponPayload> CODEC = PacketCodec.unit(new ReloadWeaponPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
