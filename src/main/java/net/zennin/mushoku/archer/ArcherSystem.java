package net.zennin.mushoku.archer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ArcherData;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.item.QuiverItem;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.network.ArcherActionPayload;
import net.zennin.mushoku.network.ArcherSyncPayload;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Clase Arquero: escuelas, puntos (1 por monstruo hostil, 5 por bandido), habilidades en Z/X/C y efectos sobre las
 * flechas. Funciona con cualquier arco o ballesta (vanilla o de otros mods).
 */
public final class ArcherSystem {
    private static final ResourceLocation WIND_SPEED = MushokuMod.id("archer_wind_speed");
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    /** Lo que se le suma a una flecha: multiplicador de dano, empuje extra, marca brillante, lentitud. */
    static final class ArrowMod {
        float mul = 1f, knock = 0f;
        boolean glow, slow;

        ArrowMod copy() {
            ArrowMod m = new ArrowMod();
            m.mul = mul; m.knock = knock; m.glow = glow; m.slow = slow;
            return m;
        }
    }

    /** Efectos que esperan al proximo disparo. */
    static final class Pending {
        boolean power, triple, gale;
        long deadeyeUntil;
    }

    private static final Map<AbstractArrow, ArrowMod> MODS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<AbstractArrow> SKILL_ARROWS = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
    static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Map<ArcherSkill, Long>> COOLDOWNS = new HashMap<>();

    private ArcherSystem() {}

    public static boolean isArcher(Player p) { return ModAttachments.get(p).playerClass() == SpellCasting.CLASS_ARCHER; }

    public static boolean isBow(ItemStack s) { return !s.isEmpty() && s.getItem() instanceof ProjectileWeaponItem; }

    public static void sync(ServerPlayer p) { ServerPlayNetworking.send(p, new ArcherSyncPayload(ModAttachments.archer(p))); }

    private static void msg(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    private static void msg(ServerPlayer p, String key, ChatFormatting color) { msg(p, Component.translatable(key).withStyle(color)); }

    // ---------------------------------------------------------------- menu
    public static void chooseSchool(ServerPlayer p, String key) {
        ArcherSchool s = ArcherSchool.byKey(key);
        ArcherData d = ModAttachments.archer(p);
        if (s == null || !isArcher(p) || !d.school().isEmpty()) return;
        ModAttachments.setArcher(p, d.withSchool(s.key).learn(ArcherSkill.styleSkill(s).id));
        p.displayClientMessage(Component.translatable("msg.remushoku.school_chosen",
                Component.translatable("school.remushoku." + s.key)).withStyle(ChatFormatting.GOLD), false);
        refresh(p);
        sync(p);
    }

    public static String cantLearn(ArcherData d, ArcherSkill s) {
        boolean own = s.school.key.equals(d.school());
        if (s.kind != ArcherSkill.Kind.STYLE && !d.knows(ArcherSkill.styleSkill(s.school).id)) return "msg.remushoku.need_school";
        if (!own && s.tier > 3) return "msg.remushoku.not_school";
        if (s.tier > 2) {
            boolean prev = false;
            for (ArcherSkill o : ArcherSkill.of(s.school)) if (o.tier == s.tier - 1 && d.knows(o.id)) prev = true;
            if (!prev) return "msg.remushoku.need_previous";
        }
        if (d.points() < s.cost) return "msg.remushoku.no_points";
        return null;
    }

    public static void learn(ServerPlayer p, String id) {
        ArcherSkill s = ArcherSkill.byId(id);
        ArcherData d = ModAttachments.archer(p);
        if (s == null || !isArcher(p) || d.school().isEmpty() || d.knows(id)) return;
        String why = cantLearn(d, s);
        if (why != null) { msg(p, why, ChatFormatting.RED); return; }
        ModAttachments.setArcher(p, d.withPoints(d.points() - s.cost).learn(id));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        msg(p, Component.translatable("msg.remushoku.skill_learned", Component.translatable(s.translationKey())).withStyle(ChatFormatting.GOLD));
        refresh(p);
        sync(p);
    }

    public static void bind(ServerPlayer p, int slot, String id) {
        ArcherSkill s = ArcherSkill.byId(id);
        ArcherData d = ModAttachments.archer(p);
        if (s == null || slot < 0 || slot > 2 || s.kind != ArcherSkill.Kind.ACTIVE || !d.knows(id)) return;
        ModAttachments.setArcher(p, d.bind(slot, id));
        sync(p);
    }

    /** Orbe de cambio de clase: se devuelven los puntos gastados. */
    public static void reset(ServerPlayer p) {
        ArcherData d = ModAttachments.archer(p);
        int refund = d.points();
        ArcherSchool own = ArcherSchool.byKey(d.school());
        for (String id : d.learned()) {
            ArcherSkill s = ArcherSkill.byId(id);
            if (s != null && !(own != null && s == ArcherSkill.styleSkill(own))) refund += s.cost;
        }
        ModAttachments.setArcher(p, ArcherData.DEFAULT.withPoints(refund));
        refresh(p);
        sync(p);
    }

    public static void refresh(ServerPlayer p) {
        ArcherData d = ModAttachments.archer(p);
        boolean bow = isArcher(p) && (isBow(p.getMainHandItem()) || isBow(p.getOffhandItem()));
        set(p, Attributes.MOVEMENT_SPEED, WIND_SPEED, bow && d.knows(ArcherSkill.WIND_STYLE.id) ? 0.12 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) { if (cur != null) inst.removeModifier(id); return; }
        if (cur != null && cur.amount() == amount) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    // ---------------------------------------------------------------- eventos
    public static void init() {
        PayloadTypeRegistry.playS2C().register(ArcherSyncPayload.TYPE, ArcherSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ArcherActionPayload.TYPE, ArcherActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ArcherActionPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            p.server.execute(() -> {
                switch (payload.action()) {
                    case ArcherActionPayload.SCHOOL -> chooseSchool(p, payload.arg());
                    case ArcherActionPayload.LEARN -> learn(p, payload.arg());
                    case ArcherActionPayload.BIND -> bind(p, payload.slot(), payload.arg());
                    case ArcherActionPayload.USE_SLOT -> useSlot(p, payload.slot());
                    default -> {}
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> { refresh(h.getPlayer()); sync(h.getPlayer()); });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 5 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (server.getTickCount() % 20 == 0) refresh(p);
                if (isBow(p.getMainHandItem()) || isBow(p.getOffhandItem())) QuiverItem.feed(p);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer p) || !isArcher(p) || entity instanceof Player) return;
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            int gain = type.contains("bandit") ? 5 : entity instanceof Enemy ? 1 : 0;
            if (gain == 0) return;
            ArcherData d = ModAttachments.archer(p);
            ModAttachments.setArcher(p, d.withPoints(d.points() + gain));
            sync(p);
        });

        // Cada flecha que dispara un arquero recibe sus mejoras
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AbstractArrow arrow && arrow.tickCount == 0 && !SKILL_ARROWS.contains(arrow)
                    && arrow.getOwner() instanceof ServerPlayer p && isArcher(p)) onShot(p, arrow);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ArcherSystem::allowDamage);
    }

    private static void onShot(ServerPlayer p, AbstractArrow arrow) {
        ArcherData d = ModAttachments.archer(p);
        ArrowMod m = new ArrowMod();
        Pending pend = PENDING.computeIfAbsent(p.getUUID(), k -> new Pending());
        long now = p.server.getTickCount();
        ServerLevel level = p.serverLevel();
        if (d.knows(ArcherSkill.HAWK_STYLE.id)) m.mul *= 1.2f;
        if (d.knows(ArcherSkill.MARKED_PREY.id)) m.glow = true;
        if (d.knows(ArcherSkill.QUICK_DRAW.id)) arrow.setCritArrow(true);
        // Lluvia de Acero: 1 de cada 4 flechas no se gasta
        if (d.knows(ArcherSkill.VOLLEY_STYLE.id) && !p.getAbilities().instabuild && arrow.pickup == AbstractArrow.Pickup.ALLOWED
                && p.getRandom().nextFloat() < 0.25f) {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            ItemStack back = arrow.getPickupItemStackOrigin().copyWithCount(1);
            if (!p.getInventory().add(back)) p.drop(back, false);
        }
        if (pend.deadeyeUntil > now) { m.mul *= 1.6f; arrow.setCritArrow(true); }
        if (pend.power) {
            pend.power = false;
            m.mul *= 2.2f;
            m.knock += 1.5f;
            arrow.setCritArrow(true);
            level.sendParticles(ParticleTypes.CRIT, arrow.getX(), arrow.getY(), arrow.getZ(), 20, 0.2, 0.2, 0.2, 0.3);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 0.6f);
        }
        if (pend.gale) {
            pend.gale = false;
            m.knock += 2.5f;
            level.sendParticles(ParticleTypes.CLOUD, arrow.getX(), arrow.getY(), arrow.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        }
        MODS.put(arrow, m);
        if (pend.triple) {
            pend.triple = false;
            Vec3 v = arrow.getDeltaMovement();
            for (int side = -1; side <= 1; side += 2) {
                Arrow extra = new Arrow(level, p, new ItemStack(Items.ARROW), p.getMainHandItem());
                extra.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                extra.setDeltaMovement(v.yRot((float) Math.toRadians(8 * side)));
                extra.setCritArrow(arrow.isCritArrow());
                extra.setBaseDamage(arrow.getBaseDamage());
                extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                SKILL_ARROWS.add(extra);
                MODS.put(extra, m.copy());
                level.addFreshEntity(extra);
            }
        }
    }

    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (REENTRY.get()) return true;
        // Paso Ligero: sin dano de caida con el arco en la mano
        if (victim instanceof ServerPlayer p && source.is(DamageTypeTags.IS_FALL) && isArcher(p)
                && ModAttachments.archer(p).knows(ArcherSkill.LIGHT_STEP.id) && (isBow(p.getMainHandItem()) || isBow(p.getOffhandItem())))
            return false;
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return true;
        ArrowMod m = MODS.get(arrow);
        if (m == null) return true;
        if (m.glow) victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
        if (m.slow) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        if (m.knock > 0) {
            Vec3 v = arrow.getDeltaMovement().normalize();
            victim.knockback(m.knock, -v.x, -v.z);
            victim.hurtMarked = true;
        }
        if (m.mul == 1f) return true;
        REENTRY.set(true);
        try {
            victim.hurt(source, amount * m.mul);
        } finally {
            REENTRY.set(false);
        }
        return false;
    }

    // ---------------------------------------------------------------- habilidades (Z / X / C)
    public static void useSlot(ServerPlayer p, int slot) {
        if (!isArcher(p)) return;
        ArcherData d = ModAttachments.archer(p);
        ArcherSkill s = ArcherSkill.byId(d.slot(slot));
        if (s == null || !d.knows(s.id)) {
            msg(p, Component.translatable("msg.remushoku.slot_empty", slot + 1).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!isBow(p.getMainHandItem()) && !isBow(p.getOffhandItem())) {
            msg(p, "msg.remushoku.need_bow", ChatFormatting.RED);
            return;
        }
        long now = p.server.getTickCount();
        Map<ArcherSkill, Long> cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        long ready = cds.getOrDefault(s, 0L);
        if (ready > now) {
            msg(p, Component.translatable("msg.remushoku.cooldown", Component.translatable(s.translationKey()), (ready - now + 19) / 20)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        ManaData m = ModAttachments.get(p);
        if (m.mana() < s.mana && !p.getAbilities().instabuild) {
            msg(p, "msg.remushoku.no_mana", ChatFormatting.RED);
            return;
        }
        if (!run(p, s)) return;
        if (!p.getAbilities().instabuild) ModAttachments.set(p, m.withMana(m.mana() - s.mana));
        cds.put(s, now + s.cooldown);
    }

    private static void nextArrow(ServerPlayer p, ArcherSkill s) {
        msg(p, Component.translatable("msg.remushoku.next_arrow", Component.translatable(s.translationKey())).withStyle(ChatFormatting.AQUA));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1f, 1.3f);
    }

    private static boolean run(ServerPlayer p, ArcherSkill s) {
        ServerLevel level = p.serverLevel();
        Pending pend = PENDING.computeIfAbsent(p.getUUID(), k -> new Pending());
        long now = p.server.getTickCount();
        switch (s) {
            case POWER_SHOT -> { pend.power = true; nextArrow(p, s); }
            case TRIPLE_SHOT -> { pend.triple = true; nextArrow(p, s); }
            case GALE_ARROW -> { pend.gale = true; nextArrow(p, s); }
            case DEADEYE -> {
                pend.deadeyeUntil = now + 160;
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 180, 0, false, false));
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getEyeY(), p.getZ(), 30, 0.4, 0.3, 0.4, 0.2);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, 0.7f);
                msg(p, Component.translatable("msg.remushoku.deadeye").withStyle(ChatFormatting.GOLD));
            }
            case PIERCING_RAY -> piercingRay(p);
            case PHANTOM_ARROW -> phantomArrow(p);
            case ARROW_RAIN -> rain(p, 4, 2, 40, 1);
            case STEEL_STORM -> rain(p, 8, 2, 80, 3);
            case BARRAGE -> Tasks.repeat(0, 2, 20, t -> {
                if (!p.isAlive()) return false;
                Arrow a = skillArrow(p, 2.0);
                Vec3 look = p.getLookAngle();
                a.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                a.shoot(look.x, look.y, look.z, 3.0f, 4f);
                level.addFreshEntity(a);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.7f, 1.4f);
                return true;
            });
            case DISENGAGE -> {
                Vec3 look = p.getLookAngle();
                Vec3 back = new Vec3(-look.x, 0, -look.z).normalize().scale(1.5);
                p.setDeltaMovement(back.x, 0.65, back.z);
                p.hurtMarked = true;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, false, false));
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 16, 0.4, 0.1, 0.4, 0.05);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1f, 1.1f);
                pend.gale = true; // la proxima flecha sale con viento
            }
            case TEMPEST_ARROW -> {
                MagicProjectile proj = new MagicProjectile(level, p, MagicProjectile.WIND, 12f).size(1.4f).knockback(1f).lift();
                proj.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                proj.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.2f, 0f);
                level.addFreshEntity(proj);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1f, 0.8f);
            }
            case GALE_DANCE -> {
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(look.x * 1.6, 0.35, look.z * 1.6);
                p.hurtMarked = true;
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
                Tasks.later(6, () -> {
                    List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16),
                            e -> e != p && e.isAlive() && e instanceof Enemy);
                    near.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
                    for (int i = 0; i < Math.min(4, near.size()); i++) {
                        LivingEntity t = near.get(i);
                        Arrow a = skillArrow(p, 3.0);
                        a.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                        Vec3 dir = t.getEyePosition().subtract(a.position()).normalize();
                        a.shoot(dir.x, dir.y + 0.05, dir.z, 3.0f, 0f);
                        level.addFreshEntity(a);
                    }
                    level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, 1.6f);
                });
            }
            default -> { return false; }
        }
        return true;
    }

    /** Flecha de habilidad: no se puede juntar y no recibe dos veces las mejoras del disparo normal. */
    private static Arrow skillArrow(ServerPlayer p, double damage) {
        Arrow a = new Arrow(p.serverLevel(), p, new ItemStack(Items.ARROW), p.getMainHandItem());
        a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        a.setBaseDamage(damage);
        SKILL_ARROWS.add(a);
        ArrowMod m = new ArrowMod();
        if (ModAttachments.archer(p).knows(ArcherSkill.HAWK_STYLE.id)) m.mul = 1.2f;
        if (ModAttachments.archer(p).knows(ArcherSkill.MARKED_PREY.id)) m.glow = true;
        MODS.put(a, m);
        return a;
    }

    private static Vec3 aimPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Lluvia de flechas sobre donde miras. */
    private static void rain(ServerPlayer p, double radius, int every, int ticks, int perWave) {
        ServerLevel level = p.serverLevel();
        Vec3 c = aimPoint(p, 40);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
        Tasks.repeat(0, every, ticks, t -> {
            for (int i = 0; i < perWave; i++) {
                double a = p.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(p.getRandom().nextDouble()) * radius;
                Arrow arrow = skillArrow(p, 3.0);
                arrow.setPos(c.x + Math.cos(a) * r, c.y + 14, c.z + Math.sin(a) * r);
                arrow.setDeltaMovement((p.getRandom().nextDouble() - 0.5) * 0.1, -2.6, (p.getRandom().nextDouble() - 0.5) * 0.1);
                level.addFreshEntity(arrow);
            }
            return true;
        });
    }

    /** Rayo perforante: atraviesa a todos los que esten en linea (48 bloques). */
    private static void piercingRay(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        Vec3 end = aimPoint(p, 48);
        double len = end.distanceTo(eye);
        java.util.Set<Entity> hit = new java.util.HashSet<>();
        for (double t = 0; t < len; t += 0.5) {
            Vec3 at = eye.add(dir.scale(t));
            if (((int) (t * 2)) % 2 == 0) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.6), e -> e != p && e.isAlive())) {
                if (hit.add(e)) {
                    e.invulnerableTime = 0;
                    e.hurt(p.damageSources().arrow(skillArrow(p, 0), p), 12f);
                }
            }
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1f, 1.6f);
    }

    /** Flecha fantasma: busca sola al enemigo mas cercano. */
    private static void phantomArrow(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Arrow a = skillArrow(p, 8.0);
        a.setNoGravity(true);
        a.setGlowingTag(true);
        a.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
        Vec3 look = p.getLookAngle();
        a.shoot(look.x, look.y, look.z, 2.0f, 0f);
        level.addFreshEntity(a);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1.4f);
        Tasks.repeat(2, 1, 80, t -> {
            if (!a.isAlive() || a.onGround() || a.isInWater()) return false;
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(24),
                    e -> e != p && e.isAlive() && e instanceof Enemy);
            near.sort((x, y) -> Double.compare(x.distanceToSqr(a), y.distanceToSqr(a)));
            if (!near.isEmpty()) {
                Vec3 want = near.get(0).getBoundingBox().getCenter().subtract(a.position()).normalize().scale(2.0);
                Vec3 v = a.getDeltaMovement().scale(0.6).add(want.scale(0.4));
                a.setDeltaMovement(v);
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.getX(), a.getY(), a.getZ(), 1, 0, 0, 0, 0);
            return true;
        });
    }

    /** Uso en el dano de flechas de la mochila: cuantas flechas hay en un carcaj. */
    public static int arrowsIn(ItemStack quiver) {
        BundleContents c = quiver.get(DataComponents.BUNDLE_CONTENTS);
        if (c == null) return 0;
        int n = 0;
        for (ItemStack s : c.items()) if (s.is(ItemTags.ARROWS)) n += s.getCount();
        return n;
    }
}
