package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import ru.sandevistan.PhoneActionPayload;

import java.util.List;
import java.util.Locale;

public class PhoneScreen extends Screen {
    private int tab = 0; // 0 = Банк, 1 = Заказы, 2 = СМС
    private TextFieldWidget playerField;
    private TextFieldWidget amountField;
    private TextFieldWidget smsTargetField;
    private TextFieldWidget smsTextField;

    public PhoneScreen() {
        super(Text.translatable("phone.sandevistan.title"));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    protected void init() {
        int w = 176;
        int h = 238;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        this.playerField = new TextFieldWidget(this.textRenderer, x + 12, y + 84, 152, 16, Text.empty());
        this.playerField.setPlaceholder(Text.translatable("phone.sandevistan.bank.nick"));
        this.addDrawableChild(this.playerField);

        this.amountField = new TextFieldWidget(this.textRenderer, x + 12, y + 104, 92, 16, Text.empty());
        this.amountField.setPlaceholder(Text.translatable("phone.sandevistan.bank.sum"));
        this.addDrawableChild(this.amountField);

        this.smsTargetField = new TextFieldWidget(this.textRenderer, x + 12, y + 36, 152, 15, Text.empty());
        this.smsTargetField.setPlaceholder(Text.translatable("phone.sandevistan.sms.nick"));
        this.addDrawableChild(this.smsTargetField);

        this.smsTextField = new TextFieldWidget(this.textRenderer, x + 12, y + 54, 108, 15, Text.empty());
        this.smsTextField.setMaxLength(80);
        this.smsTextField.setPlaceholder(Text.translatable("phone.sandevistan.sms.msg"));
        this.addDrawableChild(this.smsTextField);

        updateWidgets();
    }

    private void updateWidgets() {
        boolean isBank = (this.tab == 0);
        boolean isSms = (this.tab == 2);
        this.playerField.visible = isBank;
        this.amountField.visible = isBank;
        this.smsTargetField.visible = isSms;
        this.smsTextField.visible = isSms;
        if (isSms && SandevistanClient.clientHasUnreadSms) {
            SandevistanClient.clientHasUnreadSms = false;
            ClientPlayNetworking.send(new PhoneActionPayload(7, "", "", 0L));
        }
    }
    private void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }

    private boolean isHovered(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = 176;
        int h = 238;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        if (button == 0) {
            if (this.tab == 0 && isHovered(mouseX, mouseY, x + 108, y + 104, 56, 16)) {
                String target = this.playerField.getText().trim();
                try {
                    long amt = Long.parseLong(this.amountField.getText().trim());
                    if (!target.isEmpty() && amt > 0) {
                        ClientPlayNetworking.send(new PhoneActionPayload(3, target, "", amt));
                        this.amountField.setText("");
                        playClick();
                    }
                } catch (NumberFormatException ignored) {}
                return true;
            }

            if (this.tab == 1 && SandevistanClient.clientCallState == 2) {
                if ("item".equals(SandevistanClient.clientContractKind) && isHovered(mouseX, mouseY, x + 12, y + 128, 152, 18)) {
                    ClientPlayNetworking.send(new PhoneActionPayload(5, "", "", 0L));
                    playClick();
                    return true;
                }
                if (isHovered(mouseX, mouseY, x + 12, y + 150, 152, 16)) {
                    ClientPlayNetworking.send(new PhoneActionPayload(4, "", "", 0L));
                    playClick();
                    return true;
                }
            }

            if (this.tab == 2 && isHovered(mouseX, mouseY, x + 123, y + 54, 41, 15)) {
                String target = this.smsTargetField.getText().trim();
                String msg = this.smsTextField.getText().trim();
                if (!target.isEmpty() && !msg.isEmpty()) {
                    ClientPlayNetworking.send(new PhoneActionPayload(6, target, msg, 0L));
                    this.smsTextField.setText("");
                    playClick();
                }
                return true;
            }

            int tabY = y + h - 34;
            if (isHovered(mouseX, mouseY, x + 10, tabY, 49, 16)) { this.tab = 0; updateWidgets(); playClick(); return true; }
            if (isHovered(mouseX, mouseY, x + 63, tabY, 50, 16)) { this.tab = 1; updateWidgets(); playClick(); return true; }
            if (isHovered(mouseX, mouseY, x + 117, tabY, 49, 16)) { this.tab = 2; updateWidgets(); playClick(); return true; }
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

        int w = 176;
        int h = 238;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        ctx.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFF0A0407);
        ctx.fill(x - 4, y - 4, x + w + 4, y - 3, 0xFF00F0FF);
        ctx.fill(x - 4, y + h + 3, x + w + 4, y + h + 4, 0xFFFF2A55);
        ctx.fill(x - 4, y - 4, x - 3, y + h + 4, 0xFFFF2A55);
        ctx.fill(x + w + 3, y - 4, x + w + 4, y + h + 4, 0xFF00F0FF);

        ctx.fill(x + w + 4, y + 36, x + w + 6, y + 56, 0xFFFF2A55);
        ctx.fill(x - 6, y + 32, x - 4, y + 48, 0xFF00F0FF);
        ctx.fill(x - 6, y + 52, x - 4, y + 68, 0xFF00F0FF);

        ctx.fill(x + w / 2 - 18, y + 2, x + w / 2 + 14, y + 4, 0xFF2B121B);
        ctx.fill(x + w / 2 + 20, y + 1, x + w / 2 + 23, y + 4, 0xFF00F0FF);

        ctx.fill(x, y + 8, x + w, y + h - 12, 0xF511070C);
        ctx.fill(x + 2, y + 9, x + w - 2, y + 22, 0xFF1F0B13);

        long dayTicks = (this.client != null && this.client.world != null) ? (this.client.world.getTimeOfDay() % 24000L) : 6000L;
        int totalMinutes = (int) (((dayTicks + 6000L) % 24000L) * 60L / 1000L);
        String clockStr = String.format(Locale.US, "%02d:%02d", totalMinutes / 60, totalMinutes % 60);
        int batteryPct = Math.max(12, Math.min(100, 100 - (int) (dayTicks * 85L / 24000L)));
        String batColor = batteryPct <= 25 ? "§c" : (batteryPct <= 50 ? "§e" : "§a");

        ctx.drawText(this.textRenderer, Text.literal("§bNC-OS " + clockStr), x + 8, y + 12, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.literal(batColor + batteryPct + "%"), x + w - 34, y + 12, 0xFCEE09, false);

        if (this.tab == 0) {
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.header"), x + 10, y + 28, 0x00F0FF, false);
            ctx.fill(x + 10, y + 40, x + w - 10, y + 62, 0xFF1E0D15);
            ctx.drawText(this.textRenderer, Text.literal("§e§l€$ " + SandevistanClient.clientEddies), x + 16, y + 47, 0xFCEE09, false);
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.transfer"), x + 10, y + 71, 0xFF2A55, false);

            boolean sendHover = isHovered(mouseX, mouseY, x + 108, y + 104, 56, 16);
            drawCyberButton(ctx, x + 108, y + 104, 56, 16, Text.translatable("phone.sandevistan.bank.send"), false, sendHover, true);

            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.history"), x + 10, y + 126, 0x00F0FF, false);
            String[] entries = SandevistanClient.clientBankHistory.isEmpty() ? new String[0] : SandevistanClient.clientBankHistory.split("\\|");
            for (int i = 0; i < Math.min(entries.length, 5); i++) {
                String line = this.textRenderer.trimToWidth(entries[i], w - 22);
                int color = line.startsWith("+") ? 0x00E676 : 0xFF5252;
                ctx.drawText(this.textRenderer, line, x + 10, y + 138 + i * 11, color, false);
            }
        } else if (this.tab == 1) {
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.header"), x + 10, y + 28, 0xFF2A55, false);
            if (SandevistanClient.clientCallState == 2) {
                ctx.fill(x + 10, y + 42, x + w - 10, y + 122, 0xFF1E0D15);
                String fixerTrim = this.textRenderer.trimToWidth(SandevistanClient.clientFixerName, w - 28);
                ctx.drawText(this.textRenderer, Text.literal("§b" + fixerTrim), x + 14, y + 48, 0x00F0FF, false);

                Text targetName = Text.translatable("target.sandevistan." + SandevistanClient.clientTargetType);
                ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.target", targetName), x + 14, y + 63, 0xFFFFFF, false);

                boolean isItem = "item".equals(SandevistanClient.clientContractKind);
                int cur = isItem ? SandevistanHud.countRequiredItems(MinecraftClient.getInstance(), SandevistanClient.clientTargetType) : SandevistanClient.clientProgress;
                ctx.drawText(this.textRenderer, Text.literal("§e" + cur + " / " + SandevistanClient.clientGoal + "  §a+€$" + SandevistanClient.clientReward), x + 14, y + 78, 0xFCEE09, false);

                if (SandevistanClient.clientDeadlineMs > 0L) {
                    long secLeft = Math.max(0L, (SandevistanClient.clientDeadlineMs - System.currentTimeMillis()) / 1000L);
                    ctx.drawText(this.textRenderer, Text.literal(String.format(Locale.US, "§cTIME: %02d:%02d", secLeft / 60L, secLeft % 60L)), x + 14, y + 95, 0xFF5252, false);
                } else {
                    ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.no_limit"), x + 14, y + 95, 0x8899A6, false);
                }

                if (isItem) {
                    boolean canDeliver = cur >= SandevistanClient.clientGoal;
                    boolean delHover = canDeliver && isHovered(mouseX, mouseY, x + 12, y + 128, 152, 18);
                    drawCyberButton(ctx, x + 12, y + 128, 152, 18, Text.translatable("phone.sandevistan.jobs.deliver"), canDeliver, delHover, canDeliver);
                }
                boolean cancelHover = isHovered(mouseX, mouseY, x + 12, y + 150, 152, 16);
                drawCyberButton(ctx, x + 12, y + 150, 152, 16, Text.translatable("phone.sandevistan.jobs.cancel"), false, cancelHover, true);
            } else {
                ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.empty"), x + 10, y + 48, 0xA0AEC0, false);
            }
        } else {
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.sms.header"), x + 10, y + 25, 0x00F0FF, false);
            boolean smsSendHover = isHovered(mouseX, mouseY, x + 123, y + 54, 41, 15);
            drawCyberButton(ctx, x + 123, y + 54, 41, 15, Text.translatable("phone.sandevistan.bank.send"), false, smsSendHover, true);

            String[] msgs = SandevistanClient.clientSmsHistory.isEmpty() ? new String[0] : SandevistanClient.clientSmsHistory.split("\\|");
            int lineY = y + 75;
            for (String raw : msgs) {
                if (lineY > y + h - 46) break;
                Text display;
                int col;
                if (raw.startsWith("AD:")) {
                    display = Text.translatable("sms.sandevistan.ad." + raw.substring(3));
                    col = 0xFCEE09;
                } else if (raw.startsWith("OUT:")) {
                    display = Text.literal("§7-> " + raw.substring(4));
                    col = 0xA0AEC0;
                } else if (raw.startsWith("IN:")) {
                    display = Text.literal("§b<- " + raw.substring(3));
                    col = 0x00F0FF;
                } else {
                    display = Text.literal(raw);
                    col = 0xFFFFFF;
                }
                List<OrderedText> wrapped = this.textRenderer.wrapLines(display, w - 20);
                for (OrderedText ot : wrapped) {
                    if (lineY > y + h - 46) break;
                    ctx.drawText(this.textRenderer, ot, x + 10, lineY, col, false);
                    lineY += 10;
                }
                lineY += 3;
            }
        }

        int tabY = y + h - 34;
        drawCyberButton(ctx, x + 10, tabY, 49, 16, Text.translatable("phone.sandevistan.tab.bank"), this.tab == 0, isHovered(mouseX, mouseY, x + 10, tabY, 49, 16), true);
        drawCyberButton(ctx, x + 63, tabY, 50, 16, Text.translatable("phone.sandevistan.tab.jobs"), this.tab == 1, isHovered(mouseX, mouseY, x + 63, tabY, 50, 16), true);
        drawCyberButton(ctx, x + 117, tabY, 49, 16, Text.translatable("phone.sandevistan.tab.sms"), this.tab == 2, isHovered(mouseX, mouseY, x + 117, tabY, 49, 16), true);
        if (SandevistanClient.clientHasUnreadSms && this.tab != 2) {
            ctx.fill(x + 161, tabY - 2, x + 165, tabY + 2, 0xFFFF2A55);
        }

        ctx.fill(x + w / 2 - 14, y + h - 8, x + w / 2 + 14, y + h - 5, 0xFF00F0FF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
