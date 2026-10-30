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
    private static final int BOOK_W = 140, ENG_W = 132, ROW = 13, ROWS = 8;
    private int scroll;

    public ResearchScreen(ResearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 71;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth - BOOK_W - ENG_W - 8) / 2 + BOOK_W + 4;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blitSprite(WOOD, x - 4, y - 4, imageWidth + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, x, y, imageWidth, imageHeight);
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + 81, 0x405A4630);  // linea sobre el inventario
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
            var lines = font.split(Component.translatable("gui.remushoku.research.hint"), imageWidth - 16);
            int yy = y + 58 - (lines.size() - 1) * 5;
            for (var line : lines) { g.drawString(font, line, x + imageWidth / 2 - font.width(line) / 2, yy, INK_SOFT, false); yy += 9; }
        }
        // recetario
        int bx = leftPos - BOOK_W - 4, by = y;
        g.blitSprite(WOOD, bx - 4, by - 4, BOOK_W + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, bx, by, BOOK_W, imageHeight);
        g.drawString(font, Component.translatable("gui.remushoku.research.book"), bx + 6, by + 6, INK, false);
        bookHover = ItemStack.EMPTY;
        int ry = by + 19;
        for (ResearchRecipes.Recipe r : ResearchRecipes.ALL) {
            boolean rowHover = mx >= bx + 2 && mx < bx + BOOK_W - 2 && my >= ry - 2 && my < ry + 18;
            if (rowHover) g.fill(bx + 2, ry - 2, bx + BOOK_W - 2, ry + 18, 0x30FFE8A0);
            g.fill(bx + 5, ry + 18, bx + BOOK_W - 5, ry + 19, 0x305A4630);
            int rx = bx + 5;
            java.util.List<ItemStack> ins = new java.util.ArrayList<>();
            if (r.stones() > 0) ins.add(new ItemStack(SpellData.stoneFor(r.stoneTier()), 3));
            for (ResearchRecipes.Need n : r.needs()) ins.add(new ItemStack(n.item(), n.count()));
            for (ItemStack st : ins) { rx = item(g, st, rx, ry, mx, my) + 2; }
            g.drawString(font, "→", rx + 1, ry + 4, INK_SOFT, false);
            rx = item(g, ResearchRecipes.result(r), rx + 10, ry, mx, my);
            String mana = r.mana() + "✦";
            g.drawString(font, mana, bx + BOOK_W - 6 - font.width(mana), ry + 4, 0xFF2A5FA0, false);
            ry += 21;
        }
        // grabado: lista de hechizos que conoce (con la pluma y un pergamino en blanco en los huecos)
        int ex = leftPos + imageWidth + 4, ey = topPos;
        g.blitSprite(WOOD, ex - 4, ey - 4, ENG_W + 8, imageHeight + 8);
        g.blitSprite(PARCHMENT, ex, ey, ENG_W, imageHeight);
        g.drawString(font, Component.translatable("gui.remushoku.research.engrave"), ex + 6, ey + 6, INK, false);
        if (!menu.engraving()) {
            int yy = ey + 22;
            for (var line : font.split(Component.translatable("gui.remushoku.research.engrave_hint"), ENG_W - 12)) {
                g.drawString(font, line, ex + 6, yy, INK_SOFT, false);
                yy += 10;
            }
            if (rollTick >= 0) drawRoll(g, ex, ey);
            return;
        }
        java.util.List<net.zennin.mushoku.magic.Spell> list = known();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - ROWS)));
        for (int i = 0; i < ROWS && i + scroll < list.size(); i++) {
            var s = list.get(i + scroll);
            int ry2 = ey + 20 + i * ROW;
            int ecost = net.zennin.mushoku.menu.ResearchRecipes.engraveCost(s);
            boolean eok = ClientManaState.mana >= ecost || minecraft.player.getAbilities().instabuild;
            boolean hover = mx >= ex + 3 && mx < ex + ENG_W - 3 && my >= ry2 - 1 && my < ry2 + ROW - 1;
            if (hover) g.fill(ex + 3, ry2 - 2, ex + ENG_W - 3, ry2 + ROW - 2, 0x40FFE8A0);
            String c = String.valueOf(ecost);
            String name = font.plainSubstrByWidth(Component.translatable(s.translationKey()).getString(), ENG_W - 14 - font.width(c));
            g.drawString(font, name, ex + 6, ry2, eok ? INK : INK_SOFT, false);
            g.drawString(font, c, ex + ENG_W - 6 - font.width(c), ry2, eok ? 0xFF2A5FA0 : INK_RED, false);
        }
        if (list.size() > ROWS)
            g.drawString(font, (scroll + 1) + "-" + Math.min(list.size(), scroll + ROWS) + "/" + list.size(), ex + 6, ey + 20 + ROWS * ROW, INK_SOFT, false);
        if (list.isEmpty()) g.drawString(font, Component.translatable("gui.remushoku.research.no_known"), ex + 6, ey + 22, INK_SOFT, false);
        // investigar un hechizo nuevo (ruleta): boton abajo
        int bx2 = ex + 5, by2 = ey + imageHeight - 30, bw2 = ENG_W - 10, bh2 = 24;
        boolean rhover = rollTick < 0 && mx >= bx2 && mx < bx2 + bw2 && my >= by2 && my < by2 + bh2;
        boolean rok = ClientManaState.mana >= ResearchMenu.RESEARCH_COST || minecraft.player.getAbilities().instabuild;
        g.fill(bx2, by2, bx2 + bw2, by2 + bh2, 0xFF5A3A22);
        g.fill(bx2 + 1, by2 + 1, bx2 + bw2 - 1, by2 + bh2 - 1, rhover ? 0xFF8A5A30 : 0xFF6E4628);
        g.fill(bx2 + 1, by2 + 1, bx2 + bw2 - 1, by2 + 2, 0xFFC9A227);
        Component rt = Component.translatable("gui.remushoku.research.roll");
        g.drawString(font, rt, bx2 + bw2 / 2 - font.width(rt) / 2, by2 + 3, rok ? 0xFFFFE6A0 : 0xFFB08070, false);
        String info = ResearchMenu.RESEARCH_COST + "✦  ·  " + String.format(java.util.Locale.ROOT, "%.1f", ResearchMenu.researchChance(menu.offeredStone()) * 100) + "%";
        g.drawString(font, info, bx2 + bw2 / 2 - font.width(info) / 2, by2 + 13, 0xFF9FC8FF, false);
        if (rollTick >= 0) drawRoll(g, ex, ey);
    }

    private ItemStack bookHover = ItemStack.EMPTY;

    // ------------------------------------------------------------------ ruleta de investigacion
    private static final int ROLL = 50, SHOW = 70;
    private int rollTick = -1;
    private String rollResult = "", rollShown = "";
    private int lastSwap = -99;

    /** Llega el resultado del servidor: arranca la ruleta (el resultado se ve al final). */
    public void startRoll(String result) { rollResult = result; rollTick = 0; lastSwap = -99; }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (rollTick < 0) return;
        rollTick++;
        // los nombres pasan rapido y despues cada vez mas lento, con un "tic"
        int gap = 1 + rollTick / 7;
        if (rollTick < ROLL && rollTick - lastSwap >= gap) {
            lastSwap = rollTick;
            var all = net.zennin.mushoku.magic.Spell.values();
            rollShown = all[minecraft.level.random.nextInt(all.length)].id;
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 1.4f + minecraft.level.random.nextFloat() * 0.3f, 0.5f));
        }
        if (rollTick == ROLL) {
            rollShown = rollResult;
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(rollResult.isEmpty()
                    ? net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value() : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), rollResult.isEmpty() ? 0.6f : 1.2f, 1f));
        }
        if (rollTick > ROLL + SHOW) rollTick = -1;
    }

    private void drawRoll(GuiGraphics g, int ex, int ey) {
        int x0 = ex + 4, y0 = ey + 44, x1 = ex + ENG_W - 4, y1 = ey + 96;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x0, y0, x1, y1, 0xF0241812);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0xFFC9A227);
        g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0xFFC9A227);
        boolean done = rollTick >= ROLL;
        Component head = Component.translatable(done ? (rollResult.isEmpty() ? "gui.remushoku.research.fail" : "gui.remushoku.research.win") : "gui.remushoku.research.rolling");
        g.drawString(font, head, (x0 + x1) / 2 - font.width(head) / 2, y0 + 7, done && !rollResult.isEmpty() ? 0xFFFFD84A : 0xFFD8C8A8, false);
        var sp = rollShown.isEmpty() ? null : net.zennin.mushoku.magic.Spell.byId(rollShown);
        if (sp != null) {
            String name = font.plainSubstrByWidth(Component.translatable(sp.translationKey()).getString(), x1 - x0 - 8);
            int col = 0xFF000000 | net.zennin.mushoku.magic.SpellKnowledge.rgb(sp.level);
            g.drawString(font, name, (x0 + x1) / 2 - font.width(name) / 2, y0 + 25, done ? col : (col & 0x00FFFFFF) | 0xB0000000, true);
            if (done) {
                Component lv = Component.translatable("level.remushoku." + sp.level.tier);
                g.drawString(font, lv, (x0 + x1) / 2 - font.width(lv) / 2, y0 + 37, col, false);
            }
        } else if (done) {
            Component f = Component.translatable("gui.remushoku.research.burnt");
            g.drawString(font, f, (x0 + x1) / 2 - font.width(f) / 2, y0 + 27, 0xFFB08070, false);
        }
        g.pose().popPose();
    }

    /** Item del recetario en tamano real con su cantidad; devuelve donde termina. */
    private int item(GuiGraphics g, ItemStack st, int x, int y, int mx, int my) {
        g.renderItem(st, x, y);
        g.renderItemDecorations(font, st, x, y);
        if (mx >= x && mx < x + 16 && my >= y && my < y + 16) bookHover = st;
        return x + 16;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (!bookHover.isEmpty()) g.renderTooltip(font, bookHover, mx, my);
    }

    private java.util.List<net.zennin.mushoku.magic.Spell> known() {
        java.util.List<net.zennin.mushoku.magic.Spell> out = new java.util.ArrayList<>();
        for (var s : net.zennin.mushoku.magic.Spell.values())
            if (ResearchMenu.knows(minecraft.player, s)) out.add(s);
        return out;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int ex = leftPos + imageWidth + 4, ey = topPos;
        if (menu.engraving() && rollTick < 0 && mx >= ex + 5 && mx < ex + ENG_W - 5 && my >= ey + imageHeight - 30 && my < ey + imageHeight - 6) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.zennin.mushoku.network.EngravePayload(ResearchMenu.RESEARCH));
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }
        if (menu.engraving() && rollTick < 0 && mx >= ex + 3 && mx < ex + ENG_W - 3 && my >= ey + 18 && my < ey + 18 + ROWS * ROW) {
            int i = (int) ((my - ey - 18) / ROW);
            var list = known();
            if (i >= 0 && i < ROWS && i + scroll < list.size()) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.zennin.mushoku.network.EngravePayload(list.get(i + scroll).id));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= leftPos + imageWidth) { scroll -= (int) Math.signum(dy); return true; }
        return super.mouseScrolled(mx, my, dx, dy);
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

}
