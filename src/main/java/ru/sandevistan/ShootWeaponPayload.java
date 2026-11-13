package ru.sandevistan;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShootWeaponPayload(boolean aiming) implements CustomPayload {
    public static final Id<ShootWeaponPayload> ID = new Id<>(Identifier.of("sandevistan", "shoot_weapon"));
    public static final PacketCodec<RegistryByteBuf, ShootWeaponPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOL, ShootWeaponPayload::aiming,
        ShootWeaponPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
