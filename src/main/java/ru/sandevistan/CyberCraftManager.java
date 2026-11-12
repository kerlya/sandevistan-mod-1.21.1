package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public final class CyberCraftManager {
    public record IngredientReq(Item item, int count) {}
    public record CyberRecipe(String id, String category, Item result, int reqLevel, int xpReward, List<IngredientReq> ingredients) {}

    public static final List<CyberRecipe> RECIPES = List.of(
        new CyberRecipe(
            "metal_arms",
            "arms",
            SandevistanMod.METAL_ARMS,
            1,
            35,
            List.of(
                new IngredientReq(Items.IRON_INGOT, 6),
                new IngredientReq(Items.COPPER_INGOT, 4),
                new IngredientReq(Items.REDSTONE, 2),
                new IngredientReq(Items.PISTON, 1)
            )
        )
    );

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cyberlevel")
            .executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                CyberwareStorage st = CyberwareStorage.get(p.getServer());
                int lvl = st.getLevel(p.getUuid());
                int xp = st.getXp(p.getUuid());
                int next = st.getXpForNextLevel(lvl);
                p.sendMessage(Text.translatable("msg.sandevistan.level.info", lvl, xp, next), false);
                return 1;
            })
            .then(CommandManager.literal("add")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("xp", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        int addXp = IntegerArgumentType.getInteger(ctx, "xp");
                        grantXp(p, addXp);
                        return 1;
                    }))));
    }

    public static void grantXp(ServerPlayerEntity p, int amount) {
        if (p.getServer() == null || amount <= 0) return;
        CyberwareStorage st = CyberwareStorage.get(p.getServer());
        boolean up = st.addXp(p.getUuid(), amount);
        if (up) {
            int newLvl = st.getLevel(p.getUuid());
            p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.2f);
            p.sendMessage(Text.translatable("msg.sandevistan.level.up", newLvl), false);
        }
        SandevistanMod.forceSyncCyberware(p);
    }

    public static void handleCraft(ServerPlayerEntity p, String recipeId) {
        if (p.getServer() == null) return;
        CyberRecipe recipe = null;
        for (CyberRecipe r : RECIPES) {
            if (r.id().equals(recipeId)) {
                recipe = r;
                break;
            }
        }
        if (recipe == null) return;

        CyberwareStorage st = CyberwareStorage.get(p.getServer());
        int playerLvl = st.getLevel(p.getUuid());
        if (playerLvl < recipe.reqLevel()) {
            p.sendMessage(Text.translatable("msg.sandevistan.craft.low_level", recipe.reqLevel()), true);
            return;
        }

        for (IngredientReq req : recipe.ingredients()) {
            int have = 0;
            for (int i = 0; i < p.getInventory().size(); i++) {
                ItemStack stack = p.getInventory().getStack(i);
                if (stack.isOf(req.item())) have += stack.getCount();
            }
            if (have < req.count()) {
                p.sendMessage(Text.translatable("msg.sandevistan.craft.no_items"), true);
                return;
            }
        }

        for (IngredientReq req : recipe.ingredients()) {
            int left = req.count();
            for (int i = 0; i < p.getInventory().size() && left > 0; i++) {
                ItemStack stack = p.getInventory().getStack(i);
                if (stack.isOf(req.item())) {
                    int take = Math.min(left, stack.getCount());
                    stack.decrement(take);
                    left -= take;
                }
            }
        }

        ItemStack out = new ItemStack(recipe.result());
        if (!p.getInventory().insertStack(out)) {
            p.dropItem(out, false);
        }

        p.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_USE, SoundCategory.MASTER, 0.85f, 1.25f);
        p.sendMessage(Text.translatable("msg.sandevistan.craft.done", out.getName(), recipe.xpReward()), false);
        grantXp(p, recipe.xpReward());
    }
}
