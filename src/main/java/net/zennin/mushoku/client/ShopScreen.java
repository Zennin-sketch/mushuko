package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.guild.Coins;
import net.zennin.mushoku.guild.Shops;
import net.zennin.mushoku.network.GuildActionPayload;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;

import static net.zennin.mushoku.client.MedievalUi.*;

/**
 * Tienda (herrero, tendero o vendedor de magia) en un mostrador de madera: cada producto es una ficha de pergamino
 * con su icono, nombre, precio de compra y de venta, y los botones Comprar / Vender a la derecha (sin superponerse).
 */
public class ShopScreen extends Screen {
    private static final int CARD_H = 32, GAP = 4, BTN_W = 52;
    private final String shop;
    private int left, top, panelW, panelH, cols, cardW, perPage, page;

    public ShopScreen(String shop) {
        super(Component.translatable("entity.remushoku." + switch (shop) {
            case Shops.BLACKSMITH -> "black_smith_keep";
            case Shops.MAGIC -> "magic_shop_keeper";
            default -> "general_store_keep";
        }));
        this.shop = shop;
    }

    @Override
    protected void init() {
        List<Shops.Offer> offers = Shops.offers(shop);
        cols = width >= 470 ? 2 : 1;
        panelW = cols == 2 ? 450 : Math.min(240, width - 8);
        cardW = (panelW - 20 - GAP * (cols - 1)) / cols;
        int maxRows = Math.max(1, (height - 70) / (CARD_H + GAP));
        int rows = Math.min(maxRows, (offers.size() + cols - 1) / cols);
        perPage = rows * cols;
        int pages = (offers.size() + perPage - 1) / perPage;
        page = Math.min(page, pages - 1);
        panelH = 46 + rows * (CARD_H + GAP) + (pages > 1 ? 18 : 4);
        left = (width - panelW) / 2;
        top = Math.max(2, (height - panelH) / 2);

        for (int k = 0; k < perPage; k++) {
            int i = page * perPage + k;
            if (i >= offers.size()) break;
            Shops.Offer o = offers.get(i);
            int x = cardX(k), y = cardY(k);
            int bx = x + cardW - BTN_W - 5;
            addRenderableWidget(new MButton(bx, y + 3, BTN_W, 12, Component.translatable("gui.remushoku.shop.buy"),
                    b -> ClientPlayNetworking.send(new GuildActionPayload(GuildActionPayload.BUY, i, shop))));
            if (o.sell() > 0 && o.ench() == null)
                addRenderableWidget(new MButton(bx, y + 17, BTN_W, 12, Component.translatable("gui.remushoku.shop.sell"),
                        b -> ClientPlayNetworking.send(new GuildActionPayload(GuildActionPayload.SELL, i, shop))));
        }
        if (pages > 1) {
            int py = top + panelH - 18;
            MButton prev = new MButton(left + panelW / 2 - 60, py, 20, 12, Component.literal("<"), b -> { page--; rebuildWidgets(); });
            prev.active = page > 0;
            MButton next = new MButton(left + panelW / 2 + 40, py, 20, 12, Component.literal(">"), b -> { page++; rebuildWidgets(); });
            next.active = page < pages - 1;
            addRenderableWidget(prev);
            addRenderableWidget(next);
        }
    }

    private int cardX(int k) { return left + 10 + (k % cols) * (cardW + GAP); }

    private int cardY(int k) { return top + 44 + (k / cols) * (CARD_H + GAP); }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.blitSprite(WOOD, left, top + 8, panelW, panelH - 8);
        title(g, title, width / 2, top);

        // Bolsa del jugador: oro, plata, cobre grande y cobre chico con sus iconos
        int money = minecraft.player == null ? 0 : Coins.total(minecraft.player);
        int[] amounts = {money / Coins.GOLD, money % Coins.GOLD / Coins.SILVER, money % Coins.SILVER / Coins.LARGE, money % Coins.LARGE};
        ItemStack[] coins = {new ItemStack(ModItems.GOLD_COIN), new ItemStack(ModItems.SILVER_COIN), new ItemStack(ModItems.LARGE_COPPER), new ItemStack(ModItems.SMALL_COPPER)};
        Component purse = Component.translatable("gui.remushoku.shop.purse");
        int stripW = font.width(purse) + 4 * 34 + 16;
        int sx = width / 2 - stripW / 2;
        g.blitSprite(PARCHMENT, sx - 4, top + 22, stripW + 8, 16);
        ink(g, purse, sx + 2, top + 26, INK);
        int cx = sx + font.width(purse) + 8;
        for (int c = 0; c < 4; c++) {
            icon(g, coins[c], cx, top + 23, 0.75f);
            g.drawString(font, String.valueOf(amounts[c]), cx + 13, top + 26, INK, false);
            cx += 34;
        }

        List<Shops.Offer> offers = Shops.offers(shop);
        for (int k = 0; k < perPage; k++) {
            int i = page * perPage + k;
            if (i >= offers.size()) break;
            Shops.Offer o = offers.get(i);
            int x = cardX(k), y = cardY(k);
            g.blitSprite(PARCHMENT, x, y, cardW, CARD_H);
            ItemStack st = new ItemStack(o.item().get(), o.count());
            g.renderItem(st, x + 6, y + 8);
            g.renderItemDecorations(font, st, x + 6, y + 8);
            int textX = x + 26, textW = cardW - 26 - BTN_W - 10;
            Component name = st.getHoverName();
            if (o.ench() != null)
                name = name.copy().append(" ").append(Component.translatable("gui.remushoku.shop.ench_" + o.ench().location().getPath(), o.enchLevel()));
            g.drawString(font, font.plainSubstrByWidth(name.getString(), textW), textX, y + 6, INK, false);
            // Precios: "Compra N (moneda)"   "Venta N (moneda)"
            int px = textX;
            String buy = Component.translatable("gui.remushoku.shop.buy_short").getString();
            g.drawString(font, buy, px, y + 19, INK_SOFT, false);
            px += font.width(buy) + 3;
            px += price(g, o.buy(), px, y + 19, INK_RED);
            if (o.sell() > 0 && o.ench() == null) {
                String sell = Component.translatable("gui.remushoku.shop.sell_short").getString();
                if (px + 6 + font.width(sell) + 3 + priceW(o.sell()) <= textX + textW) {
                    px += 6;
                    g.drawString(font, sell, px, y + 19, INK_SOFT, false);
                    px += font.width(sell) + 3;
                    price(g, o.sell(), px, y + 19, INK_GREEN);
                }
            }
        }
        if (perPage < offers.size()) {
            int pages = (offers.size() + perPage - 1) / perPage;
            g.drawCenteredString(font, (page + 1) + " / " + pages, left + panelW / 2, top + panelH - 16, GOLD);
        }
    }

    private int priceW(int amount) { return font.width(String.valueOf(Coins.count(amount))) + 11; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        List<Shops.Offer> offers = Shops.offers(shop);
        for (int k = 0; k < perPage; k++) {
            int i = page * perPage + k;
            if (i >= offers.size()) break;
            Shops.Offer o = offers.get(i);
            int x = cardX(k), y = cardY(k);
            if (mx >= x && mx < x + cardW - BTN_W - 8 && my >= y && my < y + CARD_H) {
                Component tip = Component.translatable("gui.remushoku.shop.tip_buy", o.count(), new ItemStack(o.item().get()).getHoverName(),
                        Component.translatable("coin.remushoku." + Coins.key(o.buy()), Coins.count(o.buy())));
                if (o.sell() > 0 && o.ench() == null)
                    tip = tip.copy().append("\n").append(Component.translatable("gui.remushoku.shop.tip_sell", o.count(),
                            Component.translatable("coin.remushoku." + Coins.key(o.sell()), Coins.count(o.sell()))));
                g.renderTooltip(font, font.split(tip, 220), mx, my);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
