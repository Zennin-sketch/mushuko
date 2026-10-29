package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.item.SpellData;
import net.zennin.mushoku.network.RefinePayloads;
import net.zennin.mushoku.refine.Refining;
import net.zennin.mushoku.registry.ModItems;

/**
 * Menu rúnico del refinado: la piedra al centro de un circulo de runas que gira; la parte dorada del anillo es la
 * probabilidad de exito. Al imbuir, la ruleta gira, frena y la aguja cae en dorado (sube de nivel) o en rojo (se rompe).
 */
public class RefineScreen extends Screen {
    private static final Style RUNES = Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt"));
    private static final String GLYPHS = "abcdefghijklmnopqrstuvwxyz";
    private static RefineScreen open;

    private BlockPos pos;
    private int tier, dust, mana, playerMana;
    private float chance;
    // tirada
    private boolean spinning, success, done;
    private float spinStart, finalAngle, chanceAtSpin;
    private Button addBtn, rollBtn;

    private RefineScreen() { super(Component.translatable("gui.remushoku.refine.title")); }

    public static void onOpen(RefinePayloads.Open o) {
        Minecraft mc = Minecraft.getInstance();
        RefineScreen s = open != null && mc.screen == open ? open : new RefineScreen();
        s.pos = o.pos(); s.tier = o.tier(); s.dust = o.dust(); s.chance = o.chance(); s.mana = o.mana(); s.playerMana = o.playerMana();
        if (s != mc.screen) { open = s; mc.setScreen(s); }
        else s.refreshButtons();
    }

    public static void onSpin(RefinePayloads.Spin sp) {
        if (open == null || Minecraft.getInstance().screen != open) return;
        RefineScreen s = open;
        s.spinning = true; s.done = false; s.success = sp.success(); s.chanceAtSpin = sp.chance();
        s.spinStart = s.now();
        // donde cae la aguja: dentro de la parte dorada (0 .. chance) o de la roja, sin tocar los bordes
        float r = 0.15f + 0.7f * Minecraft.getInstance().level.random.nextFloat();
        float frac = sp.success() ? r * sp.chance() : sp.chance() + r * (1 - sp.chance());
        s.finalAngle = 360f * 5 + frac * 360f;
        s.refreshButtons();
    }

    public static void onResult(RefinePayloads.Result r) {
        if (open == null || Minecraft.getInstance().screen != open || !open.pos.equals(r.pos())) return;
        open.done = true; open.spinning = false; open.success = r.success();
        Minecraft mc = Minecraft.getInstance();
        if (r.success()) mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.4f, 0.6f));
        open.refreshButtons();
    }

    private float now() { return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true); }

    private int dustInInventory() {
        var inv = Minecraft.getInstance().player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.REFINING_DUST)) n += inv.getItem(i).getCount();
        return n;
    }

    @Override
    protected void init() {
        int cx = width / 2, by = height / 2 + 92;
        addBtn = addRenderableWidget(Button.builder(Component.empty(), b -> send(RefinePayloads.ADD_DUST)).bounds(cx - 154, by, 150, 20).build());
        rollBtn = addRenderableWidget(Button.builder(Component.translatable("gui.remushoku.refine.roll"), b -> send(RefinePayloads.ROLL))
                .bounds(cx + 4, by, 150, 20).build());
        refreshButtons();
    }

    private void refreshButtons() {
        if (addBtn == null) return;
        boolean creative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild;
        int inv = dustInInventory();
        addBtn.setMessage(Component.translatable("gui.remushoku.refine.add", creative ? "∞" : String.valueOf(inv)));
        boolean busy = spinning || done;
        addBtn.active = !busy && dust < Refining.MAX_DUST && (creative || inv > 0) && tier < 6;
        rollBtn.active = !busy && tier < 6 && playerMana >= mana;
        if (done) {
            rollBtn.active = success && tier + 1 < 6;
            rollBtn.setMessage(Component.translatable(success ? "gui.remushoku.refine.again" : "gui.remushoku.refine.broken"));
        } else rollBtn.setMessage(Component.translatable("gui.remushoku.refine.roll"));
    }

    private void send(int action) {
        if (done && action == RefinePayloads.ROLL) { // otra vez: pide los datos nuevos (la piedra ya subio)
            done = false;
            ClientPlayNetworking.send(new RefinePayloads.Action(pos, -1));
            return;
        }
        ClientPlayNetworking.send(new RefinePayloads.Action(pos, action));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xE00A0820, 0xF0040310);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int cx = width / 2, cy = height / 2 - 6;
        float t = now();
        var font = this.font;
        // ---- titulo rúnico
        g.drawCenteredString(font, Component.literal("ᚱ ").append(title).append(" ᚱ"), cx, cy - 104, 0xFFB9E6FF);
        g.drawCenteredString(font, Component.literal("refinatio lapidis").withStyle(RUNES), cx, cy - 92, 0xFF5D7FB0);
        // ---- anillos de runas que giran despacio
        ring(g, cx, cy, 86, t * 0.6f, 0xFF3A5A8C, 28);
        ring(g, cx, cy, 70, -t * 0.9f, 0xFF6EA8E0, 22);
        // ---- ruleta: dorado = exito, rojo = se rompe
        float c = spinning || done ? chanceAtSpin : chance;
        float angle = 0;
        if (spinning || done) {
            float k = done ? 1f : Math.min(1f, (t - spinStart) / Refining.SPIN_TICKS);
            float e = 1f - (float) Math.pow(1f - k, 3);
            angle = finalAngle * e;
        }
        wheel(g, cx, cy, 52, c, angle);
        // aguja arriba
        g.fill(cx - 1, cy - 64, cx + 2, cy - 50, 0xFFFFFFFF);
        g.fill(cx - 3, cy - 66, cx + 4, cy - 63, 0xFFFFE7A0);
        // ---- piedra en el centro (late)
        float pulse = 1f + 0.06f * Mth.sin(t * 0.25f);
        ItemStack from = new ItemStack(SpellData.stoneFor(Math.max(1, tier)));
        g.pose().pushPose();
        g.pose().translate(cx, cy, 200);
        g.pose().scale(2.2f * pulse, 2.2f * pulse, 1);
        g.renderItem(from, -8, -8);
        g.pose().popPose();
        // ---- datos
        int lx = cx - 230, ly = cy - 40;
        if (lx < 6) lx = 6;
        Component fromName = Component.translatable(from.getDescriptionId());
        Component toName = tier < 6 ? Component.translatable(SpellData.stoneFor(tier + 1).getDescriptionId()) : Component.literal("-");
        g.drawString(font, Component.translatable("gui.remushoku.refine.from"), lx, ly, 0xFF8FA8C8);
        g.drawString(font, fromName, lx, ly + 11, 0xFFE8F2FF);
        g.drawString(font, Component.translatable("gui.remushoku.refine.to"), lx, ly + 28, 0xFF8FA8C8);
        g.drawString(font, toName, lx, ly + 39, 0xFFFFE08A);
        g.drawString(font, Component.translatable("gui.remushoku.refine.mana"), lx, ly + 56, 0xFF8FA8C8);
        boolean enough = playerMana >= mana;
        g.drawString(font, Component.literal(mana + (playerMana == Integer.MAX_VALUE ? "" : "  (" + playerMana + ")")), lx, ly + 67, enough ? 0xFF7FD8FF : 0xFFFF6A6A);
        int rx = cx + 120;
        g.drawString(font, Component.translatable("gui.remushoku.refine.chance"), rx, ly, 0xFF8FA8C8);
        g.drawString(font, Component.literal(Math.round(c * 100) + "%"), rx, ly + 11, 0xFFFFE08A);
        g.drawString(font, Component.translatable("gui.remushoku.refine.cap", Math.round(Refining.CAP * 100)), rx, ly + 24, 0xFF6F86A6);
        g.drawString(font, Component.translatable("gui.remushoku.refine.dust"), rx, ly + 44, 0xFF8FA8C8);
        for (int i = 0; i < Refining.MAX_DUST; i++) {
            int sx = rx + i * 14, sy = ly + 57;
            boolean full = i < dust;
            diamond(g, sx + 5, sy + 5, 5, full ? 0xFFC59BFF : 0xFF2B2440);
            if (full) diamond(g, sx + 5, sy + 5, 2, 0xFFFFFFFF);
        }
        // ---- resultado
        if (done) {
            Component msg = Component.translatable(success ? "gui.remushoku.refine.success" : "gui.remushoku.refine.fail");
            g.pose().pushPose();
            g.pose().translate(cx, cy + 70, 300);
            g.pose().scale(1.6f, 1.6f, 1);
            g.drawCenteredString(font, msg, 0, 0, success ? 0xFFFFE08A : 0xFFFF5A5A);
            g.pose().popPose();
        } else if (spinning) {
            g.drawCenteredString(font, Component.literal("…").append(Component.translatable("gui.remushoku.refine.rolling")), cx, cy + 70, 0xFFB9E6FF);
        }
    }

    /** Anillo de runas (letras del alfabeto de la mesa de encantamientos) girando. */
    private void ring(GuiGraphics g, int cx, int cy, int r, float deg, int color, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(deg + i * 360.0 / n);
            int x = cx + (int) Math.round(Math.sin(a) * r), y = cy - (int) Math.round(Math.cos(a) * r);
            g.drawString(font, Component.literal(String.valueOf(GLYPHS.charAt(i % GLYPHS.length()))).withStyle(RUNES), x - 3, y - 4, color, false);
        }
        for (int i = 0; i < 180; i++) {
            double a = Math.toRadians(i * 2.0);
            int x = cx + (int) Math.round(Math.sin(a) * (r + 7)), y = cy - (int) Math.round(Math.cos(a) * (r + 7));
            g.fill(x, y, x + 1, y + 1, (color & 0x00FFFFFF) | 0x66000000);
        }
    }

    /** Ruleta: sector dorado (exito) y rojo (rotura), girada 'angle' grados. */
    private void wheel(GuiGraphics g, int cx, int cy, int r, float chance, float angle) {
        for (int d = 0; d < 360; d += 2) {
            float frac = d / 360f;
            boolean gold = frac < chance;
            int col = gold ? 0xFFE8C25A : 0xFF8E2A3A;
            double a = Math.toRadians(d - angle);
            for (int k = r - 9; k <= r; k += 2) {
                int x = cx + (int) Math.round(Math.sin(a) * k), y = cy - (int) Math.round(Math.cos(a) * k);
                g.fill(x, y, x + 2, y + 2, col);
            }
        }
        // marcas cada 10%
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(i * 36 - angle);
            int x = cx + (int) Math.round(Math.sin(a) * (r + 2)), y = cy - (int) Math.round(Math.cos(a) * (r + 2));
            g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
        }
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, col);
        }
    }

    @Override
    public void removed() {
        if (open == this) open = null;
        super.removed();
    }
}
