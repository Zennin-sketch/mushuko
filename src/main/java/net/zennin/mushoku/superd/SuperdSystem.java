package net.zennin.mushoku.superd;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.race.Race;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Guerreros Superd. Como en Mushoku Tensei, los Superd no usan magia: no tienen clase ni mana (no conjuran, no toman
 * pociones de mana ni usan la Mesa de Investigacion). Pelean con la lanza y con la joya roja de la frente, el "tercer
 * ojo" que siente a los seres vivos aunque esten detras de las paredes.
 * <ul>
 *   <li>Z — Embestida: se lanza hacia adelante atravesando a todos en el camino.</li>
 *   <li>X — Lanza giratoria: gira la lanza alrededor y golpea y empuja a todos los cercanos.</li>
 *   <li>C — Lanzamiento: arroja la lanza (una copia de la que tiene en la mano), que vuelve sola.</li>
 *   <li>R — Furia Superd: el grito de guerra de la tribu (fuerza, velocidad y resistencia por un rato).</li>
 *   <li>G — Tercer ojo: lo maneja el cliente (ve a los seres vivos a traves de las paredes, sin mana).</li>
 * </ul>
 * Z, X y C necesitan una lanza en la mano. No gastan mana: solo tienen tiempo de espera.
 */
public final class SuperdSystem {
    public static final int LUNGE = 0, WHIRL = 1, THROW = 2, FURY = 3;
    /** Espera de cada habilidad (ticks). */
    public static final int[] COOLDOWN = {120, 160, 200, 1800};
    private static final Map<UUID, long[]> READY = new HashMap<>();

    private SuperdSystem() {}

    public static boolean isSuperd(Player p) {
        if (p.level().isClientSide) return "superd".equals(net.zennin.mushoku.client.ClientRaceState.data.race());
        return ModAttachments.race(p).raceOrNull() == Race.SUPERD;
    }

    /** Lanzas: la de Ruijerd, las lanzas Superd, la maldita de Laplace y cualquier otra lanza (por su nombre). */
    public static boolean isSpear(ItemStack s) {
        return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("spear");
    }

    /** Aviso comun cuando un Superd intenta usar algo que requiere mana. */
    public static void denyMagic(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("msg.remushoku.superd_no_magic").withStyle(ChatFormatting.DARK_GREEN), true);
    }

    public static long[] ready(Player p) { return READY.computeIfAbsent(p.getUUID(), k -> new long[4]); }

    public static void use(ServerPlayer p, int skill) {
        if (!isSuperd(p) || skill < 0 || skill > 3) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        long[] r = ready(p);
        if (r[skill] > now && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("msg.remushoku.superd_cooldown",
                    Component.translatable("superd.remushoku.skill" + skill), (r[skill] - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ItemStack spear = p.getMainHandItem();
        if (skill != FURY && !isSpear(spear)) {
            p.displayClientMessage(Component.translatable("msg.remushoku.superd_need_spear").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean ok = switch (skill) {
            case LUNGE -> lunge(p, level);
            case WHIRL -> whirl(p, level);
            case THROW -> throwSpear(p, level, spear);
            default -> fury(p, level);
        };
        if (!ok) return;
        r[skill] = now + COOLDOWN[skill];
        sync(p);
    }

    private static float damage(ServerPlayer p) { return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE); }

    /** Z: embestida de 7 bloques; golpea una vez a cada uno que atraviesa. */
    private static boolean lunge(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.35, look.y)), look.z).normalize();
        p.setDeltaMovement(dir.scale(1.55).add(0, 0.12, 0));
        p.hurtMarked = true;
        net.zennin.mushoku.magic.Escape.grant(p);
        net.zennin.mushoku.network.ModNetworking.playAnim(p, "superd_lunge");
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1f, 1.2f);
        Set<Integer> hit = new HashSet<>();
        float dmg = damage(p) * 1.3f;
        Tasks.repeat(0, 1, 8, t -> {
            if (!p.isAlive()) return false;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + dir.x * 1.5, p.getY() + 1.1, p.getZ() + dir.z * 1.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(1.2).expandTowards(dir.scale(1.5)),
                    e -> e != p && e.isAlive() && !hit.contains(e.getId()))) {
                hit.add(e.getId());
                e.hurt(level.damageSources().playerAttack(p), dmg);
                e.knockback(0.6, -dir.x, -dir.z);
                level.playSound(null, e.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1f, 1f);
            }
            return true;
        });
        return true;
    }

    /** X: giro de lanza alrededor (4.5 bloques), empuja hacia afuera. */
    private static boolean whirl(ServerPlayer p, ServerLevel level) {
        net.zennin.mushoku.network.ModNetworking.playAnim(p, "superd_whirl");
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
        float dmg = damage(p);
        Tasks.later(5, () -> {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 3, p.getY() + 1, p.getZ() + Math.sin(a) * 3, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5, 1.5, 4.5),
                    e -> e != p && e.isAlive() && e.distanceTo(p) < 5)) {
                e.hurt(level.damageSources().playerAttack(p), dmg);
                Vec3 out = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                e.knockback(1.2, -out.x, -out.z);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1f, 0.6f);
        });
        return true;
    }

    /** C: arroja una copia de la lanza que vuelve sola (no se pierde la de la mano). */
    private static boolean throwSpear(ServerPlayer p, ServerLevel level, ItemStack spear) {
        net.zennin.mushoku.network.ModNetworking.playAnim(p, "superd_throw");
        Tasks.later(6, () -> {
            if (!p.isAlive()) return;
            ThrownSpearEntity s = new ThrownSpearEntity(level, p, spear.copyWithCount(1), damage(p) * 1.6f);
            s.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 2.6f, 0.5f);
            level.addFreshEntity(s);
            level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2f, 0.9f);
        });
        return true;
    }

    /** R: Furia Superd (grito de guerra). Con la lanza maldita de Laplace es mas fuerte pero te nubla la vista. */
    private static boolean fury(ServerPlayer p, ServerLevel level) {
        boolean cursed = net.zennin.mushoku.registry.ModItems.LAPLACE_SPEAR != null && p.getMainHandItem().is(net.zennin.mushoku.registry.ModItems.LAPLACE_SPEAR);
        int amp = cursed ? 2 : 1;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, amp));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
        if (cursed) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        net.zennin.mushoku.network.ModNetworking.playAnim(p, "superd_roar");
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 1.3f);
        DustParticleOptions red = new DustParticleOptions(new org.joml.Vector3f(cursed ? 0.5f : 0.9f, 0.05f, cursed ? 0.6f : 0.1f), 1.6f);
        level.sendParticles(red, p.getX(), p.getY() + 1, p.getZ(), 60, 1.2, 1, 1.2, 0);
        // los enemigos cercanos se asustan (lentitud y debilidad breves)
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(8),
                e -> e != p && e instanceof net.minecraft.world.entity.monster.Enemy)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        return true;
    }

    /** Manda al cliente cuanto falta de cada habilidad (para el HUD). */
    public static void sync(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        long[] r = ready(p);
        int[] left = new int[4];
        for (int i = 0; i < 4; i++) left[i] = (int) Math.max(0, r[i] - now);
        net.zennin.mushoku.network.ModNetworking.send(p, new net.zennin.mushoku.network.SuperdSyncPayload(left[0], left[1], left[2], left[3]));
    }

    public static void init() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> READY.remove(h.getPlayer().getUUID()));
    }
}
