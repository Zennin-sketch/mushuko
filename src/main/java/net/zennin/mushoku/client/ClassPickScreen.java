package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zennin.mushoku.network.ActionPayload;

/** Menu para elegir clase: Mago o Espadachin. */
public class ClassPickScreen extends Screen {
    public ClassPickScreen() {
        super(Component.translatable("gui.remushoku.class_pick_gui.label_choose_a_class"));
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.class_pick_gui.button_mage"), b -> pick(ActionPayload.CHOOSE_MAGE))
                .bounds(cx - 105, cy, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.class_pick_gui.button_swordsman"), b -> pick(ActionPayload.CHOOSE_SWORDSMAN))
                .bounds(cx + 5, cy, 100, 20).build());
    }

    private void pick(int action) {
        ClientPlayNetworking.send(new ActionPayload(action));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFD27F);
        g.drawCenteredString(font, Component.translatable("gui.remushoku.class_pick.hint"), width / 2, height / 2 - 24, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
