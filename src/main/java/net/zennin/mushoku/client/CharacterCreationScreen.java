package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.network.HitogamiActionPayload;
import net.zennin.mushoku.race.Appearance;
import net.zennin.mushoku.race.Race;
import net.zennin.mushoku.registry.ModItems;
import net.zennin.mushoku.sword.SwordStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Creacion de personaje (estilo Dragon Mine Z): tu personaje en el centro, el selector abajo, la historia de la opcion a
 * la izquierda y lo que cambia a la derecha. Pasos: Raza -> Rasgo (elfo, demonio y bestia) -> Clase.
 * El equipo que se ve en la clase es solo de muestra: no se entrega.
 */
public class CharacterCreationScreen extends Screen {
    private enum Stage { RACE, TRAIT, CLASS }

    private static final Race[] RACES = {Race.HUMAN, Race.ELF, Race.DWARF, Race.MIGURD, Race.SUPERD, Race.OGRE, Race.BEAST, Race.DEMON};
    private static final String[] CLASSES = {"mage", "swordsman", "archer"};
    private static final int GOLD = 0xFFE8C86A, TEXT = 0xFFE6E2D8, DIM = 0xFF9C988E, PANEL = 0xC8101016, LINE = 0xFF6E5C34;

    private Stage stage = Stage.RACE;
    private int race = 0, variant = 0, height = 1, cls = 0, aff = 0;

    public CharacterCreationScreen() {
        super(Component.translatable("gui.remushoku.cc.title"));
    }

    private Race race() { return RACES[race]; }

    private boolean hasTrait() { Race r = race(); return r == Race.ELF || r == Race.DEMON || r == Race.BEAST; }

    private Appearance appearance() { return Appearance.forRace(race(), variant, height); }

    @Override
    protected void init() {
        int cx = width / 2;
        int sy = height - 72;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1)).bounds(cx - 95, sy, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1)).bounds(cx + 75, sy, 20, 20).build());
        if (stage == Stage.CLASS && cls == 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { aff = (aff + 2) % 3; }).bounds(cx - 95, sy + 24, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { aff = (aff + 1) % 3; }).bounds(cx + 75, sy + 24, 20, 20).build());
        }
        if (stage != Stage.RACE)
            addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.cc.back"), b -> back()).bounds(cx - 190, height - 26, 90, 20).build());
        boolean last = stage == Stage.CLASS;
        Button next = Button.builder(Component.translatable(last ? "gui.remushoku.cc.confirm" : "gui.remushoku.cc.next"), b -> next())
                .bounds(cx + 100, height - 26, 90, 20).build();
        next.active = !(last && cls == 2);
        addRenderableWidget(next);
    }

    private void cycle(int dir) {
        switch (stage) {
            case RACE -> { race = (race + dir + RACES.length) % RACES.length; variant = 0; height = 1; }
            case TRAIT -> {
                if (race() == Race.BEAST) variant = (variant + dir + 3) % 3;
                else height = (height + dir + 3) % 3;
            }
            case CLASS -> cls = (cls + dir + CLASSES.length) % CLASSES.length;
        }
        rebuildWidgets();
    }

    private void back() {
        stage = stage == Stage.CLASS && hasTrait() ? Stage.TRAIT : Stage.RACE;
        rebuildWidgets();
    }

    private void next() {
        switch (stage) {
            case RACE -> stage = hasTrait() ? Stage.TRAIT : Stage.CLASS;
            case TRAIT -> stage = Stage.CLASS;
            case CLASS -> {
                if (cls == 2) return;
                ClientPlayNetworking.send(new HitogamiActionPayload(HitogamiActionPayload.CREATE, race().key, variant, height,
                        cls == 0 ? SpellCasting.CLASS_MAGE : SpellCasting.CLASS_SWORDSMAN, cls == 1 ? SwordStyle.values()[aff].key : ""));
                onClose();
                return;
            }
        }
        rebuildWidgets();
    }

    @Override
    public void removed() {
        ClientAppearance.preview = null;
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xB0141420, 0xE00A0A10);
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 8, GOLD);
        drawSteps(g, cx, 22);

        renderPlayer(g, cx, mouseX, mouseY);

        // Selector(es)
        int sy = height - 72;
        selectorBox(g, cx, sy, selectorLabel(), stage == Stage.CLASS && cls == 2 ? DIM : TEXT);
        if (stage == Stage.CLASS && cls == 1)
            selectorBox(g, cx, sy + 24, Component.translatable("style.remushoku." + SwordStyle.values()[aff].key), TEXT);

        // Paneles
        int side = Math.max(110, Math.min(180, cx - 120));
        panel(g, 10, 40, side, height - 120, leftTitle(), leftText());
        panel(g, width - 10 - side, 40, side, height - 120, rightTitle(), rightText());
    }

    private void drawSteps(GuiGraphics g, int cx, int y) {
        String[] keys = {"gui.remushoku.cc.step_race", "gui.remushoku.cc.step_trait", "gui.remushoku.cc.step_class"};
        Stage[] st = {Stage.RACE, Stage.TRAIT, Stage.CLASS};
        List<Component> parts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (st[i] == Stage.TRAIT && !hasTrait()) continue;
            parts.add(Component.translatable(keys[i]).withStyle(st[i] == stage ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) line.append(Component.literal("  ›  ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(parts.get(i));
        }
        g.drawCenteredString(font, line, cx, y, 0xFFFFFFFF);
    }

    private void selectorBox(GuiGraphics g, int cx, int y, Component label, int color) {
        g.fill(cx - 72, y, cx + 72, y + 20, 0xE0101016);
        g.renderOutline(cx - 72, y, 144, 20, LINE);
        g.drawCenteredString(font, label, cx, y + 6, color);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, Component title, List<Component> lines) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, LINE);
        g.drawString(font, title, x + 8, y + 8, GOLD, false);
        g.fill(x + 8, y + 19, x + w - 8, y + 20, 0x806E5C34);
        int ty = y + 26;
        for (Component c : lines) {
            if (c.getString().isEmpty()) { ty += 5; continue; }
            for (FormattedCharSequence seq : font.split(c, w - 16)) {
                if (ty > y + h - 10) return;
                g.drawString(font, seq, x + 8, ty, TEXT, false);
                ty += 10;
            }
        }
    }

    /** Tu personaje en el centro, con el rasgo elegido y (en la clase) el equipo de muestra. */
    private void renderPlayer(GuiGraphics g, int cx, int mouseX, int mouseY) {
        LocalPlayer p = minecraft.player;
        if (p == null) return;
        ClientAppearance.preview = appearance();
        Inventory inv = p.getInventory();
        int sel = inv.selected;
        ItemStack main = inv.items.get(sel), off = inv.offhand.get(0);
        ItemStack[] armor = {inv.armor.get(0), inv.armor.get(1), inv.armor.get(2), inv.armor.get(3)};
        try {
            if (stage == Stage.CLASS) {
                switch (cls) {
                    case 0 -> {
                        inv.items.set(sel, new ItemStack(ModItems.AQUA_HEARTIA));
                        inv.offhand.set(0, ItemStack.EMPTY);
                        inv.armor.set(0, new ItemStack(ModItems.MK2_BOOTS));
                        inv.armor.set(1, new ItemStack(ModItems.MK2_LEGGINGS));
                        inv.armor.set(2, new ItemStack(ModItems.MK2_CHESTPLATE));
                        inv.armor.set(3, ItemStack.EMPTY);
                    }
                    case 1 -> {
                        inv.items.set(sel, new ItemStack(ModItems.PAULS_SWORD));
                        inv.offhand.set(0, new ItemStack(ModItems.MIGURD_SWORD));
                        for (int i = 0; i < 4; i++) inv.armor.set(i, ItemStack.EMPTY);
                    }
                    default -> {
                        inv.items.set(sel, new ItemStack(Items.BOW));
                        inv.offhand.set(0, ItemStack.EMPTY);
                        for (int i = 0; i < 4; i++) inv.armor.set(i, ItemStack.EMPTY);
                    }
                }
            }
            int top = 38, bottom = height - 80;
            int size = Math.max(30, Math.min(90, (bottom - top) / 2 - 8));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx - 60, top, cx + 60, bottom, size, 0.0625f, mouseX, mouseY, p);
        } finally {
            inv.items.set(sel, main);
            inv.offhand.set(0, off);
            for (int i = 0; i < 4; i++) inv.armor.set(i, armor[i]);
        }
    }

    // ------------------------------------------------------------------ textos
    private Component selectorLabel() {
        return switch (stage) {
            case RACE -> Component.translatable("race.remushoku." + race().key);
            case TRAIT -> race() == Race.BEAST
                    ? Component.translatable("gui.remushoku.cc.animal." + variant)
                    : Component.translatable("gui.remushoku.cc.height", Component.translatable("gui.remushoku.cc.height." + height));
            case CLASS -> Component.translatable("class.remushoku." + CLASSES[cls]);
        };
    }

    private Component leftTitle() {
        return switch (stage) {
            case RACE -> Component.translatable("race.remushoku." + race().key);
            case TRAIT -> Component.translatable("gui.remushoku.cc.trait_title." + race().key);
            case CLASS -> Component.translatable("class.remushoku." + CLASSES[cls]);
        };
    }

    private List<Component> leftText() {
        List<Component> out = new ArrayList<>();
        switch (stage) {
            case RACE -> split(out, "race.remushoku." + race().key + ".lore");
            case TRAIT -> split(out, "gui.remushoku.cc.trait." + race().key);
            case CLASS -> split(out, "class.remushoku." + CLASSES[cls] + ".lore");
        }
        return out;
    }

    private Component rightTitle() {
        return Component.translatable(stage == Stage.CLASS ? "gui.remushoku.cc.features" : "gui.remushoku.cc.stats");
    }

    private List<Component> rightText() {
        List<Component> out = new ArrayList<>();
        if (stage != Stage.CLASS) {
            raceStats(out, race());
            return out;
        }
        split(out, "class.remushoku." + CLASSES[cls] + ".features");
        if (cls == 1) {
            out.add(Component.empty());
            SwordStyle st = SwordStyle.values()[aff];
            out.add(Component.translatable("style.remushoku." + st.key).withStyle(ChatFormatting.GOLD));
            out.add(Component.translatable("style.remushoku." + st.key + ".desc").withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private static void split(List<Component> out, String key) {
        for (String s : Component.translatable(key).getString().split("\n")) out.add(Component.literal(s));
    }

    private static Component num(String key, double value, String fmt, boolean higherIsBetter) {
        ChatFormatting c = value == 0 ? ChatFormatting.GRAY : (value > 0) == higherIsBetter ? ChatFormatting.GREEN : ChatFormatting.RED;
        String v = (value > 0 ? "+" : "") + String.format(fmt, value);
        return Component.translatable(key).append(": ").append(Component.literal(v).withStyle(c));
    }

    private static Component mult(String key, double value) {
        ChatFormatting c = value == 1 ? ChatFormatting.GRAY : value > 1 ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.translatable(key).append(": ").append(Component.literal(String.format("×%.1f", value)).withStyle(c));
    }

    private static void raceStats(List<Component> out, Race r) {
        out.add(Component.translatable("gui.remushoku.cc.stat.health").append(": ")
                .append(Component.literal(String.format("%.0f ❤", r.health / 2)).withStyle(r.health > 20 ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        out.add(num("gui.remushoku.cc.stat.speed", r.speed * 100, "%.0f%%", true));
        out.add(num("gui.remushoku.cc.stat.armor", r.armor, "%.0f", true));
        out.add(num("gui.remushoku.cc.stat.damage", r.damage, "%.0f", true));
        out.add(num("gui.remushoku.cc.stat.attack_speed", r.attackSpeed, "%.1f", true));
        out.add(mult("gui.remushoku.cc.stat.size", r.scale));
        out.add(Component.empty());
        out.add(Component.translatable("gui.remushoku.cc.stat.magic").withStyle(ChatFormatting.GOLD));
        out.add(mult("element.remushoku.fire", r.fire));
        out.add(mult("element.remushoku.water", r.water));
        out.add(mult("element.remushoku.earth", r.earth));
        out.add(mult("element.remushoku.wind", r.wind));
        out.add(Component.empty());
        out.add(Component.translatable("race.remushoku." + r.key + ".skill", "R").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
