package com.magicalmod.client;

import com.magicalmod.net.ModPayloads.RitualActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Специальное меню групповой магии (открывается клавишей G). */
public class RitualScreen extends Screen {
    private final String status;

    public RitualScreen(String status) {
        super(Component.literal("§6Круг магии"));
        this.status = status == null ? "" : status;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 92;

        addRenderableWidget(button("§aСоздать круг", "start", cx, y));
        addRenderableWidget(button("§bВступить в круг", "join", cx, y + 26));
        addRenderableWidget(button("§eВыйти из круга", "leave", cx, y + 52));
        addRenderableWidget(button("§6Обновить", "open", cx, y + 78));
        addRenderableWidget(button("§cРасформировать", "stop", cx, y + 104));
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                .pos(cx - 100, y + 136).size(200, 20).build());
    }

    private Button button(String label, String action, int cx, int y) {
        return Button.builder(Component.literal(label), b -> send(action))
                .pos(cx - 100, y).size(200, 20).build();
    }

    private void send(String action) {
        ClientPlayNetworking.send(new RitualActionPayload(action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // ВАЖНО: super.render() сам рисует фон и делает blur. Повторный вызов
        // renderBackground() приводит к IllegalStateException "Can only blur once per frame".
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 26, 0xFFFFFF);

        String[] lines = this.status.split("\n");
        int lineY = 48;
        for (String line : lines) {
            graphics.drawCenteredString(this.font, Component.literal(line), this.width / 2, lineY, 0xCCCCCC);
            lineY += 12;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
