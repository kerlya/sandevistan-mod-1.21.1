package ru.sandevistan.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.sandevistan.PhoneActionPayload;

public class PhoneScreen extends Screen {
    private int tab = 0;
    private TextFieldWidget playerField;
    private TextFieldWidget amountField;
    private ButtonWidget sendButton;

    public PhoneScreen() {
        super(Text.translatable("phone.sandevistan.title"));
    }

    @Override
    protected void init() {
        int w = 156;
        int h = 218;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.sandevistan.tab.bank"), b -> {
            this.tab = 0;
            updateBankWidgets();
        }).dimensions(x + 8, y + h - 24, 68, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("phone.sandevistan.tab.jobs"), b -> {
            this.tab = 1;
            updateBankWidgets();
        }).dimensions(x + 80, y + h - 24, 68, 18).build());

        this.playerField = new TextFieldWidget(this.textRenderer, x + 12, y + 72, 132, 16, Text.empty());
        this.playerField.setPlaceholder(Text.translatable("phone.sandevistan.bank.nick"));
        this.addDrawableChild(this.playerField);

        this.amountField = new TextFieldWidget(this.textRenderer, x + 12, y + 92, 74, 16, Text.empty());
        this.amountField.setPlaceholder(Text.translatable("phone.sandevistan.bank.sum"));
        this.addDrawableChild(this.amountField);

        this.sendButton = ButtonWidget.builder(Text.translatable("phone.sandevistan.bank.send"), b -> {
            String target = this.playerField.getText().trim();
            try {
                long amt = Long.parseLong(this.amountField.getText().trim());
                if (!target.isEmpty() && amt > 0) {
                    ClientPlayNetworking.send(new PhoneActionPayload(3, target, amt));
                    this.amountField.setText("");
                }
            } catch (NumberFormatException ignored) {}
        }).dimensions(x + 90, y + 92, 54, 16).build();
        this.addDrawableChild(this.sendButton);

        updateBankWidgets();
    }

    private void updateBankWidgets() {
        boolean isBank = (this.tab == 0);
        this.playerField.visible = isBank;
        this.amountField.visible = isBank;
        this.sendButton.visible = isBank;
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int w = 156;
        int h = 218;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        ctx.fill(x, y, x + w, y + h, 0xF511070C);
        ctx.fill(x, y, x + w, y + 2, 0xFF00F0FF);
        ctx.fill(x, y + h - 2, x + w, y + h, 0xFFFF2A55);
        ctx.fill(x, y, x + 2, y + h, 0xFFFF2A55);
        ctx.fill(x + w - 2, y, x + w, y + h, 0xFF00F0FF);

        ctx.fill(x + 2, y + 2, x + w - 2, y + 16, 0xFF1F0B13);
        ctx.drawText(this.textRenderer, Text.literal("§bNC-OS // 5G"), x + 8, y + 5, 0x00F0FF, false);
        ctx.drawText(this.textRenderer, Text.literal("§e98%"), x + w - 30, y + 5, 0xFCEE09, false);

        if (this.tab == 0) {
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.header"), x + 10, y + 22, 0x00F0FF, false);
            ctx.fill(x + 10, y + 34, x + w - 10, y + 56, 0xFF1E0D15);
            ctx.drawText(this.textRenderer, Text.literal("§e§l€$ " + SandevistanClient.clientEddies), x + 16, y + 41, 0xFCEE09, false);

            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.transfer"), x + 10, y + 61, 0xFF2A55, false);

            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.bank.history"), x + 10, y + 114, 0x00F0FF, false);
            String[] entries = SandevistanClient.clientBankHistory.isEmpty()
                ? new String[0]
                : SandevistanClient.clientBankHistory.split("\\|");
            for (int i = 0; i < Math.min(entries.length, 5); i++) {
                String line = entries[i];
                int color = line.startsWith("+") ? 0x00E676 : 0xFF5252;
                ctx.drawText(this.textRenderer, line, x + 10, y + 126 + i * 11, color, false);
            }
        } else {
            ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.header"), x + 10, y + 22, 0xFF2A55, false);
            if (SandevistanClient.clientCallState == 2) {
                ctx.fill(x + 10, y + 36, x + w - 10, y + 122, 0xFF1E0D15);
                ctx.drawText(this.textRenderer, Text.literal("§b" + SandevistanClient.clientFixerName), x + 14, y + 42, 0x00F0FF, false);
                Text mobName = Text.translatable("mob.sandevistan." + SandevistanClient.clientTargetType);
                ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.target", mobName), x + 14, y + 58, 0xFFFFFF, false);
                ctx.drawText(this.textRenderer, Text.literal("§e" + SandevistanClient.clientProgress + " / " + SandevistanClient.clientGoal), x + 14, y + 72, 0xFCEE09, false);
                ctx.drawText(this.textRenderer, Text.literal("§a+€$" + SandevistanClient.clientReward), x + 14, y + 90, 0x00E676, false);
            } else {
                ctx.drawText(this.textRenderer, Text.translatable("phone.sandevistan.jobs.empty"), x + 10, y + 44, 0xA0AEC0, false);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
