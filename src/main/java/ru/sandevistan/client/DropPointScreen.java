package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.sandevistan.DropPointManager;
import ru.sandevistan.SellItemPayload;

import java.util.ArrayList;
import java.util.List;

public class DropPointScreen extends Screen {
    private static final String[] CATEGORIES = {"all", "weapons", "cyberware", "resources"};
    private int selectedCat = 0;
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public DropPointScreen() {
        super(Text.translatable("droppoint.sandevistan.title"));
        if (DropPointManager.ENTRIES.isEmpty()) {
            DropPointManager.init();
        }
    }

    private List<DropPointManager.SellEntry> getFilteredEntries() {
        String cat = CATEGORIES[selectedCat];
        if ("all".equals(cat)) return DropPointManager.ENTRIES;
        List<DropPointManager.SellEntry> list = new ArrayList<>();
        for (DropPointManager.SellEntry e : DropPointManager.ENTRIES) {
            if (cat.equals(e.category())) list.add(e);
        }
        return list;
    }

    private int countInInventory(Item item) {
        if (this.client == null || this.client.player == null) return 0;
        int total = 0;
        for (int i = 0; i != this.client.player.getInventory().size(); i++) {
            ItemStack st = this.client.player.getInventory().getStack(i);
            if (st.isOf(item)) total += st.getCount();
        }
        return total;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int panelW = 310;
        int panelH = 200;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        // Основной фон и рамка в стиле Рипердока
        ctx.fill(x, y, x + panelW, y + panelH, 0xEE12060A);
        ctx.fill(x, y, x + panelW, y + 2, 0xFF00F0FF);
        ctx.fill(x, y + panelH - 2, x + panelW, y + panelH, 0xFFFF2A55);
        ctx.fill(x, y, x + 2, y + panelH, 0xFFFF2A55);
        ctx.fill(x + panelW - 2, y, x + panelW, y + panelH, 0xFF00F0FF);

        // Шапка с названием и балансом Эдди
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("droppoint.sandevistan.title"), x + 10, y + 8, 0x00F0FF);
        String balText = "€$" + SandevistanClient.clientEddies;
        int balW = this.textRenderer.getWidth(balText);
        ctx.drawTextWithShadow(this.textRenderer, balText, x + panelW - balW - 10, y + 8, 0xFCEE09);

        // Вкладки категорий
        int tabX = x + 10;
        int tabY = y + 22;
        for (int i = 0; i != CATEGORIES.length; i++) {
            Text tabLabel = Text.translatable("droppoint.sandevistan.cat." + CATEGORIES[i]);
            int tabW = Math.max(46, this.textRenderer.getWidth(tabLabel) + 12);
            boolean active = i == selectedCat;
            boolean hover = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + 15;

            int bg = active ? 0xFF3A101E : (hover ? 0xFF260B14 : 0xCC18080D);
            int border = active ? 0xFF00F0FF : 0xFF661A30;
            ctx.fill(tabX, tabY, tabX + tabW, tabY + 15, bg);
            ctx.fill(tabX, tabY + 14, tabX + tabW, tabY + 15, border);
            ctx.drawText(this.textRenderer, tabLabel, tabX + 6, tabY + 4, active ? 0x00F0FF : 0xCCCCCC, false);
            tabX += tabW + 4;
        }

        List<DropPointManager.SellEntry> list = getFilteredEntries();
        if (selectedIndex >= list.size()) selectedIndex = Math.max(0, list.size() - 1);
        if (scrollOffset > Math.max(0, list.size() - 6)) scrollOffset = Math.max(0, list.size() - 6);

        // Левый список принимаемых предметов
        int listX = x + 10;
        int listY = y + 43;
        int listW = 158;
        int rowH = 23;
        ctx.fill(listX, listY, listX + listW, listY + 142, 0x88080305);

        int visibleRows = Math.min(6, list.size() - scrollOffset);
        for (int r = 0; r != visibleRows; r++) {
            int idx = scrollOffset + r;
            DropPointManager.SellEntry entry = list.get(idx);
            int ry = listY + 2 + r * rowH;
            boolean sel = idx == selectedIndex;
            boolean hover = mouseX >= listX + 2 && mouseX <= listX + listW - 2 && mouseY >= ry && mouseY <= ry + rowH - 2;

            int rowBg = sel ? 0xCC3D1220 : (hover ? 0x88240A13 : 0x55120509);
            ctx.fill(listX + 2, ry, listX + listW - 2, ry + rowH - 2, rowBg);
            if (sel) {
                ctx.fill(listX + 2, ry, listX + 4, ry + rowH - 2, 0xFF00F0FF);
            }

            ItemStack icon = new ItemStack(entry.item(), entry.batchSize());
            ctx.drawItem(icon, listX + 6, ry + 2);

            Identifier itemKey = Registries.ITEM.getId(entry.item());
            Text name = Text.translatable("item." + itemKey.getNamespace() + "." + itemKey.getPath());
            String shortName = name.getString();
            if (shortName.length() > 14) shortName = shortName.substring(0, 13) + "..";
            ctx.drawText(this.textRenderer, shortName, listX + 26, ry + 3, sel ? 0xFFFFFF : 0xCCCCCC, false);

            int owned = countInInventory(entry.item());
            String sub = "§e€$" + entry.price() + " §7(" + owned + " шт.)";
            ctx.drawText(this.textRenderer, sub, listX + 26, ry + 12, 0xAAAAAA, false);
        }
        // Правая карточка выбранного предмета
        int cardX = x + 174;
        int cardY = y + 43;
        int cardW = 126;
        int cardH = 142;
        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xAA0A0407);
        ctx.fill(cardX, cardY, cardX + cardW, cardY + 1, 0xFF441424);
        ctx.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFF441424);

        if (!list.isEmpty()) {
            DropPointManager.SellEntry selEntry = list.get(selectedIndex);
            ItemStack preview = new ItemStack(selEntry.item(), selEntry.batchSize());
            ctx.drawItem(preview, cardX + (cardW / 2) - 8, cardY + 10);

            Identifier itemKey = Registries.ITEM.getId(selEntry.item());
            Text itemName = Text.translatable("item." + itemKey.getNamespace() + "." + itemKey.getPath());
            int nameW = this.textRenderer.getWidth(itemName);
            ctx.drawTextWithShadow(this.textRenderer, itemName, cardX + Math.max(4, (cardW - nameW) / 2), cardY + 32, 0xFFFFFF);

            int owned = countInInventory(selEntry.item());
            int batches = owned / selEntry.batchSize();
            long totalEarn = batches * selEntry.price();

            String batchInfo = selEntry.batchSize() > 1
                ? "Партия: " + selEntry.batchSize() + " шт. = €$" + selEntry.price()
                : "Цена за 1 шт.: €$" + selEntry.price();
            ctx.drawText(this.textRenderer, "§b" + batchInfo, cardX + 8, cardY + 50, 0x00F0FF, false);
            ctx.drawText(this.textRenderer, "§7В наличии: §f" + owned + " шт.", cardX + 8, cardY + 63, 0xAAAAAA, false);
            ctx.drawText(this.textRenderer, "§7На сумму: §e€$" + totalEarn, cardX + 8, cardY + 75, 0xFCEE09, false);

            // Кнопка ПРОДАТЬ 1 (партию)
            int btn1X = cardX + 8;
            int btn1Y = cardY + 94;
            int btnW = cardW - 16;
            int btnH = 18;
            boolean canSell = batches > 0;
            boolean hover1 = mouseX >= btn1X && mouseX <= btn1X + btnW && mouseY >= btn1Y && mouseY <= btn1Y + btnH;

            int b1Bg = !canSell ? 0x661A1A1A : (hover1 ? 0xFF006070 : 0xCC003844);
            int b1Border = !canSell ? 0xFF444444 : 0xFF00F0FF;
            ctx.fill(btn1X, btn1Y, btn1X + btnW, btn1Y + btnH, b1Bg);
            ctx.fill(btn1X, btn1Y, btn1X + btnW, btn1Y + 1, b1Border);
            ctx.fill(btn1X, btn1Y + btnH - 1, btn1X + btnW, btn1Y + btnH, b1Border);
            Text b1Label = Text.translatable("droppoint.sandevistan.sell_one", selEntry.batchSize(), selEntry.price());
            int b1W = this.textRenderer.getWidth(b1Label);
            ctx.drawTextWithShadow(this.textRenderer, b1Label, btn1X + (btnW - b1W) / 2, btn1Y + 5, canSell ? 0xFFFFFF : 0x777777);

            // Кнопка ПРОДАТЬ ВСЁ
            int btn2Y = cardY + 116;
            boolean hover2 = mouseX >= btn1X && mouseX <= btn1X + btnW && mouseY >= btn2Y && mouseY <= btn2Y + btnH;
            int b2Bg = !canSell ? 0x661A1A1A : (hover2 ? 0xFF6A1B32 : 0xCC3D0E1D);
            int b2Border = !canSell ? 0xFF444444 : 0xFFFF2A55;
            ctx.fill(btn1X, btn2Y, btn1X + btnW, btn2Y + btnH, b2Bg);
            ctx.fill(btn1X, btn2Y, btn1X + btnW, btn2Y + 1, b2Border);
            ctx.fill(btn1X, btn2Y + btnH - 1, btn1X + btnW, btn2Y + btnH, b2Border);
            Text b2Label = Text.translatable("droppoint.sandevistan.sell_all", totalEarn);
            int b2W = this.textRenderer.getWidth(b2Label);
            ctx.drawTextWithShadow(this.textRenderer, b2Label, btn1X + (btnW - b2W) / 2, btn2Y + 5, canSell ? 0xFCEE09 : 0x777777);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<DropPointManager.SellEntry> list = getFilteredEntries();
        int maxScroll = Math.max(0, list.size() - 6);
        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (verticalAmount != 0 && 0 - verticalAmount > 0 && scrollOffset != maxScroll) {
            scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = 310;
            int panelH = 200;
            int x = (this.width - panelW) / 2;
            int y = (this.height - panelH) / 2;

            // Клик по вкладкам категорий
            int tabX = x + 10;
            int tabY = y + 22;
            for (int i = 0; i != CATEGORIES.length; i++) {
                Text tabLabel = Text.translatable("droppoint.sandevistan.cat." + CATEGORIES[i]);
                int tabW = Math.max(46, this.textRenderer.getWidth(tabLabel) + 12);
                if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + 15) {
                    selectedCat = i;
                    selectedIndex = 0;
                    scrollOffset = 0;
                    if (this.client != null) {
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
                    }
                    return true;
                }
                tabX += tabW + 4;
            }

            // Клик по строкам списка
            List<DropPointManager.SellEntry> list = getFilteredEntries();
            int listX = x + 10;
            int listY = y + 43;
            int listW = 158;
            int rowH = 23;
            int visibleRows = Math.min(6, list.size() - scrollOffset);
            for (int r = 0; r != visibleRows; r++) {
                int ry = listY + 2 + r * rowH;
                if (mouseX >= listX + 2 && mouseX <= listX + listW - 2 && mouseY >= ry && mouseY <= ry + rowH - 2) {
                    selectedIndex = scrollOffset + r;
                    if (this.client != null) {
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.1f));
                    }
                    return true;
                }
            }

            // Клик по кнопкам продажи
            if (!list.isEmpty()) {
                DropPointManager.SellEntry selEntry = list.get(selectedIndex);
                int cardX = x + 174;
                int cardY = y + 43;
                int cardW = 126;
                int btn1X = cardX + 8;
                int btnW = cardW - 16;
                int btn1Y = cardY + 94;
                int btn2Y = cardY + 116;
                int btnH = 18;

                if (mouseX >= btn1X && mouseX <= btn1X + btnW && mouseY >= btn1Y && mouseY <= btn1Y + btnH) {
                    ClientPlayNetworking.send(new SellItemPayload(selEntry.id(), false));
                    return true;
                }
                if (mouseX >= btn1X && mouseX <= btn1X + btnW && mouseY >= btn2Y && mouseY <= btn2Y + btnH) {
                    ClientPlayNetworking.send(new SellItemPayload(selEntry.id(), true));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
