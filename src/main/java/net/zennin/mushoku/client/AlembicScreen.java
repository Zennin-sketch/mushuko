package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.zennin.mushoku.menu.AlembicMenu;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Alambique de Cobre: caldera con el fuego abajo, el serpentin que gotea y la pocion que sale. */
public class AlembicScreen extends AbstractContainerScreen<AlembicMenu> {
    public AlembicScreen(AlembicMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blitSprite(WOOD, x - 4, y - 4, imageWidth + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, x, y, imageWidth, 78);
        g.blitSprite(PARCHMENT, x, y + 78, imageWidth, imageHeight - 78);
        slot(g, x + AlembicMenu.WATER_X - 1, y + AlembicMenu.WATER_Y - 1);
        slot(g, x + AlembicMenu.ING_X - 1, y + AlembicMenu.ING_Y - 1);
        slot(g, x + AlembicMenu.FUEL_X - 1, y + AlembicMenu.FUEL_Y - 1);
        slot(g, x + AlembicMenu.OUT_X - 5, y + AlembicMenu.OUT_Y - 5, 26);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, x + 7 + c * 18, y + 83 + r * 18);
        for (int c = 0; c < 9; c++) slot(g, x + 7 + c * 18, y + 141);
        // caldera de cobre (se llena con el progreso) y serpentin hasta la salida
        int bx = x + 72, by = y + 16, bw = 24, bh = 30;
        g.fill(bx, by, bx + bw, by + bh, 0xFF6E3A22);
        g.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, 0xFF2A1A12);
        int lvl = Math.round((bh - 4) * menu.progress());
        g.fill(bx + 2, by + bh - 2 - lvl, bx + bw - 2, by + bh - 2, 0xFF3A6FE0);
        if (lvl > 2) g.fill(bx + 2, by + bh - 2 - lvl, bx + bw - 2, by + bh - 1 - lvl, 0xFF8FC0FF);
        g.fill(bx + bw - 2, by + 4, x + AlembicMenu.OUT_X - 6, by + 6, 0xFFB86A40);
        g.fill(x + AlembicMenu.OUT_X - 8, by + 6, x + AlembicMenu.OUT_X - 6, y + AlembicMenu.OUT_Y - 6, 0xFFB86A40);
        // fuego
        float f = menu.fuel();
        int fh = Math.round(12 * f);
        if (fh > 0) {
            int fx = x + AlembicMenu.FUEL_X + 1, fy = y + AlembicMenu.FUEL_Y - 4;
            g.fill(fx + 3, fy - fh, fx + 13, fy, 0xFFFF8A20);
            g.fill(fx + 6, fy - Math.max(1, fh - 3), fx + 10, fy, 0xFFFFE070);
        }
        // flecha del agua y del ingrediente a la caldera
        g.fill(x + 62, y + 24, x + 72, y + 26, 0xFF8E7656);
        g.fill(x + 62, y + 58, x + 72, y + 60, 0xFF8E7656);
    }

    private static void slot(GuiGraphics g, int x, int y) { slot(g, x, y, 18); }

    private static void slot(GuiGraphics g, int x, int y, int s) {
        g.fill(x, y, x + s, y + s, 0xFF5A4630);
        g.fill(x + 1, y + 1, x + s, y + s, 0xFFC9B48E);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, 0xFF8E7656);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 5, INK, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, INK_SOFT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
