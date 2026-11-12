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
import ru.sandevistan.BuyImplantPayload;
import ru.sandevistan.SandevistanMod;

import java.util.ArrayList;
import java.util.List;

public class RipperdocShopScreen extends Screen {
    public record ShopEntry(String id, String category, Item item, long price) {}

    private static final List<ShopEntry> ALL_ENTRIES = List.of(
        new ShopEntry("kiroshi_mk1", "eyes", SandevistanMod.KIROSHI_MK1, 2000L),
        new ShopEntry("sandevistan", "os", SandevistanMod.SANDEVISTAN, 7000L)
    );

    private static final String[] CATEGORIES = {"all", "os", "eyes", "arms", "skin", "legs"};

    private String selectedCategory = "all";
    private int selectedIndex = 0;

    public RipperdocShopScreen() {
        super(Text.translatable("shop.sandevistan.title"));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    private List<ShopEntry> getFilteredEntries() {
        if ("all".equals(this.selectedCategory)) return ALL_ENTRIES;
        List<ShopEntry> filtered = new ArrayList<>();
        for (ShopEntry e : ALL_ENTRIES) {
            if (e.category().equals(this.selectedCategory)) filtered.add(e);
        }
        return filtered;
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
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Клик по верхним вкладкам категорий (ВСЕ, ОС, ГЛАЗА, РУКИ, КОЖА, НОГИ)
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

        // Клик по строке импланта в списке
        List<ShopEntry> list = getFilteredEntries();
        int listTop = y + 42;
        for (int i = 0; i < list.size(); i++) {
            int rowY = listTop + i * 26;
            if (isHovered(mouseX, mouseY, x + 8, rowY, w - 16, 24)) {
                this.selectedIndex = i;
                playClick();
                return true;
            }
        }

        // Клик по кнопке «КУПИТЬ»
        if (!list.isEmpty() && isHovered(mouseX, mouseY, x + w - 86, y + h - 24, 78, 18)) {
            if (this.selectedIndex >= 0 && this.selectedIndex < list.size()) {
                ClientPlayNetworking.send(new BuyImplantPayload(list.get(this.selectedIndex).id()));
                playClick();
            }
            return true;
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
        if (active && enabled) {
            ctx.fill(bx, by + bh - 2, bx + bw, by + bh, 0xFF00F0FF);
        }

        int tw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, bx + (bw - tw) / 2, by + (bh - 8) / 2, textColor, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int w = 248;
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        ctx.fill(x, y, x + w, y + h, 0xF512070B);
        ctx.fill(x, y, x + w, y + 1, 0xFFFF2A55);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFF2A55);
        ctx.fill(x, y, x + 1, y + h, 0xFFFF2A55);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFF2A55);

        ctx.fill(x + 1, y + 1, x + w - 1, y + 16, 0xFF1E0B11);
        ctx.drawText(this.textRenderer, Text.translatable("shop.sandevistan.title"), x + 8, y + 4, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.literal("§e€$ " + SandevistanClient.clientEddies), x + w - 74, y + 4, 0xFCEE09, false);

        // Верхние вкладки категорий в стиле рипердока
        int tabW = 37;
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            int tx = x + 8 + i * (tabW + 2);
            boolean active = this.selectedCategory.equals(cat);
            boolean hover = isHovered(mouseX, mouseY, tx, y + 20, tabW, 16);
            drawCyberButton(ctx, tx, y + 20, tabW, 16, Text.translatable("shop.sandevistan.cat." + cat), active, hover, true);
        }

        // Вертикальный список имплантов
        List<ShopEntry> list = getFilteredEntries();
        int listTop = y + 42;
        if (list.isEmpty()) {
            ctx.drawText(this.textRenderer, Text.translatable("shop.sandevistan.empty"), x + 12, listTop + 10, 0x8899A6, false);
        } else {
            for (int i = 0; i < list.size(); i++) {
                ShopEntry entry = list.get(i);
                int rowY = listTop + i * 26;
                boolean isSelected = (i == this.selectedIndex);

                int rowBg = isSelected ? 0xDD38111E : 0xAA1B0A10;
                int rowBorder = isSelected ? 0xFF00F0FF : 0xFF4A1826;
                ctx.fill(x + 8, rowY, x + w - 8, rowY + 24, rowBg);
                ctx.fill(x + 8, rowY, x + 10, rowY + 24, rowBorder);
                ctx.fill(x + 8, rowY + 23, x + w - 8, rowY + 24, rowBorder);

                ctx.drawItem(new ItemStack(entry.item()), x + 14, rowY + 4);

                ctx.drawText(this.textRenderer, entry.item().getName(), x + 36, rowY + 3, 0xFFFFFF, false);
                boolean canAfford = SandevistanClient.clientEddies >= entry.price();
                String priceColor = canAfford ? "§a" : "§c";
                ctx.drawText(this.textRenderer, Text.literal(priceColor + "€$ " + entry.price()), x + 36, rowY + 13, 0xFCEE09, false);
            }
        }

        // Нижняя панель и кастомная неоновая кнопка «КУПИТЬ»
        ctx.fill(x + 8, y + h - 24, x + w - 92, y + h - 6, 0xFF1A090E);
        ctx.drawText(this.textRenderer, Text.translatable("shop.sandevistan.footer"), x + 12, y + h - 19, 0xFF4D70, false);

        boolean canBuy = !list.isEmpty();
        boolean buyHover = canBuy && isHovered(mouseX, mouseY, x + w - 86, y + h - 24, 78, 18);
        drawCyberButton(ctx, x + w - 86, y + h - 24, 78, 18, Text.translatable("shop.sandevistan.buy"), false, buyHover, canBuy);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
