package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerProfession;

public final class RipperdocManager {
    public static final String RIPPERDOC_TAG = "sandevistan_ripperdoc";

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) {
                if (entity.getCommandTags().contains(RIPPERDOC_TAG)) {
                    FixerContractManager.syncPlayer(sp);
                    ServerPlayNetworking.send(sp, new OpenShopPayload());
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ripperdoc")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("spawn")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    VillagerEntity doc = EntityType.VILLAGER.create(p.getServerWorld());
                    if (doc != null) {
                        doc.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), 0.0f);
                        doc.setVillagerData(doc.getVillagerData().withProfession(VillagerProfession.CLERIC).withLevel(5));
                        doc.setCustomName(Text.literal("§b[Рипердок] §fВиктор Вектор"));
                        doc.setCustomNameVisible(true);
                        doc.setInvulnerable(true);
                        doc.setPersistent();
                        doc.addCommandTag(RIPPERDOC_TAG);
                        p.getServerWorld().spawnEntity(doc);
                        p.sendMessage(Text.translatable("msg.sandevistan.ripperdoc.spawned"), false);
                    }
                    return 1;
                }))
            .then(CommandManager.literal("remove")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    int removed = 0;
                    for (VillagerEntity v : p.getServerWorld().getEntitiesByClass(VillagerEntity.class, p.getBoundingBox().expand(32.0), e -> e.getCommandTags().contains(RIPPERDOC_TAG))) {
                        v.discard();
                        removed++;
                    }
                    p.sendMessage(Text.translatable("msg.sandevistan.ripperdoc.removed", removed), false);
                    return 1;
                })));
    }
    public static void handleBuy(ServerPlayerEntity p, String implantId) {
        if (p.getServer() == null) return;
        Item item;
        long price;
        String logLabel;

        switch (implantId) {
            case "kiroshi_mk1" -> {
                item = SandevistanMod.KIROSHI_MK1;
                price = 2000L;
                logLabel = "Kiroshi Mk.1";
            }
            case "sandevistan" -> {
                item = SandevistanMod.SANDEVISTAN;
                price = 7000L;
                logLabel = "Sandevistan";
            }
            default -> {
                return;
            }
        }

        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        if (!storage.spendEddies(p.getUuid(), price, logLabel)) {
            p.sendMessage(Text.translatable("msg.sandevistan.bank.no_funds"), true);
            p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.8f, 0.7f);
            return;
        }

        ItemStack stack = new ItemStack(item);
        if (!p.getInventory().insertStack(stack)) {
            p.dropItem(stack, false);
        }

        p.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.3f);
        p.sendMessage(Text.translatable("msg.sandevistan.shop.bought", item.getName(), price), false);
        FixerContractManager.syncPlayer(p);
    }
}
