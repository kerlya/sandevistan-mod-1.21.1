package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Random;

public final class RipperdocManager {
    private static final Random RNG = new Random();

    public record ShopOffer(String id, String category, Item item, long price) {}

    public static final List<ShopOffer> OFFERS = List.of(
        new ShopOffer("sandevistan", "os", SandevistanMod.SANDEVISTAN, 1200L),
        new ShopOffer("kiroshi_mk1", "eyes", SandevistanMod.KIROSHI_MK1, 450L)
    );

    public static void register() {
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ripperdoc")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("spawn")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    RipperdocEntity doc = new RipperdocEntity(SandevistanMod.RIPPERDOC_ENTITY, p.getServerWorld());
                    doc.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), 0.0f);

                    int nameIdx = RNG.nextInt(8);
                    Text docName = Text.literal("[")
                        .append(Text.translatable("entity.sandevistan.ripperdoc.tag"))
                        .append("] ")
                        .formatted(Formatting.AQUA)
                        .append(Text.translatable("entity.sandevistan.ripperdoc.name." + nameIdx).formatted(Formatting.WHITE));

                    doc.setCustomName(docName);
                    doc.setCustomNameVisible(true);
                    p.getServerWorld().spawnEntity(doc);
                    p.sendMessage(Text.translatable("msg.sandevistan.ripperdoc.spawned"), false);
                    return 1;
                }))
            .then(CommandManager.literal("remove")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    List<RipperdocEntity> list = p.getServerWorld().getEntitiesByClass(
                        RipperdocEntity.class, p.getBoundingBox().expand(32.0), e -> true
                    );
                    int count = list.size();
                    for (RipperdocEntity doc : list) doc.discard();
                    p.sendMessage(Text.translatable("msg.sandevistan.ripperdoc.removed", count), false);
                    return 1;
                })));
    }

    public static void handleBuy(ServerPlayerEntity p, String offerId) {
        if (p.getServer() == null) return;
        ShopOffer selected = null;
        for (ShopOffer o : OFFERS) {
            if (o.id().equals(offerId)) {
                selected = o;
                break;
            }
        }
        if (selected == null) return;

        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        if (!storage.spendEddies(p.getUuid(), selected.price(), selected.id().toUpperCase())) {
            p.sendMessage(Text.translatable("msg.sandevistan.bank.no_funds"), true);
            p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.8f, 0.6f);
            return;
        }

        ItemStack stack = new ItemStack(selected.item());
        Text itemName = stack.getName();
        if (!p.getInventory().insertStack(stack)) {
            p.dropItem(stack, false);
        }

        p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.9f, 1.4f);
        p.sendMessage(Text.translatable("msg.sandevistan.shop.bought", itemName, selected.price()), false);
        FixerContractManager.syncPlayer(p);
    }
}
