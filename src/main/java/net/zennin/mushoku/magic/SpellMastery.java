package net.zennin.mushoku.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.data.ModAttachments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dominio de cada hechizo (nivel 1 a 20): sube con las veces que lo lanzas.
 * <ul>
 *   <li>Cada nivel baja el mana que cuesta (2.5 % por nivel).</li>
 *   <li>Cada 5 niveles (5, 10, 15, 20) pega mas fuerte (+25 % cada vez).</li>
 *   <li>Del 10 al 15 y del 15 al 20 hace falta un mana maximo alto (la curva del mago se empina).</li>
 *   <li>Los hechizos Santo, Rey e Imperial piden un mana maximo minimo para poder usarlos.</li>
 *   <li>La magia de agua en el Nether cuesta un 10 % mas.</li>
 * </ul>
 * Las cuentas son las mismas en el cliente (libro de hechizos) y en el servidor.
 */
public final class SpellMastery {
    public static final int MAX = 20, FREE_MAX = 10;
    private SpellMastery() {}

    // ------------------------------------------------------------------ cuentas

    /** Lanzamientos para pasar del nivel n al n+1 (los hechizos caros piden menos). */
    public static int step(Spell s, int n) {
        float f = switch (s.level.tier) { case 1 -> 1f; case 2 -> 0.9f; case 3 -> 0.8f; case 4 -> 0.6f; case 5 -> 0.5f; default -> 0.4f; };
        float base = 3 + 2 * n;
        if (n >= FREE_MAX) base *= 1.5f;
        return Math.max(1, (int) Math.ceil(base * f));
    }

    /** Lanzamientos acumulados para llegar a un nivel. */
    public static int usesFor(Spell s, int level) {
        int t = 0;
        for (int n = 1; n < level; n++) t += step(s, n);
        return t;
    }

    /** Mana maximo minimo para lanzar el hechizo (0 = sin requisito). */
    public static int useRequirement(Spell s) {
        return switch (s.level) { case SAINT -> 1500; case KING -> 3000; case IMPERIAL -> 5000; default -> 0; };
    }

    /** Mana maximo para pasar del 10 al 11 y del 15 al 16. */
    public static int gate(Spell s, int level) {
        int g1 = 1500 + 500 * s.level.tier + useRequirement(s);
        if (level > 15) return g1 * 2;
        if (level > FREE_MAX) return g1;
        return 0;
    }

    /** Nivel con estos lanzamientos y este mana maximo. */
    public static int level(Spell s, int uses, double maxMana) {
        int lvl = 1;
        while (lvl < MAX && uses >= usesFor(s, lvl + 1) && maxMana >= gate(s, lvl + 1)) lvl++;
        return lvl;
    }

    /** Multiplicador de dano: x1.25 en el 5, x1.5 en el 10, x1.75 en el 15 y x2 en el 20. */
    public static float damageMult(int level) { return 1f + 0.25f * (level / 5); }

    /** Mana que cuesta a este nivel (y +10 % para el agua en el Nether). */
    public static double cost(Spell s, int level, boolean nether) {
        double c = s.level.cost * (1 - 0.025 * (level - 1));
        if (nether && s.element == Element.WATER) c *= 1.10;
        return Math.round(c * 10) / 10.0;
    }

    /** Dano base de cada hechizo (lo que dice el libro; -1 = no hace dano directo). */
    public static float baseDamage(Spell s) {
        return switch (s) {
            case WATER_BALL, HEAT_HAND -> 3f;
            case ICE_SMASH, FIREBALL, ROCK_BULLET, WATER_ARROW, WIND_SLICE, SONIC_BOOM, FLOOD_FLUSH -> 4f;
            case ICE_PILLAR, EARTH_PILLAR -> 5f;
            case WATER_CANNON, ICICLE_LANCE, BLIZZARD_STORM, FLAME_SLICE, STONE_CANNON, EARTH_LANCE, TORNADO_IMPACT -> 6f;
            case EX_FLAME, SONIC_BLAST -> 8f;
            case ABSOLUTE_ZERO, FLASH_OVER -> 12f;
            case EXODUS_FLAME, MOUNTAIN, EYE_OF_THE_STORM -> 15f;
            case MAGMA_GUSH -> 4f;
            case SANDSTORM, TORNADO, FROST_WIND -> 2f;
            case VIOLENT_STORM -> 3f;
            case FIRE_RAIN -> 5f;
            case FREEZING_RAIN, SPIKE_RAIN, STONE_BLADE -> 7f;
            case EXPLOSION -> 10f;
            case MINEFIELD -> 8f;
            case NUCLEAR_STRIKE -> 40f;
            case LIGHTNING, CUMULONIMBUS -> 5f;
            default -> -1f;
        };
    }

    // ------------------------------------------------------------------ jugador

    public static int uses(Player p, Spell s) {
        if (p.level().isClientSide) return net.zennin.mushoku.client.ClientSpellXp.uses(s.id);
        return ModAttachments.spellXp(p).getOrDefault(s.id, 0);
    }

    public static int level(Player p, Spell s) {
        double max = p.level().isClientSide ? net.zennin.mushoku.client.ClientManaState.maxMana : ModAttachments.get(p).maxMana();
        return level(s, uses(p, s), max);
    }

    public static double cost(Player p, Spell s) {
        return cost(s, level(p, s), p.level().dimension() == Level.NETHER);
    }

    /** Puede lanzarlo (mana maximo suficiente para los Santo / Rey / Imperial). Avisa si no. */
    public static boolean canUse(ServerPlayer p, Spell s) {
        if (p.getAbilities().instabuild) return true;
        int req = useRequirement(s);
        if (req <= 0 || ModAttachments.get(p).maxMana() >= req) return true;
        p.displayClientMessage(Component.translatable("msg.remushoku.mastery_need_mana", req).withStyle(ChatFormatting.RED), true);
        return false;
    }

    /** Un lanzamiento mas: si sube de nivel, lo avisa. */
    public static void gain(ServerPlayer p, Spell s) {
        if (p instanceof net.fabricmc.fabric.api.entity.FakePlayer) return;
        int before = level(p, s);
        Map<String, Integer> m = new java.util.HashMap<>(ModAttachments.spellXp(p));
        m.merge(s.id, 1, Integer::sum);
        ModAttachments.setSpellXp(p, m);
        int after = level(p, s);
        if (after > before) {
            p.displayClientMessage(Component.translatable("msg.remushoku.mastery_up", Component.translatable(s.translationKey()), after)
                    .withStyle(after % 5 == 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA), false);
            p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, after % 5 == 0 ? 1.4f : 1.9f);
        } else if (after < MAX && after >= FREE_MAX && m.get(s.id) >= usesFor(s, after + 1) && m.get(s.id) == usesFor(s, after + 1)) {
            p.displayClientMessage(Component.translatable("msg.remushoku.mastery_gate", Component.translatable(s.translationKey()), gate(s, after + 1))
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, net.zennin.mushoku.network.SpellXpPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new net.zennin.mushoku.network.SpellXpPayload(
                    new java.util.HashMap<>(ModAttachments.spellXp(p)), new java.util.ArrayList<>(ModAttachments.knownSpells(p))));
    }

    // ------------------------------------------------------------------ potencia del hechizo en curso

    private static final Map<UUID, Float> POWER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LEVEL = new ConcurrentHashMap<>();

    /** Al empezar un hechizo: guarda su potencia (la usan el dano directo y los proyectiles, aun los que llegan despues). */
    public static void begin(Player p, Spell s) {
        int lvl = p instanceof net.fabricmc.fabric.api.entity.FakePlayer ? 1 : level(p, s);
        LEVEL.put(p.getUUID(), lvl);
        POWER.put(p.getUUID(), damageMult(lvl));
    }

    /** Multiplicador del ultimo hechizo de este jugador. */
    public static float mult(net.minecraft.world.entity.Entity p) {
        return p == null ? 1f : POWER.getOrDefault(p.getUUID(), 1f);
    }

    /** Nivel del ultimo hechizo de este jugador. */
    public static int castLevel(net.minecraft.world.entity.Entity p) {
        return p == null ? 1 : LEVEL.getOrDefault(p.getUUID(), 1);
    }

    /** Dano ajustado al nivel del hechizo. */
    public static float dmg(net.minecraft.world.entity.Entity p, float base) { return base * mult(p); }

    public static void init() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, s, server) -> sync(h.getPlayer()));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> {
            POWER.remove(h.getPlayer().getUUID());
            LEVEL.remove(h.getPlayer().getUUID());
        });
    }
}
