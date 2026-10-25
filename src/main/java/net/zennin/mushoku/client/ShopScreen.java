package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.guild.Coins;
import net.zennin.mushoku.guild.Shops;
import net.zennin.mushoku.network.GuildActionPayload;

import java.util.List;

/** Tienda (herrero, tendero o vendedor de magia): comprar y vender por monedas. */
public class ShopScreen extends Screen {
    private final String shop;
    private int left, top;
    private static final int COL_W = 200, ROW = 26;

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
        int rows = (offers.size() + 1) / 2;
        left = (width - COL_W * 2) / 2;
        top = Math.max(6, (height - (30 + rows * ROW)) / 2);
        for (int i = 0; i < offers.size(); i++) {
            Shops.Offer o = offers.get(i);
            int x = left + (i % 2) * COL_W, y = top + 30 + (i / 2) * ROW;
            int idx = i;
            addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.shop.buy"),
                    b -> ClientPlayNetworking.send(new GuildActionPayload(GuildActionPayload.BUY, idx, shop))).bounds(x + 130, y, 30, 18).build());
            if (o.sell() > 0 && o.ench() == null)
                addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.shop.sell"),
                        b -> ClientPlayNetworking.send(new GuildActionPayload(GuildActionPayload.SELL, idx, shop))).bounds(x + 162, y, 34, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, top, 0xFFD27F);
        int money = minecraft.player == null ? 0 : Coins.total(minecraft.player);
        g.drawCenteredString(font, Component.translatable("gui.remushoku.shop.money", money / Coins.GOLD, money % Coins.GOLD / Coins.SILVER,
                money % Coins.SILVER / Coins.LARGE, money % Coins.LARGE), width / 2, top + 12, 0xCCCCCC);
        List<Shops.Offer> offers = Shops.offers(shop);
        for (int i = 0; i < offers.size(); i++) {
            Shops.Offer o = offers.get(i);
            int x = left + (i % 2) * COL_W, y = top + 30 + (i / 2) * ROW;
            ItemStack st = new ItemStack(o.item().get(), o.count());
            g.renderItem(st, x, y + 1);
            g.renderItemDecorations(font, st, x, y + 1);
            Component name = o.ench() == null ? st.getHoverName()
                    : st.getHoverName().copy().append(" ").append(Component.translatable("gui.remushoku.shop.ench_" + o.ench().location().getPath(), o.enchLevel()));
            g.drawString(font, font.plainSubstrByWidth(name.getString(), 108), x + 20, y, 0xFFFFFF, true);
            Component price = Component.translatable("gui.remushoku.shop.price", GuildScreen.coins(o.buy()));
            if (o.sell() > 0 && o.ench() == null) price = price.copy().append(" · ").append(Component.translatable("gui.remushoku.shop.sell_price", GuildScreen.coins(o.sell())));
            g.drawString(font, price, x + 20, y + 10, 0x8FD8FF, false);
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) g.renderTooltip(font, st, mx, my);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
