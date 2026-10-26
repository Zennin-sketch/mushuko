package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.zennin.mushoku.archer.ArcherSchool;
import net.zennin.mushoku.archer.ArcherSkill;
import net.zennin.mushoku.archer.ArcherSystem;
import net.zennin.mushoku.data.ArcherData;
import net.zennin.mushoku.network.ArcherActionPayload;

import java.util.List;

/** Menu del arquero (tecla V): escuelas, aprender habilidades y asignarlas a Z/X/C. Igual que el del espadachin. */
public class ArcherScreen extends Screen {
    private static ArcherSchool tab = null;
    private static final int ROW = 19, PANEL_W = 320;
    private int seenVersion = -1;
    private int left, top;

    public ArcherScreen() {
        super(Component.translatable("gui.remushoku.archer.title"));
    }

    @Override
    protected void init() {
        ArcherData d = ClientArcherState.data;
        seenVersion = ClientArcherState.version;
        if (d.school().isEmpty()) {
            minecraft.setScreen(new ArcherSchoolScreen());
            return;
        }
        if (tab == null) tab = ArcherSchool.byKey(d.school());
        if (tab == null) tab = ArcherSchool.HAWK;
        left = (width - PANEL_W) / 2;
        List<ArcherSkill> skills = ArcherSkill.of(tab);
        top = Math.max(4, (height - (46 + skills.size() * ROW)) / 2);
        ArcherSchool[] schools = ArcherSchool.values();
        for (int i = 0; i < schools.length; i++) {
            ArcherSchool s = schools[i];
            MutableComponent label = Component.translatable("school.remushoku." + s.key);
            if (s == tab) label = label.withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
            addRenderableWidget(Button.builder(label, b -> { tab = s; rebuildWidgets(); })
                    .bounds(left + i * (PANEL_W / 3) + 2, top + 22, PANEL_W / 3 - 4, 18).build());
        }
        int y = top + 46;
        for (ArcherSkill s : skills) {
            addRow(d, s, y);
            y += ROW;
        }
    }

    private void addRow(ArcherData d, ArcherSkill s, int y) {
        int right = left + PANEL_W;
        if (!d.knows(s.id)) {
            Button b = Button.builder(Component.translatable("gui.remushoku.sword.learn", s.cost), btn -> send(ArcherActionPayload.LEARN, 0, s.id))
                    .bounds(right - 110, y, 110, 17).build();
            String why = ArcherSystem.cantLearn(d, s, net.minecraft.client.Minecraft.getInstance().player != null && net.minecraft.client.Minecraft.getInstance().player.getAbilities().instabuild);
            b.active = why == null;
            b.setTooltip(Tooltip.create(why == null ? description(s)
                    : description(s).copy().append("\n").append(Component.translatable(why).withStyle(ChatFormatting.RED))));
            addRenderableWidget(b);
            return;
        }
        if (s.kind == ArcherSkill.Kind.ACTIVE) {
            for (int i = 0; i < 3; i++) {
                int slot = i;
                boolean bound = d.slot(i).equals(s.id);
                String key = switch (i) {
                    case 0 -> ModKeys.SKILL_1.getTranslatedKeyMessage().getString();
                    case 1 -> ModKeys.SKILL_2.getTranslatedKeyMessage().getString();
                    default -> ModKeys.SKILL_3.getTranslatedKeyMessage().getString();
                };
                Button b = Button.builder(Component.literal(key).withStyle(bound ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                        btn -> send(ArcherActionPayload.BIND, slot, s.id)).bounds(right - 110 + i * 37, y, 35, 17).build();
                b.setTooltip(Tooltip.create(Component.translatable("gui.remushoku.sword.bind_tip", key).append("\n").append(description(s))));
                addRenderableWidget(b);
            }
        }
    }

    private static Component description(ArcherSkill s) {
        MutableComponent c = Component.translatable(s.translationKey()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" (").append(Component.translatable("swordtier.remushoku." + s.tier)).append(")").withStyle(ChatFormatting.GRAY))
                .append("\n").append(Component.translatable(s.translationKey() + ".desc").withStyle(ChatFormatting.WHITE));
        if (s.kind == ArcherSkill.Kind.ACTIVE)
            c.append("\n").append(Component.translatable("gui.remushoku.sword.active_info", (int) s.mana, s.cooldown / 20).withStyle(ChatFormatting.AQUA));
        return c;
    }

    private void send(int action, int slot, String arg) {
        ClientPlayNetworking.send(new ArcherActionPayload(action, slot, arg));
    }

    @Override
    public void tick() {
        super.tick();
        if (seenVersion != ClientArcherState.version) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ArcherData d = ClientArcherState.data;
        g.drawCenteredString(font, title, width / 2, top, 0xFFD27F);
        ArcherSchool own = ArcherSchool.byKey(d.school());
        Component info = Component.translatable("gui.remushoku.sword.points", (net.minecraft.client.Minecraft.getInstance().player != null && net.minecraft.client.Minecraft.getInstance().player.getAbilities().instabuild) ? (Object) "∞" : (Object) d.points()).append("   ")
                .append(Component.translatable("gui.remushoku.archer.school",
                        own == null ? Component.literal("-") : Component.translatable("school.remushoku." + own.key)));
        g.drawCenteredString(font, info, width / 2, top + 11, 0xCCCCCC);
        int y = top + 46;
        for (ArcherSkill s : ArcherSkill.of(tab)) {
            boolean known = d.knows(s.id);
            int color = known ? 0x7CFC7C : ArcherSystem.cantLearn(d, s, net.minecraft.client.Minecraft.getInstance().player != null && net.minecraft.client.Minecraft.getInstance().player.getAbilities().instabuild) == null ? 0xFFFFFF : 0x888888;
            Component name = Component.translatable(s.translationKey());
            g.drawString(font, name, left + 4, y + 5, color, true);
            String tier = Component.translatable("swordtier.remushoku." + s.tier).getString();
            g.drawString(font, tier, left + 4 + font.width(name) + 6, y + 5, 0x8899AA, true);
            if (known && s.kind != ArcherSkill.Kind.ACTIVE)
                g.drawString(font, Component.translatable("gui.remushoku.sword.learned"), left + PANEL_W - 110, y + 5, 0x7CFC7C, true);
            if (mouseX >= left && mouseX < left + PANEL_W - 112 && mouseY >= y && mouseY < y + ROW - 2)
                g.renderTooltip(font, font.split(description(s), 220), mouseX, mouseY);
            y += ROW;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
