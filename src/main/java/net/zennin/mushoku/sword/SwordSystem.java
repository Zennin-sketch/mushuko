package net.zennin.mushoku.sword;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.data.SwordData;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.network.SwordSyncPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Progresion del espadachin: afinidad, puntos de habilidad (1 por monstruo hostil, 5 por bandido),
 * aprender y asignar habilidades, Touki por niveles y las habilidades pasivas de defensa.
 */
public final class SwordSystem {
    public static final int TOUKI_COST = 100, TOUKI_MAX = 10;
    private static final ResourceLocation AFF_ATTACK = MushokuMod.id("affinity_attack");
    private static final ResourceLocation AFF_HEALTH = MushokuMod.id("affinity_health");
    private static final ResourceLocation AFF_SPEED = MushokuMod.id("affinity_speed");
    private static final ResourceLocation TOUKI_ATTACK = MushokuMod.id("touki_attack");
    private static final ResourceLocation TOUKI_HEALTH = MushokuMod.id("touki_health");
    private static final ResourceLocation TOUKI_SPEED = MushokuMod.id("touki_speed");

    /** Fin del efecto Fluir (bloquea y refleja el proximo golpe). */
    static final Map<UUID, Long> FLOW = new HashMap<>();
    private static final Map<UUID, Long> DEFLECT_CD = new HashMap<>();
    private static final Map<UUID, Long> DUEL_CD = new HashMap<>();
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    private SwordSystem() {}

    public static boolean isSwordsman(Player p) {
        return SpellCasting.isSwordsmanClass(ModAttachments.get(p).playerClass());
    }

    public static void sync(ServerPlayer p) {
        ServerPlayNetworking.send(p, new SwordSyncPayload(ModAttachments.sword(p)));
    }

    private static void msg(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    private static void msg(ServerPlayer p, String key, ChatFormatting color) {
        msg(p, Component.translatable(key).withStyle(color));
    }

    // ---------- Acciones del menu ----------

    public static void chooseAffinity(ServerPlayer p, String key) {
        SwordStyle style = SwordStyle.byKey(key);
        SwordData d = ModAttachments.sword(p);
        if (style == null || !isSwordsman(p) || !d.style().isEmpty()) return;
        ModAttachments.setSword(p, d.withStyle(style.key).learn(SwordSkill.styleSkill(style).id));
        p.displayClientMessage(Component.translatable("msg.remushoku.affinity_chosen",
                Component.translatable("style.remushoku." + style.key)).withStyle(ChatFormatting.GOLD), false);
        refresh(p);
        sync(p);
    }

    public static void learn(ServerPlayer p, String id) {
        SwordSkill s = SwordSkill.byId(id);
        SwordData d = ModAttachments.sword(p);
        if (s == null || !isSwordsman(p) || d.style().isEmpty() || d.knows(id)) return;
        boolean creative = p.getAbilities().instabuild;
        String why = cantLearn(d, s, creative);
        if (why != null) { msg(p, why, ChatFormatting.RED); return; }
        ModAttachments.setSword(p, d.withPoints(creative ? d.points() : d.points() - s.cost).learn(id));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        msg(p, Component.translatable("msg.remushoku.skill_learned", Component.translatable(s.translationKey())).withStyle(ChatFormatting.GOLD));
        refresh(p);
        sync(p);
    }

    /** null si se puede aprender; si no, la clave del mensaje con el motivo. */
    public static String cantLearn(SwordData d, SwordSkill s) { return cantLearn(d, s, false); }

    /** creative: en creativo los puntos son infinitos (no se piden ni se gastan). */
    public static String cantLearn(SwordData d, SwordSkill s, boolean creative) {
        boolean affinity = s.style.key.equals(d.style());
        if (s.kind != SwordSkill.Kind.STYLE && !d.knows(SwordSkill.styleSkill(s.style).id)) return "msg.remushoku.need_style";
        if (!affinity && s.tier > 3) return "msg.remushoku.not_affinity";
        if (s.tier > 2) {
            boolean prev = false;
            for (SwordSkill o : SwordSkill.of(s.style)) if (o.tier == s.tier - 1 && d.knows(o.id)) prev = true;
            if (!prev) return "msg.remushoku.need_previous";
        }
        if (!creative && d.points() < s.cost) return "msg.remushoku.no_points";
        return null;
    }

    public static void bind(ServerPlayer p, int slot, String id) {
        SwordSkill s = SwordSkill.byId(id);
        SwordData d = ModAttachments.sword(p);
        if (s == null || slot < 0 || slot > 2 || s.kind != SwordSkill.Kind.ACTIVE || !d.knows(id)) return;
        ModAttachments.setSword(p, d.bind(slot, id));
        sync(p);
    }

    public static void toggle(ServerPlayer p, String id) {
        SwordSkill s = SwordSkill.byId(id);
        SwordData d = ModAttachments.sword(p);
        if (s == null || s.kind != SwordSkill.Kind.TOGGLE || !d.knows(id)) return;
        SwordData n = d.toggle(id);
        ModAttachments.setSword(p, n);
        msg(p, Component.translatable(n.isOn(id) ? "msg.remushoku.toggle_on" : "msg.remushoku.toggle_off",
                Component.translatable(s.translationKey())).withStyle(ChatFormatting.AQUA));
        sync(p);
    }

    public static void buyTouki(ServerPlayer p) {
        SwordData d = ModAttachments.sword(p);
        if (!isSwordsman(p) || d.style().isEmpty()) return;
        if (d.touki() >= TOUKI_MAX) { msg(p, "msg.remushoku.touki_max", ChatFormatting.GRAY); return; }
        boolean creative = p.getAbilities().instabuild;
        if (!creative && d.points() < TOUKI_COST) { msg(p, "msg.remushoku.no_points", ChatFormatting.RED); return; }
        ModAttachments.setSword(p, d.withPoints(creative ? d.points() : d.points() - TOUKI_COST).withTouki(d.touki() + 1));
        ManaData m = ModAttachments.get(p);
        ModAttachments.set(p, m.withMaxMana(m.maxMana() + 100)); // como el original: cada nivel suma 100 de mana maximo
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.2f);
        msg(p, Component.translatable("msg.remushoku.touki_up", d.touki() + 1).withStyle(ChatFormatting.GOLD));
        refresh(p);
        sync(p);
        ModNetworking.syncMana(p);
    }

    public static void toggleTouki(ServerPlayer p) {
        SwordData d = ModAttachments.sword(p);
        if (!isSwordsman(p) || d.touki() <= 0) { msg(p, "msg.remushoku.touki_none", ChatFormatting.GRAY); return; }
        boolean on = !d.toukiOn();
        ModAttachments.setSword(p, d.withToukiOn(on));
        if (on) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1f, 0.6f);
        msg(p, on ? "msg.remushoku.touki_on" : "msg.remushoku.touki_off", on ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        refresh(p);
        sync(p);
    }

    /** Orbe de cambio de clase: se borra la afinidad y se devuelven los puntos gastados. */
    public static void reset(ServerPlayer p) {
        SwordData d = ModAttachments.sword(p);
        int refund = d.points() + d.touki() * TOUKI_COST;
        SwordStyle aff = SwordStyle.byKey(d.style());
        for (String id : d.learned()) {
            SwordSkill s = SwordSkill.byId(id);
            if (s != null && !(aff != null && s == SwordSkill.styleSkill(aff))) refund += s.cost;
        }
        ModAttachments.setSword(p, SwordData.DEFAULT.withPoints(refund));
        refresh(p);
        sync(p);
    }

    // ---------- Atributos: afinidad y Touki ----------

    public static void refresh(ServerPlayer p) {
        SwordData d = ModAttachments.sword(p);
        boolean sw = isSwordsman(p);
        SwordStyle style = sw ? SwordStyle.byKey(d.style()) : null;
        set(p, Attributes.ATTACK_DAMAGE, AFF_ATTACK, style == SwordStyle.SWORD_GOD ? 3 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MAX_HEALTH, AFF_HEALTH, style == SwordStyle.WATER_GOD ? 4 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MOVEMENT_SPEED, AFF_SPEED, style == SwordStyle.NORTH_GOD ? 0.2 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        int t = sw && d.toukiOn() ? d.touki() : 0;
        set(p, Attributes.ATTACK_DAMAGE, TOUKI_ATTACK, t * (style == SwordStyle.SWORD_GOD ? 1.5 : 1.0), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MAX_HEALTH, TOUKI_HEALTH, t * (style == SwordStyle.WATER_GOD ? 3 : 2), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MOVEMENT_SPEED, TOUKI_SPEED, t * (style == SwordStyle.NORTH_GOD ? 0.05 : 0.03), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur != null && cur.amount() == amount) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    // ---------- Eventos ----------

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            refresh(handler.getPlayer());
            sync(handler.getPlayer());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                SwordData d = ModAttachments.sword(p);
                if (d.toukiOn() && isSwordsman(p)) {
                    ManaData m = ModAttachments.get(p);
                    double drain = 2 + d.touki();
                    if (m.mana() < drain) {
                        ModAttachments.setSword(p, d.withToukiOn(false));
                        msg(p, "msg.remushoku.touki_no_mana", ChatFormatting.RED);
                        sync(p);
                    } else if (!p.getAbilities().instabuild) {
                        ModAttachments.set(p, m.withMana(m.mana() - drain));
                    }
                }
                refresh(p);
            }
        });

        // Puntos de habilidad al matar
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer p) || !isSwordsman(p) || entity instanceof Player) return;
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            int gain = type.contains("bandit") ? 5 : entity instanceof Enemy ? 1 : 0;
            if (gain == 0) return;
            SwordData d = ModAttachments.sword(p);
            ModAttachments.setSword(p, d.withPoints(d.points() + gain));
            sync(p);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SwordSystem::allowDamage);

        // Doble empuñadura: la mano izquierda tambien golpea
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide || !(player instanceof ServerPlayer p) || !(entity instanceof LivingEntity target)) return InteractionResult.PASS;
            SwordData d = ModAttachments.sword(p);
            if (!isSwordsman(p) || !d.knows(SwordSkill.DUEL_WIELDING.id) || !d.isOn(SwordSkill.DUEL_WIELDING.id)) return InteractionResult.PASS;
            if (!SwordSkills.isWeapon(p.getOffhandItem(), d)) return InteractionResult.PASS;
            long now = p.server.getTickCount();
            if (DUEL_CD.getOrDefault(p.getUUID(), 0L) > now) return InteractionResult.PASS;
            DUEL_CD.put(p.getUUID(), now + 25);
            float dmg = (float) (SwordSkills.itemDamage(p.getOffhandItem()) * 0.8);
            Tasks.later(5, () -> {
                if (!target.isAlive() || !p.isAlive()) return;
                p.swing(InteractionHand.OFF_HAND, true);
                target.invulnerableTime = 0;
                target.hurt(p.damageSources().playerAttack(p), dmg);
                p.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.2f);
            });
            return InteractionResult.PASS;
        });
    }

    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (REENTRY.get() || !(victim instanceof ServerPlayer p) || !isSwordsman(p)) return true;
        SwordData d = ModAttachments.sword(p);
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        long now = p.server.getTickCount();
        ServerLevel level = p.serverLevel();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;

        // Fluir: bloquea y devuelve el proximo golpe
        Long flow = FLOW.get(p.getUUID());
        if (flow != null && flow > now && attacker instanceof LivingEntity a) {
            FLOW.remove(p.getUUID());
            hurtBack(p, a, amount * 1.5f);
            a.knockback(1.2, p.getX() - a.getX(), p.getZ() - a.getZ());
            level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.6, 0.6, 0.2);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1f, 1.4f);
            return false;
        }
        // Contraataque de la Espada de Luz: 25% de cortar las manos de quien te ataca cuerpo a cuerpo
        if (d.knows(SwordSkill.LIGHT_REVERSAL.id) && direct instanceof LivingEntity a && direct == attacker
                && SwordSkills.isWeapon(p.getMainHandItem(), d) && p.getRandom().nextFloat() < 0.25f) {
            hurtBack(p, a, (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2);
            a.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
            a.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
            SwordSkills.disarm(a);
            level.sendParticles(ParticleTypes.END_ROD, a.getX(), a.getY() + 1, a.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.8f);
            msg(p, "msg.remushoku.light_reversal", ChatFormatting.YELLOW);
            return false;
        }
        // Desviar: cuesta 5 de mana, una vez cada 3 segundos
        if (d.knows(SwordSkill.DEFLECT.id) && d.isOn(SwordSkill.DEFLECT.id) && attacker != null
                && SwordSkills.isWeapon(p.getMainHandItem(), d) && DEFLECT_CD.getOrDefault(p.getUUID(), 0L) <= now) {
            ManaData m = ModAttachments.get(p);
            if (m.mana() >= 5) {
                ModAttachments.set(p, m.withMana(m.mana() - 5));
                DEFLECT_CD.put(p.getUUID(), now + 60);
                if (direct instanceof LivingEntity a) a.knockback(0.8, p.getX() - a.getX(), p.getZ() - a.getZ());
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.3f);
                return false;
            }
        }
        // Bloqueo Magico: la mitad del daño magico
        if (d.knows(SwordSkill.MAGIC_BLOCK.id) && source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            REENTRY.set(true);
            try { p.hurt(source, amount * 0.5f); } finally { REENTRY.set(false); }
            return false;
        }
        return true;
    }

    private static void hurtBack(ServerPlayer p, LivingEntity a, float dmg) {
        REENTRY.set(true);
        try {
            a.invulnerableTime = 0;
            a.hurt(p.damageSources().playerAttack(p), dmg);
        } finally {
            REENTRY.set(false);
        }
    }
}
