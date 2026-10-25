package net.zennin.mushoku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.guild.Coins;
import net.zennin.mushoku.registry.ModItems;

/** Estilo medieval de los menus: tablon de madera, pergaminos, cinta de titulo y botones de cuero con borde dorado. */
public final class MedievalUi {
    public static final ResourceLocation WOOD = MushokuMod.id("wood_panel"), PARCHMENT = MushokuMod.id("parchment"),
            SCROLL = MushokuMod.id("scroll"), RIBBON = MushokuMod.id("ribbon"), PIN = MushokuMod.id("pin");
    private static final ResourceLocation BTN = MushokuMod.id("button"), BTN_HOVER = MushokuMod.id("button_hover"),
            BTN_OFF = MushokuMod.id("button_disabled"), TAB = MushokuMod.id("tab_active");
    /** Tinta marron sobre pergamino. */
    public static final int INK = 0xFF3B2410, INK_SOFT = 0xFF6B4A28, GOLD = 0xFFF0D070, INK_GREEN = 0xFF2E6B1E, INK_RED = 0xFF8B1A1A;

    private MedievalUi() {}

    public static Font font() { return Minecraft.getInstance().font; }

    public static void title(GuiGraphics g, Component title, int cx, int y) {
        int w = font().width(title) + 36;
        g.blitSprite(RIBBON, cx - w / 2, y, w, 18);
        g.drawCenteredString(font(), title, cx, y + 5, GOLD);
    }

    public static void ink(GuiGraphics g, Component c, int x, int y, int color) {
        g.drawString(font(), c, x, y, color, false);
    }

    /** Dibuja "N [moneda]" con la moneda mas adecuada; devuelve el ancho usado. */
    public static int price(GuiGraphics g, int amount, int x, int y, int color) {
        String k = Coins.key(amount);
        Item coin = switch (k) { case "gold" -> ModItems.GOLD_COIN; case "silver" -> ModItems.SILVER_COIN; case "large" -> ModItems.LARGE_COPPER; default -> ModItems.SMALL_COPPER; };
        String n = String.valueOf(Coins.count(amount));
        g.drawString(font(), n, x, y, color, false);
        int w = font().width(n) + 1;
        icon(g, new ItemStack(coin), x + w, y - 1, 0.6f);
        return w + 10;
    }

    public static void icon(GuiGraphics g, ItemStack st, int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
    }

    /** Boton de cuero con borde dorado (o pestaña roja si esta seleccionado). */
    public static class MButton extends Button {
        private final boolean selected;

        public MButton(int x, int y, int w, int h, Component msg, OnPress onPress, boolean selected) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
            this.selected = selected;
        }

        public MButton(int x, int y, int w, int h, Component msg, OnPress onPress) { this(x, y, w, h, msg, onPress, false); }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            ResourceLocation s = !active ? BTN_OFF : selected ? TAB : isHoveredOrFocused() ? BTN_HOVER : BTN;
            g.blitSprite(s, getX(), getY(), getWidth(), getHeight());
            int color = !active ? 0xFF9A8E7E : isHoveredOrFocused() ? 0xFFFFF4C0 : GOLD;
            Font f = font();
            String text = f.plainSubstrByWidth(getMessage().getString(), getWidth() - 4);
            g.drawCenteredString(f, text, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2 + 1, color);
        }
    }
}
