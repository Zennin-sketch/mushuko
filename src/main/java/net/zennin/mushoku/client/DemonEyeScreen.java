package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.data.ExtraData;
import net.zennin.mushoku.extra.DemonEyes;
import net.zennin.mushoku.network.GuildActionPayload;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Elegir que ojo demoniaco poner en cada ranura (consume el Ojo Demoniaco de la mano). */
public class DemonEyeScreen extends Screen {
    private static final int W = 300, H = 170;
    private int left, top, seen = -1;

    public DemonEyeScreen() { super(Component.translatable("gui.remushoku.eyes.title")); }

    @Override
    protected void init() {
        seen = ClientExtraState.version;
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
        ExtraData d = ClientExtraState.data;
        for (int slot = 0; slot < 2; slot++) {
            int s = slot, x = left + 22 + slot * 140;
            for (int i = 0; i < DemonEyes.EYES.size(); i++) {
                String eye = DemonEyes.EYES.get(i);
                MButton b = new MButton(x + 8, top + 48 + i * 21, 112, 16, Component.translatable("eye.remushoku." + eye), btn -> {
                    ClientPlayNetworking.send(new GuildActionPayload(GuildActionPayload.EYE_PICK, s, eye));
                    onClose();
                }, d.eye(s).equals(eye));
                b.setTooltip(Tooltip.create(Component.translatable("eye.remushoku." + eye + ".desc")));
                b.active = !d.eye(s).equals(eye);
                addRenderableWidget(b);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (seen != ClientExtraState.version) rebuildWidgets();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.blitSprite(WOOD, left, top + 8, W, H - 8);
        title(g, title, width / 2, top);
        for (int slot = 0; slot < 2; slot++) {
            int x = left + 22 + slot * 140;
            g.blitSprite(PARCHMENT, x, top + 24, 128, 136);
            Component head = Component.translatable("gui.remushoku.eyes.slot", slot + 1,
                    (slot == 0 ? ModKeys.EYE_1 : ModKeys.EYE_2).getTranslatedKeyMessage());
            g.drawString(font, head, x + 64 - font.width(head) / 2, top + 32, INK, false);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
