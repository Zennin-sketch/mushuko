package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.zennin.mushoku.client.MedievalUi.MButton;
import net.zennin.mushoku.companion.CompanionState;
import net.zennin.mushoku.network.CompanionActionPayload;
import net.zennin.mushoku.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

import static net.zennin.mushoku.client.MedievalUi.*;

/**
 * Menu de interaccion con un personaje (al estilo de Minecraft Comes Alive): retrato animado, corazones,
 * animo y estado a la izquierda; Hablar / Interactuar / Ordenes / Especial a la derecha.
 */
public class CompanionScreen extends net.minecraft.client.gui.screens.Screen {
    private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full"),
            HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half"), HEART_EMPTY = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final int W = 320, H = 200;
    private static String tab = "talk";
    private CompanionState state;
    private int left, top;

    public CompanionScreen(CompanionState state) {
        super(Component.empty());
        this.state = state;
    }

    /** Llega el estado del servidor: abre el menu o lo actualiza. */
    public static void receive(CompanionState s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CompanionScreen cs && cs.state.entityId() == s.entityId()) {
            cs.state = s;
            cs.rebuildWidgets();
        } else if (mc.screen == null) {
            mc.setScreen(new CompanionScreen(s));
        }
    }

    private Entity entity() { return minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(state.entityId()); }

    private void act(String a) { ClientPlayNetworking.send(new CompanionActionPayload(state.entityId(), a)); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
        String[] tabs = {"talk", "interact", "command", "special"};
        int tx = left + 130;
        for (int i = 0; i < tabs.length; i++) {
            String t = tabs[i];
            addRenderableWidget(new MButton(tx + i * 46, top + 24, 44, 14, Component.translatable("gui.remushoku.companion.tab." + t),
                    b -> { tab = t; rebuildWidgets(); }, t.equals(tab)));
        }
        List<String> actions = new ArrayList<>();
        switch (tab) {
            case "talk" -> {
                actions.addAll(List.of("greet", "chat", "joke", "compliment", "story"));
                if (state.romance()) actions.add("flirt");
            }
            case "interact" -> {
                actions.addAll(List.of("hug", "gift"));
                if (state.romance()) actions.add("kiss");
            }
            case "command" -> actions.addAll(List.of("follow", "stay", "free", "sethome", "gohome"));
            default -> {
                actions.add("special");
                if (state.romance()) actions.add(state.married() ? "divorce" : "marry");
            }
        }
        for (int i = 0; i < actions.size(); i++) {
            String a = actions.get(i);
            int col = i % 2, row = i / 2;
            MButton b = new MButton(tx + col * 92, top + 46 + row * 20, 90, 16, label(a), btn -> act(a),
                    a.equals("follow") && state.mode() == 1 || a.equals("stay") && state.mode() == 2 || a.equals("free") && state.mode() == 0);
            b.setTooltip(Tooltip.create(Component.translatable("gui.remushoku.companion.tip." + (a.equals("special") ? "special." + state.kind() : a))));
            if (a.equals("marry")) b.active = minecraft.player != null && minecraft.player.getMainHandItem().is(ModItems.WEDDING_RING) && !state.marriedOther();
            if (a.equals("gift")) b.active = minecraft.player != null && !minecraft.player.getMainHandItem().isEmpty();
            if (a.equals("gohome")) b.active = state.hasHome();
            addRenderableWidget(b);
        }
        addRenderableWidget(new MButton(left + W - 64, top + H - 22, 54, 14, Component.translatable("gui.remushoku.companion.close"), b -> onClose()));
    }

    private Component label(String a) {
        return a.equals("special") ? Component.translatable("gui.remushoku.companion.special." + state.kind())
                : Component.translatable("gui.remushoku.companion." + a);
    }

    @Override
    public void tick() {
        super.tick();
        Entity e = entity();
        if (e == null || !e.isAlive() || minecraft.player.distanceToSqr(e) > 144) onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.blitSprite(WOOD, left, top + 8, W, H - 8);
        Entity e = entity();
        title(g, e != null ? e.getDisplayName() : Component.literal("?"), width / 2, top);

        // Retrato
        g.blitSprite(PARCHMENT, left + 10, top + 22, 112, H - 32);
        if (e instanceof LivingEntity le)
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 14, top + 26, left + 118, top + 120, 38, 0.0625f, mx, my, le);

        // Corazones: 10 corazones = 100 de relacion (mas alla, se muestran dorados)
        int hearts = state.hearts();
        int hx = left + 16, hy = top + 124;
        for (int i = 0; i < 10; i++) {
            int value = hearts - i * 10;
            g.blitSprite(HEART_EMPTY, hx + i * 10, hy, 9, 9);
            if (value >= 10) g.blitSprite(HEART_FULL, hx + i * 10, hy, 9, 9);
            else if (value >= 5) g.blitSprite(HEART_HALF, hx + i * 10, hy, 9, 9);
        }
        String num = hearts + (hearts > 100 ? " ★" : "");
        g.drawString(font, num, left + 66 - font.width(num) / 2, hy + 11, hearts < 0 ? INK_RED : INK, false);
        int mood = state.mood();
        String moodKey = mood >= 8 ? "very_happy" : mood >= 3 ? "happy" : mood > -3 ? "neutral" : mood > -8 ? "upset" : "angry";
        Component moodC = Component.translatable("gui.remushoku.companion.mood", Component.translatable("gui.remushoku.companion.mood." + moodKey));
        g.drawString(font, moodC, left + 66 - font.width(moodC) / 2, hy + 22, INK_SOFT, false);
        Component status = Component.translatable(state.married() ? "gui.remushoku.companion.status.married"
                : "gui.remushoku.companion.status.mode" + state.mode());
        g.drawString(font, status, left + 66 - font.width(status) / 2, hy + 33, state.married() ? INK_RED : INK_SOFT, false);

        // Dialogo (ultima frase)
        g.blitSprite(SCROLL, left + 128, top + 114, W - 138, 62);
        if (!state.line().isEmpty() && minecraft.player != null) {
            Component line = Component.translatable(state.line(), minecraft.player.getDisplayName());
            int y = top + 125;
            for (FormattedCharSequence l : font.split(line, W - 152)) {
                if (y > top + 160) break;
                g.drawString(font, l, left + 135, y, INK, false);
                y += 9;
            }
        } else {
            g.drawString(font, Component.translatable("gui.remushoku.companion.hint"), left + 135, top + 125, INK_SOFT, false);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
