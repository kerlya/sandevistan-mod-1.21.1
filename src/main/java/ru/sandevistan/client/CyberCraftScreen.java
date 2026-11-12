package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import ru.sandevistan.CraftImplantPayload;
import ru.sandevistan.CyberCraftManager;

import java.util.ArrayList;
import java.util.List;

public class CyberCraftScreen extends Screen {
    private static final String[] CATEGORIES = {"all", "os", "eyes", "arms", "skin", "legs"};

    private final Screen parentWorkbench;
    private String selectedCategory = "all";
    private int selectedIndex = 0;

    public CyberCraftScreen(Screen parentWorkbench) {
        super(Text.translatable("craft.sandevistan.title"));
        this.parentWorkbench = parentWorkbench;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    private List<CyberCraftManager.CyberRecipe> getFilteredRecipes() {
        if ("all".equals(this.selectedCategory)) return CyberCraftManager.RECIPES;
        List<CyberCraftManager.CyberRecipe> filtered = new ArrayList<>();
        for (CyberCraftManager.CyberRecipe r : CyberCraftManager.RECIPES) {
            if (r.category().equals(this.selectedCategory)) filtered.add(r);
        }
        return filtered;
    }

    private int countInInventory(Item item) {
        if (this.client == null || this.client.player == null) return 0;
        int total = 0;
        for (int i = 0; i < this.client.player.getInventory().size(); i++) {
            ItemStack st = this.client.player.getInventory().getStack(i);
            if (st.isOf(item)) total += st.getCount();
        }
        return total;
    }

    private boolean canCraftRecipe(CyberCraftManager.CyberRecipe r) {
        if (SandevistanClient.clientCyberLevel < r.reqLevel()) return false;
        for (CyberCraftManager.IngredientReq req : r.ingredients()) {
            if (countInInventory(req.item()) < req.count()) return false;
        }
        return true;
    }

    private void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }

    private boolean isHovered(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int w = 248;
        int h = 216;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Верхняя вкладка возврата к обычному верстаку
        if (isHovered(mouseX, mouseY, x, y - 20, 96, 18)) {
            playClick();
            if (this.client != null) this.client.setScreen(this.parentWorkbench);
            return true;
        }

        // Вкладки категорий
        int tabW = 37;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int tx = x + 8 + i * (tabW + 2);
            if (isHovered(mouseX, mouseY, tx, y + 20, tabW, 16)) {
                this.selectedCategory = CATEGORIES[i];
                this.selectedIndex = 0;
                playClick();
                return true;
            }
        }

        // Выбор рецепта в списке
        List<CyberCraftManager.CyberRecipe> list = getFilteredRecipes();
        int listTop = y + 42;
        for (int i = 0; i < list.size(); i++) {
            int rowY = listTop + i * 26;
            if (isHovered(mouseX, mouseY, x + 8, rowY, w - 16, 24)) {
                this.selectedIndex = i;
                playClick();
                return true;
            }
        }

        // Кнопка «СОЗДАТЬ»
        if (!list.isEmpty() && this.selectedIndex < list.size()) {
            CyberCraftManager.CyberRecipe sel = list.get(this.selectedIndex);
            if (canCraftRecipe(sel) && isHovered(mouseX, mouseY, x + w - 86, y + h - 24, 78, 18)) {
                ClientPlayNetworking.send(new CraftImplantPayload(sel.id()));
                playClick();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void drawCyberButton(DrawContext ctx, int bx, int by, int bw, int bh, Text label, boolean active, boolean hovered, boolean enabled) {
        int bg = !enabled ? 0xFF14070B : (active ? 0xFF3A101E : (hovered ? 0xFF2B0E18 : 0xFF19080E));
        int border = !enabled ? 0xFF3B1420 : (active ? 0xFF00F0FF : (hovered ? 0xFFFF2A55 : 0xFF6B1E34));
        int textColor = !enabled ? 0x5C2A39 : (active ? 0x00F0FF : (hovered ? 0xFFFFFF : 0xFF4D70));

        ctx.fill(bx, by, bx + bw, by + bh, bg);
        ctx.fill(bx, by, bx + bw, by + 1, border);
        ctx.fill(bx, by + bh - 1, bx + bw, by + bh, border);
        ctx.fill(bx, by, bx + 1, by + bh, border);
        ctx.fill(bx + bw - 1, by, bx + bw, by + bh, border);
        if (active && enabled) ctx.fill(bx, by + bh - 2, bx + bw, by + bh, 0xFF00F0FF);

        int tw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, bx + (bw - tw) / 2, by + (bh - 8) / 2, textColor, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int w = 248;
        int h = 216;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Верхняя вкладка возврата к верстаку
        boolean backHover = isHovered(mouseX, mouseY, x, y - 20, 96, 18);
        drawCyberButton(ctx, x, y - 20, 96, 18, Text.translatable("craft.sandevistan.back"), false, backHover, true);

        ctx.fill(x, y, x + w, y + h, 0xF512070B);
        ctx.fill(x, y, x + w, y + 1, 0xFFFF2A55);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFF2A55);
        ctx.fill(x, y, x + 1, y + h, 0xFFFF2A55);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFF2A55);

        ctx.fill(x + 1, y + 1, x + w - 1, y + 16, 0xFF1E0B11);
        ctx.drawText(this.textRenderer, Text.translatable("craft.sandevistan.title"), x + 8, y + 4, 0x00F0FF, false);
        String lvlStr = "§eLVL " + SandevistanClient.clientCyberLevel;
        ctx.drawText(this.textRenderer, Text.literal(lvlStr), x + w - this.textRenderer.getWidth(lvlStr) - 8, y + 4, 0xFCEE09, false);

        // Вкладки категорий в стиле рипердока
        int tabW = 37;
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            int tx = x + 8 + i * (tabW + 2);
            boolean active = this.selectedCategory.equals(cat);
            boolean hover = isHovered(mouseX, mouseY, tx, y + 20, tabW, 16);
            drawCyberButton(ctx, tx, y + 20, tabW, 16, Text.translatable("shop.sandevistan.cat." + cat), active, hover, true);
        }

        List<CyberCraftManager.CyberRecipe> list = getFilteredRecipes();
        int listTop = y + 42;
        if (list.isEmpty()) {
            ctx.drawText(this.textRenderer, Text.translatable("craft.sandevistan.empty"), x + 12, listTop + 10, 0x8899A6, false);
        } else {
            for (int i = 0; i < list.size(); i++) {
                CyberCraftManager.CyberRecipe r = list.get(i);
                int rowY = listTop + i * 26;
                boolean isSelected = (i == this.selectedIndex);

                int rowBg = isSelected ? 0xDD38111E : 0xAA1B0A10;
                int rowBorder = isSelected ? 0xFF00F0FF : 0xFF4A1826;
                ctx.fill(x + 8, rowY, x + w - 8, rowY + 24, rowBg);
                ctx.fill(x + 8, rowY, x + 10, rowY + 24, rowBorder);
                ctx.fill(x + 8, rowY + 23, x + w - 8, rowY + 24, rowBorder);

                ctx.drawItem(new ItemStack(r.result()), x + 14, rowY + 4);
                ctx.drawText(this.textRenderer, r.result().getName(), x + 36, rowY + 3, 0xFFFFFF, false);

                boolean lvlOk = SandevistanClient.clientCyberLevel >= r.reqLevel();
                String reqColor = lvlOk ? "§a" : "§c";
                ctx.drawText(this.textRenderer, Text.literal(reqColor + "LVL " + r.reqLevel() + " §7// §b+" + r.xpReward() + " XP"), x + 36, rowY + 13, 0xFCEE09, false);
            }

            // Панель необходимых ресурсов для выбранного рецепта
            if (this.selectedIndex < list.size()) {
                CyberCraftManager.CyberRecipe sel = list.get(this.selectedIndex);
                int reqY = y + h - 54;
                ctx.fill(x + 8, reqY, x + w - 8, reqY + 26, 0xFF1A090E);
                ctx.drawText(this.textRenderer, Text.translatable("craft.sandevistan.ingredients"), x + 12, reqY + 2, 0x00F0FF, false);

                int ix = x + 12;
                for (CyberCraftManager.IngredientReq req : sel.ingredients()) {
                    int have = countInInventory(req.item());
                    boolean ok = have >= req.count();
                    ctx.drawItem(new ItemStack(req.item()), ix, reqY + 9);
                    ctx.drawText(this.textRenderer, Text.literal((ok ? "§a" : "§c") + have + "/" + req.count()), ix + 17, reqY + 14, 0xFFFFFF, false);
                    ix += 46;
                }
            }
        }

        // Нижняя панель и кнопка «СОЗДАТЬ»
        ctx.fill(x + 8, y + h - 24, x + w - 92, y + h - 6, 0xFF1A090E);
        ctx.drawText(this.textRenderer, Text.translatable("craft.sandevistan.footer"), x + 12, y + h - 19, 0xFF4D70, false);

        boolean canCraft = !list.isEmpty() && this.selectedIndex < list.size() && canCraftRecipe(list.get(this.selectedIndex));
        boolean craftHover = canCraft && isHovered(mouseX, mouseY, x + w - 86, y + h - 24, 78, 18);
        drawCyberButton(ctx, x + w - 86, y + h - 24, 78, 18, Text.translatable("craft.sandevistan.create"), canCraft, craftHover, canCraft);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
