package com.magicalmod.client;

import com.magicalmod.net.ModPayloads.RitualActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Кастомное меню круга магии. ВСЕ цвета — с альфой 0xFF, иначе прозрачные. */
public class RitualScreen extends Screen {
    private static final int PANEL_W = 250;
    private static final int PANEL_H = 214;

    private final String status;
    private int panelX;
    private int panelY;

    public RitualScreen(String status) {
        super(Component.literal("КРУГ МАГИИ"));
        this.status = status == null ? "" : status;
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        int bx = panelX + 25;
        int y = panelY + 78;
        addRenderableWidget(styleButton(bx, y, 200, 20, "§a✦ Создать круг", "start", 0xFF2E7D52));
        addRenderableWidget(styleButton(bx, y + 24, 200, 20, "§b➜ Вступить в круг", "join", 0xFF2E5D8A));
        addRenderableWidget(styleButton(bx, y + 48, 200, 20, "§e✕ Выйти из круга", "leave", 0xFF8A6A2E));
        addRenderableWidget(styleButton(bx, y + 72, 200, 20, "§6↻ Обновить", "open", 0xFF6A4E8A));
        addRenderableWidget(styleButton(bx, y + 96, 200, 20, "§c■ Расформировать", "stop", 0xFF8A3A3A));
        addRenderableWidget(styleButton(bx, y + 124, 200, 18, "Закрыть", "close", 0xFF3A3A48));
    }

    private Button styleButton(int x, int y, int w, int h, String label, String action, int color) {
        boolean close = action.equals("close");
        return new StylizedButton(x, y, w, h, Component.literal(label),
                b -> {
                    if (close) this.onClose();
                    else ClientPlayNetworking.send(new RitualActionPayload(action));
                }, color, this.font);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // super.render() вызовет наш renderBackground (панель), затем нарисует кнопки поверх
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Не вызываем super — иначе двойной blur ("Can only blur once per frame")
        long time = System.currentTimeMillis();
        int glow = 0xFF9B5CFF + (int) (Math.sin(time / 400.0D) * 0x101010);
        int px = panelX, py = panelY;

        graphics.fill(px - 3, py - 3, px + PANEL_W + 3, py + PANEL_H + 3, glow);
        graphics.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, 0xFF120A22);
        graphics.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF1B1030);

        graphics.fill(px, py, px + PANEL_W, py + 26, 0xFF3A1F66);
        graphics.fill(px, py + 26, px + PANEL_W, py + 28, glow);
        graphics.drawCenteredString(this.font, this.title, px + PANEL_W / 2, py + 9, 0xFFFFFFFF);

        int cx = px + PANEL_W / 2;
        int cy = py + 52;
        for (int i = 0; i < 10; i++) {
            double a = time / 500.0D + i * (Math.PI * 2.0D / 10.0D);
            int dx = (int) (Math.cos(a) * 13.0D);
            int dy = (int) (Math.sin(a) * 13.0D);
            graphics.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, glow);
        }
        graphics.fill(cx - 5, cy - 5, cx + 5, cy + 5, 0xFF7B3FBF);

        String[] lines = this.status.split("\n");
        int lineY = py + 68;
        for (String line : lines) {
            graphics.drawCenteredString(this.font, Component.literal(line), cx, lineY, 0xFFD8C8F0);
            lineY += 11;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final class StylizedButton extends Button {
        private final int color;
        private final net.minecraft.client.gui.Font font;

        StylizedButton(int x, int y, int w, int h, Component message, OnPress press,
                       int color, net.minecraft.client.gui.Font font) {
            super(x, y, w, h, message, press, b -> Component.empty());
            this.color = color;
            this.font = font;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            int base = isHovered() ? brighten(color) : color;
            graphics.fill(x, y, x + this.width, y + this.height, 0xFF0A0612);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, base);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + 2, brighten(base));
            graphics.drawCenteredString(font, getMessage(),
                    x + this.width / 2, y + (this.height - 8) / 2, 0xFFFFFFFF);
        }

        private static int brighten(int c) {
            int r = Math.min(255, ((c >> 16) & 0xFF) + 45);
            int g = Math.min(255, ((c >> 8) & 0xFF) + 45);
            int b = Math.min(255, (c & 0xFF) + 45);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
}
