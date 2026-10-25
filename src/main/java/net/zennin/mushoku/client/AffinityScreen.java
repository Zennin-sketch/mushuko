package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zennin.mushoku.network.ActionPayload;
import net.zennin.mushoku.network.SwordActionPayload;
import net.zennin.mushoku.sword.SwordStyle;

/** Eleccion de afinidad del espadachin (Dios Espada, Dios Agua o Dios Norte). */
public class AffinityScreen extends Screen {
    public AffinityScreen() {
        super(Component.translatable("gui.remushoku.sword_m_pick_gui.label_choose_an_affinity"));
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        SwordStyle[] styles = SwordStyle.values();
        for (int i = 0; i < styles.length; i++) {
            SwordStyle st = styles[i];
            Button b = Button.builder(Component.translatable("style.remushoku." + st.key), btn -> pick(st))
                    .bounds(cx - 155 + i * 105, cy, 100, 20).build();
            b.setTooltip(Tooltip.create(Component.translatable("style.remushoku." + st.key + ".desc")));
            addRenderableWidget(b);
        }
    }

    private void pick(SwordStyle st) {
        if (ClientManaState.playerClass != 2) ClientPlayNetworking.send(new ActionPayload(ActionPayload.CHOOSE_SWORDSMAN));
        ClientPlayNetworking.send(new SwordActionPayload(SwordActionPayload.AFFINITY, 0, st.key));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFD27F);
        g.drawCenteredString(font, Component.translatable("gui.remushoku.sword.affinity_hint"), width / 2, height / 2 - 24, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
