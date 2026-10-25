package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.zennin.mushoku.data.SwordData;
import net.zennin.mushoku.network.SwordActionPayload;
import net.zennin.mushoku.sword.SwordSkill;
import net.zennin.mushoku.sword.SwordStyle;
import net.zennin.mushoku.sword.SwordSystem;

import java.util.List;

/** Menu del espadachin (tecla V): estilos, aprender habilidades, asignarlas a Z/X/C y subir el Touki. */
public class SwordsmanScreen extends Screen {
    private static SwordStyle tab = null;
    private static final int ROW = 19, PANEL_W = 320;
    private int seenVersion = -1;
    private int left, top;

    public SwordsmanScreen() {
        super(Component.translatable("gui.remushoku.swordsman_gui.label_sword_styles"));
    }

    @Override
    protected void init() {
        SwordData d = ClientSwordState.data;
        seenVersion = ClientSwordState.version;
        if (d.style().isEmpty()) {
            minecraft.setScreen(new AffinityScreen());
            return;
        }
        if (tab == null) tab = SwordStyle.byKey(d.style());
        if (tab == null) tab = SwordStyle.SWORD_GOD;
        left = (width - PANEL_W) / 2;
        List<SwordSkill> skills = SwordSkill.of(tab);
        int contentH = 46 + (skills.size() + 1) * ROW;
        top = Math.max(4, (height - contentH) / 2);

        // Pestañas de estilo
        SwordStyle[] styles = SwordStyle.values();
        for (int i = 0; i < styles.length; i++) {
            SwordStyle st = styles[i];
            MutableComponent label = Component.translatable("style.remushoku." + st.key);
            if (st == tab) label = label.withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
            Button b = Button.builder(label, btn -> { tab = st; rebuildWidgets(); })
                    .bounds(left + i * (PANEL_W / 3) + 2, top + 22, PANEL_W / 3 - 4, 18).build();
            addRenderableWidget(b);
        }

        int y = top + 46;
        for (SwordSkill s : skills) {
            addRow(d, s, y);
            y += ROW;
        }
        // Touki
        int bx = left + PANEL_W - 150;
        Button add = Button.builder(Component.translatable("gui.remushoku.sword.touki_add", SwordSystem.TOUKI_COST),
                b -> send(SwordActionPayload.TOUKI_LEVEL, 0, "")).bounds(bx, y, 78, 17).build();
        add.active = d.touki() < SwordSystem.TOUKI_MAX && d.points() >= SwordSystem.TOUKI_COST;
        add.setTooltip(Tooltip.create(Component.translatable("gui.remushoku.sword.touki_desc")));
        addRenderableWidget(add);
        Button tog = Button.builder(Component.translatable(d.toukiOn() ? "gui.remushoku.sword.on" : "gui.remushoku.sword.off")
                        .withStyle(d.toukiOn() ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                b -> send(SwordActionPayload.TOUKI_TOGGLE, 0, "")).bounds(bx + 82, y, 68, 17).build();
        tog.active = d.touki() > 0;
        addRenderableWidget(tog);
    }

    private void addRow(SwordData d, SwordSkill s, int y) {
        Tooltip tip = Tooltip.create(description(s));
        int right = left + PANEL_W;
        if (!d.knows(s.id)) {
            Button b = Button.builder(Component.translatable("gui.remushoku.sword.learn", s.cost), btn -> send(SwordActionPayload.LEARN, 0, s.id))
                    .bounds(right - 110, y, 110, 17).build();
            String why = SwordSystem.cantLearn(d, s);
            b.active = why == null;
            b.setTooltip(Tooltip.create(why == null ? description(s)
                    : description(s).copy().append("\n").append(Component.translatable(why).withStyle(ChatFormatting.RED))));
            addRenderableWidget(b);
            return;
        }
        switch (s.kind) {
            case ACTIVE -> {
                for (int i = 0; i < 3; i++) {
                    int slot = i;
                    boolean bound = d.slot(i).equals(s.id);
                    String key = switch (i) { case 0 -> ModKeys.SKILL_1.getTranslatedKeyMessage().getString();
                        case 1 -> ModKeys.SKILL_2.getTranslatedKeyMessage().getString();
                        default -> ModKeys.SKILL_3.getTranslatedKeyMessage().getString(); };
                    Button b = Button.builder(Component.literal(key).withStyle(bound ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                            btn -> send(SwordActionPayload.BIND, slot, s.id)).bounds(right - 110 + i * 37, y, 35, 17).build();
                    b.setTooltip(Tooltip.create(Component.translatable("gui.remushoku.sword.bind_tip", key).append("\n").append(description(s))));
                    addRenderableWidget(b);
                }
            }
            case TOGGLE -> {
                boolean on = d.isOn(s.id);
                Button b = Button.builder(Component.translatable(on ? "gui.remushoku.sword.on" : "gui.remushoku.sword.off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), btn -> send(SwordActionPayload.TOGGLE, 0, s.id))
                        .bounds(right - 110, y, 110, 17).build();
                b.setTooltip(tip);
                addRenderableWidget(b);
            }
            default -> {}
        }
    }

    private static Component description(SwordSkill s) {
        MutableComponent c = Component.translatable(s.translationKey()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (").append(Component.translatable("swordtier.remushoku." + s.tier)).append(")").withStyle(ChatFormatting.GRAY))
                .append("\n").append(Component.translatable(s.translationKey() + ".desc").withStyle(ChatFormatting.WHITE));
        if (s.kind == SwordSkill.Kind.ACTIVE)
            c.append("\n").append(Component.translatable("gui.remushoku.sword.active_info", (int) s.mana, s.cooldown / 20).withStyle(ChatFormatting.AQUA));
        return c;
    }

    private void send(int action, int slot, String arg) {
        ClientPlayNetworking.send(new SwordActionPayload(action, slot, arg));
    }

    @Override
    public void tick() {
        super.tick();
        if (seenVersion != ClientSwordState.version) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        SwordData d = ClientSwordState.data;
        g.drawCenteredString(font, title, width / 2, top, 0xFFD27F);
        SwordStyle aff = SwordStyle.byKey(d.style());
        Component info = Component.translatable("gui.remushoku.sword.points", d.points()).append("   ")
                .append(Component.translatable("gui.remushoku.sword.affinity",
                        aff == null ? Component.literal("-") : Component.translatable("style.remushoku." + aff.key)));
        g.drawCenteredString(font, info, width / 2, top + 11, 0xCCCCCC);
        int y = top + 46;
        for (SwordSkill s : SwordSkill.of(tab)) {
            boolean known = d.knows(s.id);
            int color = known ? 0x7CFC7C : SwordSystem.cantLearn(d, s) == null ? 0xFFFFFF : 0x888888;
            Component name = Component.translatable(s.translationKey());
            g.drawString(font, name, left + 4, y + 5, color, true);
            String tier = Component.translatable("swordtier.remushoku." + s.tier).getString();
            g.drawString(font, tier, left + 4 + font.width(name) + 6, y + 5, 0x8899AA, true);
            if (known && (s.kind == SwordSkill.Kind.PASSIVE || s.kind == SwordSkill.Kind.STYLE))
                g.drawString(font, Component.translatable("gui.remushoku.sword.learned"), left + PANEL_W - 110, y + 5, 0x7CFC7C, true);
            if (mouseX >= left && mouseX < left + PANEL_W - 112 && mouseY >= y && mouseY < y + ROW - 2)
                g.renderTooltip(font, font.split(description(s), 220), mouseX, mouseY);
            y += ROW;
        }
        g.drawString(font, Component.translatable("gui.remushoku.sword.touki", d.touki(), SwordSystem.TOUKI_MAX), left + 4, y + 5, 0xFFB347, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
