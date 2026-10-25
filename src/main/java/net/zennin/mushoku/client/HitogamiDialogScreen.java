package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.zennin.mushoku.hitogami.HitogamiSystem;
import net.zennin.mushoku.network.HitogamiActionPayload;

import java.util.List;

/**
 * Dialogo de Hitogami, estilo novela visual: el texto aparece letra por letra (clic para mostrarlo entero).
 * Presentacion: al terminar aparece el "mientras puedas..." chiquito y el boton "Crear personaje".
 * Oraculo: la profecia con coordenadas y el boton "Despertar".
 */
public class HitogamiDialogScreen extends Screen {
    private static final int GOLD = 0xFFE8C86A, WHITE = 0xFFF2F0EA;
    private final int mode;
    private final String text;
    private final long start = System.currentTimeMillis();
    private boolean skipped;
    private Button action;

    public HitogamiDialogScreen(int mode, String text) {
        super(Component.literal("Hitogami"));
        this.mode = mode;
        this.text = mode == HitogamiSystem.MODE_INTRO ? Component.translatable("hitogami.remushoku.intro").getString() : text;
    }

    private int boxW() { return Math.min(380, width - 40); }

    private int boxH() { return 120; }

    private int boxX() { return (width - boxW()) / 2; }

    private int boxY() { return height - boxH() - 34; }

    @Override
    protected void init() {
        Component label = mode == HitogamiSystem.MODE_INTRO
                ? Component.translatable("gui.remushoku.hitogami.create") : Component.translatable("gui.remushoku.hitogami.wake");
        action = Button.builder(label, b -> {
            if (mode == HitogamiSystem.MODE_INTRO) minecraft.setScreen(new CharacterCreationScreen());
            else {
                ClientPlayNetworking.send(HitogamiActionPayload.wake());
                onClose();
            }
        }).bounds(width / 2 - 70, height - 28, 140, 20).build();
        action.visible = done();
        addRenderableWidget(action);
    }

    private int shown() {
        if (skipped) return text.length();
        return (int) Math.min(text.length(), (System.currentTimeMillis() - start) * 40 / 1000);
    }

    private boolean done() { return shown() >= text.length(); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!done()) {
            skipped = true;
            action.visible = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Sin el desenfoque de siempre: el mundo blanco queda de fondo, apenas oscurecido abajo
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (done()) action.visible = true;
        super.render(g, mouseX, mouseY, partialTick);  // fondo + boton
        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        g.fill(x, y, x + w, y + h, 0xE0121218);
        g.renderOutline(x, y, w, h, 0xFF8A7440);
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0x60E8C86A);
        // Nombre
        int nameW = font.width("Hitogami") + 12;
        g.fill(x + 10, y - 9, x + 10 + nameW, y + 4, 0xF0121218);
        g.renderOutline(x + 10, y - 9, nameW, 13, 0xFF8A7440);
        g.drawString(font, "Hitogami", x + 16, y - 6, GOLD, false);

        String visible = text.substring(0, shown());
        List<FormattedCharSequence> lines = font.split(Component.literal(visible), w - 24);
        int ty = y + 12;
        for (FormattedCharSequence line : lines) {
            if (ty > y + h - 14) break;
            g.drawString(font, line, x + 12, ty, WHITE, false);
            ty += 10;
        }
        // "mientras puedas..." chiquito y medio transparente
        if (mode == HitogamiSystem.MODE_INTRO && done()) {
            float fade = Math.min(1f, (System.currentTimeMillis() - start - text.length() * 25L) / 1500f);
            if (skipped) fade = 1f;
            int a = (int) (0x70 * Math.max(0f, fade));
            if (a > 4) {
                g.pose().pushPose();
                g.pose().translate(x + w - 12, ty + 4, 0);
                g.pose().scale(0.75f, 0.75f, 1f);
                String small = Component.translatable("hitogami.remushoku.intro_small").getString();
                g.drawString(font, small, -font.width(small), 0, (a << 24) | 0xD8D4CC, false);
                g.pose().popPose();
            }
        }
        if (!done() && System.currentTimeMillis() / 400 % 2 == 0) g.drawString(font, "▼", x + w - 14, y + h - 12, GOLD, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
