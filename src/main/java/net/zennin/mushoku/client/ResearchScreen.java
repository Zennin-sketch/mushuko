package net.zennin.mushoku.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.item.SpellData;
import net.zennin.mushoku.menu.ResearchMenu;
import net.zennin.mushoku.menu.ResearchRecipes;

import static net.zennin.mushoku.client.MedievalUi.*;

/** Mesa de Investigacion: pergamino con los tres huecos, la flecha rúnica, la piedra que sale y el recetario al costado. */
public class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private static final int BOOK_W = 118;

    public ResearchScreen(ResearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth - BOOK_W - 4) / 2 + BOOK_W + 4;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blitSprite(WOOD, x - 4, y - 4, imageWidth + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, x, y, imageWidth, 78);
        g.blitSprite(PARCHMENT, x, y + 78, imageWidth, imageHeight - 78);
        // huecos
        for (int i = 0; i < 3; i++) slot(g, x + ResearchMenu.IN_X + i * 22 - 1, y + ResearchMenu.IN_Y - 1);
        slot(g, x + ResearchMenu.OUT_X - 5, y + ResearchMenu.OUT_Y - 5, 26);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, x + 7 + c * 18, y + 83 + r * 18);
        for (int c = 0; c < 9; c++) slot(g, x + 7 + c * 18, y + 141);
        // flecha de runas que se llena si alcanza el mana
        int cost = menu.manaCost();
        boolean ok = cost > 0 && (ClientManaState.mana >= cost || minecraft.player.getAbilities().instabuild);
        int ax = x + 96, ay = y + ResearchMenu.IN_Y + 4;
        float t = (minecraft.level.getGameTime() % 40 + pt) / 40f;
        for (int i = 0; i < 4; i++) {
            int col = cost == 0 ? 0xFF9A8466 : ok ? (Math.abs(i / 4f - t) < 0.25f ? 0xFF7FE0FF : 0xFF3A8FD0) : 0xFFB04030;
            g.fill(ax + i * 4, ay + 2, ax + i * 4 + 3, ay + 6, col);
        }
        g.fill(ax + 16, ay, ax + 18, ay + 8, cost == 0 ? 0xFF9A8466 : ok ? 0xFF3A8FD0 : 0xFFB04030);
        g.fill(ax + 18, ay + 2, ax + 20, ay + 6, cost == 0 ? 0xFF9A8466 : ok ? 0xFF3A8FD0 : 0xFFB04030);
        if (cost > 0) {
            Component c = Component.translatable("gui.remushoku.research.cost", cost);
            g.drawString(font, c, x + imageWidth / 2 - font.width(c) / 2 + 10, y + 58, ok ? INK_GREEN : INK_RED, false);
        } else {
            Component c = Component.translatable("gui.remushoku.research.hint");
            g.drawString(font, c, x + imageWidth / 2 - font.width(c) / 2, y + 58, INK_SOFT, false);
        }
        // recetario
        int bx = leftPos - BOOK_W - 4, by = y;
        g.blitSprite(WOOD, bx - 4, by - 4, BOOK_W + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, bx, by, BOOK_W, imageHeight);
        g.drawString(font, Component.translatable("gui.remushoku.research.book"), bx + 6, by + 6, INK, false);
        int ry = by + 20;
        for (ResearchRecipes.Recipe r : ResearchRecipes.ALL) {
            int rx = bx + 5;
            if (r.stones() > 0) {
                icon(g, new ItemStack(SpellData.stoneFor(r.stoneTier())), rx, ry, 0.75f);
                g.drawString(font, "3", rx + 10, ry + 6, INK, false);
                rx += 18;
            }
            for (ResearchRecipes.Need n : r.needs()) {
                icon(g, new ItemStack(n.item()), rx, ry, 0.75f);
                if (n.count() > 1) g.drawString(font, String.valueOf(n.count()), rx + 10, ry + 6, INK, false);
                rx += 18;
            }
            g.drawString(font, ">", rx + 1, ry + 3, INK_SOFT, false);
            icon(g, ResearchRecipes.result(r), rx + 9, ry, 0.75f);
            g.drawString(font, r.mana() + "✦", bx + BOOK_W - 6 - font.width(r.mana() + "✦"), ry + 3, 0xFF2A5FA0, false);
            ry += 23;
        }
    }

    private static void slot(GuiGraphics g, int x, int y) { slot(g, x, y, 18); }

    private static void slot(GuiGraphics g, int x, int y, int s) {
        g.fill(x, y, x + s, y + s, 0xFF5A4630);
        g.fill(x + 1, y + 1, x + s, y + s, 0xFFC9B48E);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, 0xFF8E7656);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 8, INK, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, INK_SOFT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
