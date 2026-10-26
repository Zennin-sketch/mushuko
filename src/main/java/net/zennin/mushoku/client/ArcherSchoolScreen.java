package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.zennin.mushoku.archer.ArcherSchool;
import net.zennin.mushoku.network.ActionPayload;
import net.zennin.mushoku.network.ArcherActionPayload;

/** Eleccion de escuela del arquero (Ojo de Halcon, Lluvia de Acero o Viento Errante). */
public class ArcherSchoolScreen extends Screen {
    public ArcherSchoolScreen() {
        super(Component.translatable("gui.remushoku.archer.choose_school"));
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        ArcherSchool[] schools = ArcherSchool.values();
        for (int i = 0; i < schools.length; i++) {
            ArcherSchool s = schools[i];
            Button b = Button.builder(Component.translatable("school.remushoku." + s.key), btn -> pick(s))
                    .bounds(cx - 155 + i * 105, cy, 100, 20).build();
            b.setTooltip(Tooltip.create(Component.translatable("school.remushoku." + s.key + ".desc")));
            addRenderableWidget(b);
        }
    }

    private void pick(ArcherSchool s) {
        if (!net.zennin.mushoku.magic.SpellCasting.isArcherClass(ClientManaState.playerClass)) ClientPlayNetworking.send(new ActionPayload(ActionPayload.CHOOSE_ARCHER));
        ClientPlayNetworking.send(new ArcherActionPayload(ArcherActionPayload.SCHOOL, 0, s.key));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFD27F);
        g.drawCenteredString(font, Component.translatable("gui.remushoku.archer.school_hint"), width / 2, height / 2 - 24, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
