package net.zennin.mushokucombat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.zennin.mushokucombat.MushokuCombat;
import net.zennin.mushokucombat.Sheath;
import net.zennin.mushokucombat.net.SheathPayload;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Estado del arma de cada jugador en el cliente (guardada, sacandola, en la mano, guardandola) y la tecla de modo combate.
 * Las posiciones de las fundas y los ticks de agarre vienen de assets/mushoku_combat/sheaths.json (los genera el
 * mismo generador de animaciones, asi el arma pasa de la funda a la mano sin saltos).
 */
public final class ClientSheath {
    public record Slot(String name, float[] pos, float[] rot, float bend, float axis, float[] wrist,
                       String draw, String sheathe, int grab, int drawLength, int release, int sheatheLength) {}

    private static final class State {
        byte phase = Sheath.SHEATHED;
        int t = 0;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static Map<String, Slot> slots;
    private static Map<String, String> items;

    private ClientSheath() {}

    static void init() {
        Sheath.clientDrawn = p -> {
            State s = STATES.get(p.getId());
            return s != null && (s.phase == Sheath.DRAWN || s.phase == Sheath.DRAWING);
        };
    }

    // ---------------------------------------------------------------- datos

    private static void load() {
        if (slots != null) return;
        slots = new HashMap<>();
        items = new HashMap<>();
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(MushokuCombat.id("sheaths.json"));
            if (res.isEmpty()) return;
            try (Reader r = new InputStreamReader(res.get().open())) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                for (var e : root.getAsJsonObject("slots").entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    slots.put(e.getKey(), new Slot(e.getKey(), floats(o, "pos"), floats(o, "rot"), o.get("bend").getAsFloat(),
                            o.get("axis").getAsFloat(), floats(o, "wrist"), o.get("draw").getAsString(), o.get("sheathe").getAsString(),
                            o.get("grab").getAsInt(), o.get("drawLength").getAsInt(), o.get("release").getAsInt(),
                            o.get("sheatheLength").getAsInt()));
                }
                for (var e : root.getAsJsonObject("items").entrySet()) items.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception e) {
            MushokuCombat.LOG.warn("No se pudo leer sheaths.json", e);
        }
    }

    private static float[] floats(JsonObject o, String key) {
        var a = o.getAsJsonArray(key);
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    /** Funda del arma (cintura o espalda), o null si ese objeto no se guarda. */
    public static Slot slotOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        load();
        String name = items.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (name == null) {
            if (stack.getItem() instanceof SwordItem) name = "hip";
            else if (stack.getItem() instanceof TridentItem) name = "back";
            else return null;
        }
        return slots.get(name);
    }

    // ---------------------------------------------------------------- estado

    private static State state(Player p) {
        return STATES.computeIfAbsent(p.getId(), k -> new State());
    }

    /** Si el arma se dibuja en la mano (si no, va en la funda). */
    public static boolean handVisible(Player p) {
        Slot slot = slotOf(p.getMainHandItem());
        if (slot == null) return true;
        State s = STATES.get(p.getId());
        if (s == null) return false;
        return switch (s.phase) {
            case Sheath.DRAWN -> true;
            case Sheath.DRAWING -> s.t >= slot.grab();
            case Sheath.SHEATHING -> s.t < slot.release();
            default -> false;
        };
    }

    public static boolean isSheathed(Player p) {
        State s = STATES.get(p.getId());
        return s == null || s.phase == Sheath.SHEATHED || s.phase == Sheath.SHEATHING;
    }

    /** Cambia el estado de un jugador; con animacion si es DRAWING o SHEATHING. */
    static void set(AbstractClientPlayer p, byte phase) {
        State s = state(p);
        s.phase = phase;
        s.t = 0;
        Slot slot = slotOf(p.getMainHandItem());
        if (slot == null) {
            if (phase == Sheath.DRAWING) s.phase = Sheath.DRAWN;
            if (phase == Sheath.SHEATHING) s.phase = Sheath.SHEATHED;
            return;
        }
        if (phase == Sheath.DRAWING) CombatAnimations.play(p, slot.draw(), 2);
        else if (phase == Sheath.SHEATHING) CombatAnimations.play(p, slot.sheathe(), 3);
    }

    static void receive(Minecraft mc, int entityId, byte phase) {
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof AbstractClientPlayer p) set(p, phase);
        else STATES.computeIfAbsent(entityId, k -> new State()).phase = phase == Sheath.DRAWING ? Sheath.DRAWN : phase == Sheath.SHEATHING ? Sheath.SHEATHED : phase;
    }

    /** El jugador local saca o guarda el arma. */
    public static void toggle(LocalPlayer p) {
        State s = state(p);
        boolean drawn = s.phase == Sheath.DRAWN || s.phase == Sheath.DRAWING;
        setLocal(p, drawn ? Sheath.SHEATHING : Sheath.DRAWING);
    }

    /** Desenvainar si esta guardada (al atacar o al empezar a conjurar). */
    public static void ensureDrawn(LocalPlayer p) {
        if (isSheathed(p)) setLocal(p, Sheath.DRAWING);
    }

    private static void setLocal(LocalPlayer p, byte phase) {
        set(p, phase);
        byte sent = state(p).phase;
        if (ClientPlayNetworking.canSend(SheathPayload.TYPE)) ClientPlayNetworking.send(new SheathPayload(sent));
    }

    static void tick(Minecraft mc) {
        if (mc.level == null) {
            STATES.clear();
            return;
        }
        STATES.entrySet().removeIf(e -> mc.level.getEntity(e.getKey()) == null);
        for (var e : STATES.entrySet()) {
            State s = e.getValue();
            if (s.phase != Sheath.DRAWING && s.phase != Sheath.SHEATHING) continue;
            s.t++;
            Entity ent = mc.level.getEntity(e.getKey());
            Slot slot = ent instanceof Player pl ? slotOf(pl.getMainHandItem()) : null;
            int len = slot == null ? 0 : s.phase == Sheath.DRAWING ? slot.drawLength() - 4 : slot.sheatheLength() - 4;
            if (s.t >= len) {
                s.phase = s.phase == Sheath.DRAWING ? Sheath.DRAWN : Sheath.SHEATHED;
                s.t = 0;
            }
        }
        LocalPlayer p = mc.player;
        if (p == null) return;
        while (CombatClient.COMBAT_MODE.consumeClick()) {
            if (mc.screen == null) toggle(p);
        }
    }

    /** Al principio del tick: con el arma guardada, atacar la saca (y ese clic no pega). */
    static void interceptAttack(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return;
        if (slotOf(p.getMainHandItem()) == null) return;
        State s = state(p);
        boolean blocked = s.phase == Sheath.SHEATHED || s.phase == Sheath.SHEATHING
                || (s.phase == Sheath.DRAWING && s.t < slotOf(p.getMainHandItem()).grab() + 3);
        if (!blocked) return;
        boolean clicked = false;
        while (mc.options.keyAttack.consumeClick()) clicked = true;
        if (mc.options.keyAttack.isDown()) mc.options.keyAttack.setDown(false);
        if (clicked && (s.phase == Sheath.SHEATHED || s.phase == Sheath.SHEATHING)) setLocal(p, Sheath.DRAWING);
    }
}
