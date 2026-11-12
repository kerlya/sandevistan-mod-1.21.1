package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    private ButtonWidget buyButton;

    public RipperdocShopScreen() {
        super(Text.translatable("shop.sandevistan.title"));
    }

    private List<ShopEntry> getFilteredEntries() {
        if ("all".equals(this.selectedCategory)) return ALL_ENTRIES;
        List<ShopEntry> filtered = new ArrayList<>();
        for (ShopEntry e : ALL_ENTRIES) {
            if (e.category().equals(this.selectedCategory)) filtered.add(e);
        }
        return filtered;
    }

    @Override
    protected void init() {
        int w = 248;
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        int tabW = 37;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            int tx = x + 8 + i * (tabW + 2);
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("shop.sandevistan.cat." + cat), b -> {
                this.selectedCategory = cat;
                this.selectedIndex = 0;
            }).dimensions(tx, y + 20, tabW, 16).build());
        }

        this.buyButton = ButtonWidget.builder(Text.translatable("shop.sandevistan.buy"), b -> {
            List<ShopEntry> list = getFilteredEntries();
            if (this.selectedIndex >= 0 && this.selectedIndex < list.size()) {
                ClientPlayNetworking.send(new BuyImplantPayload(list.get(this.selectedIndex).id()));
            }
        }).dimensions(x + w - 86, y + h - 24, 78, 18).build();
        this.addDrawableChild(this.buyButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = 248;
        int h = 212;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        List<ShopEntry> list = getFilteredEntries();
        int listTop = y + 42;
        for (int i = 0; i < list.size(); i++) {
            int rowY = listTop + i * 26;
            if (mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= rowY && mouseY <= rowY + 24) {
                this.selectedIndex = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

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

        List<ShopEntry> list = getFilteredEntries();
        int listTop = y + 42;
        if (list.isEmpty()) {
            ctx.drawText(this.textRenderer, Text.translatable("shop.sandevistan.empty"), x + 12, listTop + 10, 0x8899A6, false);
            this.buyButton.active = false;
        } else {
            this.buyButton.active = true;
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

        ctx.fill(x + 8, y + h - 24, x + w - 92, y + h - 6, 0xFF1A090E);
        ctx.drawText(this.textRenderer, Text.translatable("shop.sandevistan.footer"), x + 12, y + h - 19, 0xFF4D70, false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
